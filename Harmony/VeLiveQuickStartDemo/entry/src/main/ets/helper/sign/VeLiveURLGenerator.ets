import { rcp } from '@kit.RemoteCommunicationKit'
import URLSignTool from './VeLiveURLSignTool'
import { HashMap } from '@kit.ArkTS'
import { VeLivePullURLListModel, VeLivePullURLModel, VeLiveURLRootModel } from './VeLiveURLModel'

export type URLGenerateResult = VeLiveURLRootModel<VeLivePullURLModel> | VeLivePullURLListModel | undefined
export default class VeLiveURLGenerator {
  static shared: VeLiveURLGenerator = new VeLiveURLGenerator()
  private accessKey: string = ''
  private secretKey: string = ''
  private vHost: string = ''
  private appName: string = ''
  private pushDomain: string = ''
  private pullDomain: string = ''
  private signTool: URLSignTool = new URLSignTool(this.accessKey, this.secretKey)
  private urlSession?: rcp.Session

  static Setup(accessKey: string, secretKey: string, vHost?: string, appName?: string, pushDomain?: string, pullDomain?: string) {
    VeLiveURLGenerator.shared.setup(accessKey, secretKey, vHost, appName, pushDomain, pullDomain)
  }

  setup(accessKey: string, secretKey: string, vHost?: string, appName?: string, pushDomain?: string, pullDomain?: string) {
    this.signTool.reset(accessKey, secretKey)
    this.accessKey = accessKey
    this.secretKey = secretKey

    if (vHost !== undefined) {
      this.vHost = vHost
    }
    if (appName !== undefined) {
      this.appName = appName
    }
    if (pushDomain !== undefined) {
      this.pushDomain = pushDomain
    }
    if (pullDomain !== undefined) {
      this.pullDomain = pullDomain
    }
  }

  static async GenPullUrl(streamName: string = '',
    protocol?: string, app: string = 'live'): Promise<URLGenerateResult> {
    return VeLiveURLGenerator.shared.genPullUrl(streamName, protocol, app)
  }

  async genPullUrl(streamName: string = '', protocol?: string,
    app: string = 'live'): Promise<URLGenerateResult> {
    return new Promise<URLGenerateResult>((resolve, reject) => {
      try {
        if (this.urlSession == undefined) {
          this.urlSession = rcp.createSession()
        }
        const body = new HashMap<string, string>()
        body.set('Vhost', this.vHost)
        body.set('Domain', this.pullDomain)
        body.set('App', app === undefined ? this.appName : app)
        body.set('Stream', streamName === undefined ? '' : streamName)
        body.set('Type', 'fcdn')
        const request = this.signTool.sign({ action: 'GeneratePlayURL', body: body })
        if (request == undefined) {
          reject("can not sign request")
          return
        }
        this.urlSession?.fetch(request).then((value) => {
          const model = new VeLiveURLRootModel<VeLivePullURLModel>(value.body)
          if (protocol !== undefined) {
            if (protocol.toLowerCase() == 'rtm') {
              protocol = 'udp';
            }
            resolve(model.Result?.getUrlListModel(protocol));
          } else {
            resolve(model)
          }
        }).catch((err: object) => {
          reject(err)
        })
      } catch (error) {
        reject(error)
      }
    })
  }
}