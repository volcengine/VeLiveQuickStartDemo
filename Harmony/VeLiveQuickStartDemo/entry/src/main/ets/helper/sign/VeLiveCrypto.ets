import { cryptoFramework } from '@kit.CryptoArchitectureKit';
import { buffer, util } from '@kit.ArkTS'
import hilog from '@ohos.hilog';

const DOMAIN = 0x0004;
const LOG_TAG = 'Crypto'

export type CryptoDataType = Uint8Array | string | cryptoFramework.DataBlob

export function HmacSha256DataWithKey(data: CryptoDataType, key: CryptoDataType): Nullable<cryptoFramework.DataBlob> {
  try {
    const hmac = cryptoFramework.createMac("SHA256");
    const symKey = cryptoFramework.createSymKeyGenerator('HMAC').convertKeySync(GetDataBlob(key));
    hmac.initSync(symKey)
    hmac.updateSync(GetDataBlob(data))
    return hmac.doFinalSync()
  } catch (error) {
    hilog.error(DOMAIN, LOG_TAG, '%{public}s', error)
  }
  return undefined
}

export function HmacSha256HashStringWithKey(data: CryptoDataType, key: CryptoDataType): string {
  let blobData = HmacSha256DataWithKey(data, key)
  if (blobData != undefined) {
    return Uint8ArrayToHexString(blobData.data)
  }
  return ''
}

export function Sha256Data(str: CryptoDataType): Nullable<cryptoFramework.DataBlob> {
  try {
    const md = cryptoFramework.createMd('SHA256')
    md.updateSync(GetDataBlob(str))
    return md.digestSync()
  } catch (error) {
    hilog.error(DOMAIN, LOG_TAG, '%{public}s', error)
  }
  return undefined
}

export function Sha256HashString(str: CryptoDataType): string {
  let blobData = Sha256Data(str)
  if (blobData != undefined) {
    return Uint8ArrayToHexString(blobData.data)
  }
  return ''
}

export function Base64Decode(str: string): string {
  const base64 = new util.Base64Helper()
  return uint8ArrayToString(base64.decodeSync(str))
}

export function StringToNumberStr(str: string, length: number): string {
  let blobData = Sha256Data(str)
  if (blobData != undefined) {
    const numberStr = Uint8ArrayToDecimalString(blobData.data)
    if (length >= numberStr.length) {
      return numberStr
    }
    return numberStr.substring(0, length)
  }
  return ''
}

function uint8ArrayToString(u8a: Uint8Array, encoding: string = 'utf-8'): string {
  const decoder = util.TextDecoder.create(encoding)
  return decoder.decodeToString(u8a);
}

function Uint8ArrayToHexString(u8a: Uint8Array): string {
  let hexString = '';
  for (let i = 0; i < u8a.length; i++) {
    const hex = u8a[i].toString(16).padStart(2, '0');
    hexString += hex;
  }
  return hexString;
}

function Uint8ArrayToDecimalString(u8a: Uint8Array): string {
  let hexString = '';
  for (let i = 0; i < u8a.length; i++) {
    const hex = u8a[i].toString(10)
    hexString += hex;
  }
  return hexString;
}

function GetDataBlob(cryptoData: CryptoDataType): cryptoFramework.DataBlob {
  if (cryptoData instanceof Uint8Array) {
    return {
      data: cryptoData
    }
  } else if (typeof cryptoData == 'string') {
    return {
      data: new Uint8Array(buffer.from(cryptoData, 'utf-8').buffer)
    }
  } else {
    return cryptoData as cryptoFramework.DataBlob
  }
}