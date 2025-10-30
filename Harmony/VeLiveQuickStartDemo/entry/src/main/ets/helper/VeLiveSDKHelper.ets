import { window } from '@kit.ArkUI';
import {
  VeLivePlayerEnv,
  VeLivePlayerEnvConfig,
  VeLivePlayerEnvConstant,
  VeLivePlayerStatistics
} from '@livecore/liveplayer';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@ohos.base';
import hilog from '@ohos.hilog';
import VeLiveURLGenerator from './sign/VeLiveURLGenerator';
import inputMethod from '@ohos.inputMethod';
import { resourceManager } from '@kit.LocalizationKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import BuildProfile from 'BuildProfile';

const DOMAIN = 0x0001;
const LOG_TAG = 'VeLiveSDKHelper'

export class VeLiveSDKHelper {
  // AppID
  public static TTSDK_APP_ID = "";
  /*
   License 名称，当前 Demo文件存放在与本文件同级目录下，如果做SDK快速验证，可直接替换 ttsdk.lic 文件内容
   */
  public static TTSDK_LICENSE_NAME = "ttsdk.lic";
  /**
  不要在生产环境使用，生产环境的拉流地址请在服务端生成
   */
  /*
   *  API访问密钥 https://console.volcengine.com/iam/keymanage/
   */
  public static ACCESS_KEY_ID = "";
  public static SECRET_ACCESS_KEY = "";
  /*
   * 直播 VHOST
   * https://console.volcengine.com/iam/resourcemanage/project/default/
   */
  public static LIVE_VHOST = "";
  /*
   * 生成直播拉流地址时使用
   * 举例: https://pull.example.com/live/abc.flv
   */
  public static LIVE_APP_NAME = "live";
  /*
   * 直播拉流域名 https://console.volcengine.com/live/main/domain/list
   */
  public static LIVE_PULL_DOMAIN = "";
  private static sAppContext: common.UIAbilityContext;

  public static appContext(): common.UIAbilityContext {
    return VeLiveSDKHelper.sAppContext
  }

  public static initTTSDK(context: common.UIAbilityContext, startAppLogImmediately: boolean = true) {
    VeLiveSDKHelper.sAppContext = context;
    VeLivePlayerEnv.init(new VeLivePlayerEnvConfig(context)
      .setLicenseUri($r('app.media.ttsdk'))
      .setAppID(VeLiveSDKHelper.TTSDK_APP_ID)
      .setAppName("VeLiveQuickStartDemo")
      .setAppVersion("1.0.0")
      .setAppChannel("Test")
      .setAppRegion(VeLivePlayerEnvConstant.AppRegion.CHINA)
      .setLicenseLog(true)
      .setLicenseCallback((err: Nullable<BusinessError>, ret: boolean) => {
        if (ret == false || err != undefined) {
          hilog.error(DOMAIN, LOG_TAG, '%{public}s', "license check failed " + (err?.message || '') + (err?.code || 0));
        } else {
          hilog.info(DOMAIN, LOG_TAG, '%{public}s', "license check success");
        }
      })
      .setInitAppLog(true)
      .setAutoStartAppLog(false)
      .setAppLogReadyCallback((err: Nullable<BusinessError>, ret: Record<string, Object>) => {
        if (err === undefined) {
          hilog.info(DOMAIN, LOG_TAG, '%{public}s', "applog init success" + ret);
        } else {
          hilog.error(DOMAIN, LOG_TAG, '%{public}s', "applog init failed" + err.message);
        }
      })
    )
    if (startAppLogImmediately) {
      VeLivePlayerEnv.startAppLog()
    }
  }

  public static getPlaybackInfoString(statistics: VeLivePlayerStatistics): string {
    let infoStr = "";
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_url'))} ${statistics.url}\n`
    let videoSize = "width:" + statistics.width + " height:" + statistics.height;
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_video_size'))} ${videoSize}\n`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_video_fps'))} ${statistics.fps?.toFixed(0)} `
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_video_bitrate'))} ${statistics.bitrate?.toFixed(0)} kbps\n`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_video_buffer_time'))} ${statistics.videoBufferMs} ms`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_audio_buffer_time'))} ${statistics.audioBufferMs} ms\n`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_stream_format'))} ${statistics.format} `
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_stream_protocol'))} ${statistics.protocol}`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_video_codec'))} ${statistics.videoCodec} \n`

    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_delay_time'))} ${statistics.delayMs} ms `
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_stall_time'))} ${statistics.stallTimeMs} ms \n`
    infoStr += `${VeLiveSDKHelper.getResourceString($r('app.string.pull_info_is_hardware'))} ${statistics.isHardwareDecode}`
    hilog.info(DOMAIN, LOG_TAG, '%{public}s', infoStr);
    return infoStr;
  }

  private static getResourceString(resource: Resource): string {
    try {
      const resManager = VeLiveSDKHelper.sAppContext.resourceManager
      return resManager.getStringSync(resource.id)
    } catch (error) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', `${error}`);
    }
    return ""
  }

  static getWindowStage(): Nullable<window.WindowStage> {
    return VeLiveSDKHelper.appContext()?.windowStage;
  }

  static getMainWindow(): Nullable<window.Window> {
    try {
      return VeLiveSDKHelper.appContext()?.windowStage.getMainWindowSync();
    } catch (error) {
      return undefined
    }
  }

  static getUIContext(): Nullable<UIContext> {
    try {
      return VeLiveSDKHelper.getMainWindow()?.getUIContext();
    } catch (error) {
      return undefined
    }
  }

  static getStatusBarHeight(): number {
    try {
      const windowClass = VeLiveSDKHelper.getMainWindow();
      const avoidArea = windowClass?.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM);
      return avoidArea ? avoidArea.topRect.height : 0;
    } catch (err) {
      hilog.error(DOMAIN, LOG_TAG, 'AppUtil-getStatusBarHeight error: %{public}s', err);
      return 0;
    }
  }

  static hideGlobalKeyboard() {
    try {
      inputMethod.getController().hideTextInput().catch((_: BusinessError<void>) => {
      })
    } catch (error) {
    }
  }

}
