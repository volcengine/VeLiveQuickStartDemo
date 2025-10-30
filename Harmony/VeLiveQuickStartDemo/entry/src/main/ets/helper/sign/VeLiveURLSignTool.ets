import { rcp } from '@kit.RemoteCommunicationKit'
import { ArrayList, buffer, HashMap, JSON, uri } from '@kit.ArkTS'
import * as Crypto from './VeLiveCrypto';
import { intl } from '@kit.LocalizationKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { Base64Decode } from './VeLiveCrypto';

const DOMAIN = 0x0003;
const LOG_TAG = 'URLSignTool'

type URLSignValueType = object | string | number | URLSignMap | Array<URLSignValueType> | ArrayList<URLSignValueType>
type URLSignMap = HashMap<string, URLSignValueType>
const URLSignVersionKey = 'Version'
const URLSignActionKey = 'Action'

type SignAction = 'GeneratePushURL' | 'GeneratePlayURL'

export interface SignParam {
  action: SignAction
  method?: string
  query?: URLSignMap
  body?: URLSignMap
}

export default class URLSignTool {
  private baseUrl: string = 'https://live.volcengineapi.com'

  private get host(): string {
    return new uri.URI(this.baseUrl).host
  }

  private relativePath: string = '/'
  private region: string = 'cn-north-1'
  private version: string = '2023-01-01'
  private service: string = 'live'
  private method: string = 'POST'
  private contentType: string = 'application/x-www-form-urlencoded; charset=utf-8'
  private signHeader: string = 'content-type;host;x-content-sha256;x-date'
  private queryParams: URLSignMap = new HashMap()
  private body: URLSignMap = new HashMap()
  private action: string = ''
  private accessKey: string
  private secretKey: string

  constructor(accessKey: string, secretKey: string) {
    try {
      this.accessKey = Base64Decode(accessKey)
      this.secretKey = Base64Decode(secretKey)
    } catch (e) {
      this.accessKey = accessKey
      this.secretKey = secretKey
    }
  }

  public reset(accessKey: string, secretKey: string) {
    try {
      this.accessKey = Base64Decode(accessKey)
      this.secretKey = Base64Decode(secretKey)
    } catch (e) {
      this.accessKey = accessKey
      this.secretKey = secretKey
    }
  }

  public sign(params: SignParam): Nullable<rcp.Request> {
    this.fillParams(params)

    let queryStr = this.urlEncodeParams(this.queryParams)
    let contentSha256: string = ''
    let bodyData: Nullable<Uint8Array> = undefined
    if (this.body !== undefined && this.body.length > 0) {
      const bodyJson = JSON.stringify(this.signMapToRecord(this.body))
      bodyData = new Uint8Array(buffer.from(bodyJson, 'utf-8').buffer)
      contentSha256 = Crypto.Sha256HashString(bodyData)
    } else {
      contentSha256 = Crypto.Sha256HashString('')
    }
    let dateStr = this.dateFormatString()
    let shortDateStr = dateStr.substring(0, 8)
    let canonicalString = this.getCanonicalString(queryStr, contentSha256, dateStr)
    let hashCanonicalStr = Crypto.Sha256HashString(canonicalString)
    let credentialScope = `${shortDateStr}/${this.region}/${this.service}/request`
    let signString = `HMAC-SHA256\n${dateStr}\n${credentialScope}\n${hashCanonicalStr}`
    let signKeyData = this.signKeyWithDate(shortDateStr)
    if (signKeyData == undefined) {
      return undefined;
    }
    let signature = Crypto.HmacSha256HashStringWithKey(signString, signKeyData)
    let authorization =
      `HMAC-SHA256 Credential=${this.accessKey}/${credentialScope}, SignedHeaders=${this.signHeader}, Signature=${signature}`
    let urlStr = this.baseUrl + this.relativePath + '?' + queryStr
    let request = new rcp.Request(urlStr)
    request.method = this.method
    request.headers = {
      'Host': this.host,
      'X-Date': dateStr,
      'X-Content-Sha256': contentSha256,
      'Content-Type': this.contentType,
      'Authorization': authorization
    }
    if (this.method.toLowerCase() !== 'GET' && bodyData !== undefined) {
      request.content = bodyData.buffer
    }
    return request
  }

  private fillParams(param: SignParam) {
    if (param.action !== undefined) {
      this.action = param.action
    }
    if (param.method !== undefined) {
      this.method = param.method
    }
    try {
      if (param.query !== undefined) {
        this.queryParams.setAll(param.query)
      }
      if (param.body !== undefined) {
        this.body.setAll(param.body)
      }
    } catch (error) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', error)
    }
    this.checkActionAndVersion()
  }

  private checkActionAndVersion() {
    try {
      if (!this.queryParams.hasKey(URLSignVersionKey)) {
        this.queryParams.set(URLSignVersionKey, this.version)
      }
      if (!this.queryParams.hasKey(URLSignActionKey)) {
        this.queryParams.set(URLSignActionKey, this.action)
      }
      if (this.body.hasKey(URLSignVersionKey)) {
        this.queryParams.set(URLSignVersionKey, this.body.get(URLSignVersionKey))
        this.body.remove(URLSignVersionKey)
      }
    } catch (error) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', error)
    }
  }

  private urlEncodeParams(params: URLSignMap, sortKeys: boolean = true, encode: boolean = true): string {
    if (params == undefined || params.length <= 0) {
      return ''
    }
    try {
      let values: string[] = []
      let allKeys = Array.from(params.keys())
      if (sortKeys) {
        allKeys = allKeys.sort((a, b) => a.localeCompare(b))
      }
      allKeys.forEach((key, _index) => {
        const value = params.get(key)
        if (value === undefined || value === null) {
          values.push(this.encodeValue(key, encode))
        } else {
          values.push(`${this.encodeValue(key, encode)}=${this.encodeValue(value, encode)}`)
        }
      })
      return values.join('&')
    } catch (e) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', e)
      return ''
    }
  }

  private encodeValue(value: URLSignValueType, encode: boolean = true): string {
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return encode ? encodeURIComponent(value) : value.toString()
    } else {
      return encode ? encodeURIComponent(JSON.stringify(value)) : JSON.stringify(value)
    }
  }

  private getCanonicalString(queryStr: string, contentSha256: string, dateStr: string) {
    return this.method + "\n"
      + this.relativePath + "\n"
      + queryStr + "\n"
      + `content-type:${this.contentType}` + "\n"
      + `host:${this.host}` + "\n"
      + `x-content-sha256:${contentSha256}` + "\n"
      + `x-date:${dateStr}` + "\n"
      + "\n"
      + this.signHeader + "\n"
      + contentSha256
  }

  private signKeyWithDate(date: string): Nullable<Uint8Array> {
    let kData = Crypto.HmacSha256DataWithKey(date, this.secretKey)
    if (kData == undefined) {
      return undefined
    }
    let kRegion = Crypto.HmacSha256DataWithKey(this.region, kData)
    if (kRegion == undefined) {
      return undefined
    }
    let kService = Crypto.HmacSha256DataWithKey(this.service, kRegion)
    if (kService == undefined) {
      return undefined
    }
    let kSignKey = Crypto.HmacSha256DataWithKey("request", kService)
    if (kSignKey == undefined) {
      return undefined
    }
    return kSignKey.data
  }

  private dateFormatString(): string {
    if (deviceInfo.sdkApiVersion > 20) {
      const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
        timeZone: 'GMT',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
      return dateFormatter.format(new Date()).replaceAll('/', '').replaceAll(':', '').replaceAll(' ', 'T') + 'Z'
    } else {
      const dateFormatter = new intl.DateTimeFormat('zh-CN', {
        timeZone: 'GMT',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
      return dateFormatter.format(new Date()).replaceAll('/', '').replaceAll(':', '').replaceAll(' ', 'T') + 'Z'
    }
  }

  private signMapToRecord(signMap: URLSignMap): Record<string, object> {
    const record: Record<string, Object> = {}
    try {
      signMap.forEach((v, k) => {
        if (v !== undefined && k !== undefined) {
          record[k] = v
        }
      })
    } catch (error) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', error)
    }
    return record
  }
}