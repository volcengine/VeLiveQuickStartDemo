import { Type, plainToInstance } from 'class-transformer'
import { util } from '@kit.ArkTS'

export class VeLiveURLError {
  Code: string = ''
  CodeN: number = -1
  Message: string = ''

  constructor(code: string, codeN: number, message: string) {
    this.Code = code
    this.CodeN = codeN
    this.Message = message
  }
}

export class VeLiveURLResponseMetadata {
  RequestId: string = ''
  Action: string = ''
  Version: string = ''
  Service: string = ''
  Region: string = ''
  @Type(() => VeLiveURLError)
  Error?: VeLiveURLError
}

export type VeLiveURLModelResultType = VeLivePullURLModel

export class VeLiveURLRootModel<T extends VeLiveURLModelResultType> {
  ResponseMetadata: VeLiveURLResponseMetadata = new VeLiveURLResponseMetadata()
  Result?: T

  constructor(data: string | ArrayBuffer | Uint8Array | undefined) {
    if (data == undefined) {
      this.ResponseMetadata.Error = new VeLiveURLError('-1', -1, 'data is undefined')
    } else {
      const jsonData = this.getJsonObject(data)
      if (jsonData['ResponseMetadata']) {
        this.ResponseMetadata = plainToInstance(VeLiveURLResponseMetadata, jsonData['ResponseMetadata'])
      }
      const resultJson = jsonData['Result'] as object
      if (resultJson !== undefined) {
        if (resultJson['URLList']) {
          this.Result = plainToInstance(VeLivePullURLModel, resultJson) as T
        }
      }
    }
  }

  private getJsonObject(data: string | ArrayBuffer | Uint8Array | undefined): object {
    let jsonStr = ''
    if (typeof data === 'string') {
      jsonStr = data
    } else if (data instanceof Uint8Array) {
      jsonStr = util.TextDecoder.create('utf-8').decodeToString(data)
    } else if (data instanceof ArrayBuffer) {
      jsonStr = util.TextDecoder.create('utf-8').decodeToString(new Uint8Array(data))
    }
    return JSON.parse(jsonStr) as Map<string, object>
  }
}

export class VeLivePullURLListModel {
  URL: string = ''
  Type: string = ''
  CDN: string = ''
  Protocol: string = ''
}

export class VeLivePullURLModel {
  @Type(() => VeLivePullURLListModel)
  URLList: VeLivePullURLListModel[] = []

  public getUrl(protocol: string): Nullable<string> {
    return this.URLList.find((s) => s.Protocol === protocol)?.URL
  }
  public getUrlListModel(protocol: string): Nullable<VeLivePullURLListModel> {
    return this.URLList.find((s) => s.Protocol === protocol)
  }
}