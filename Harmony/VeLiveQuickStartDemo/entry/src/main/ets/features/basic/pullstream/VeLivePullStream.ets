import {
  VeLivePlayer,
  VeLivePlayerConfiguration,
  VeLivePlayerFillMode,
  VeLivePlayerStatistics,
  VeLivePlayerStatus,
  VeLivePlayerViewV2
} from "@livecore/liveplayer"
import { VeLiveNavigationBar, VeLiveNavigationBarHeight } from "../../../helper/VeLiveNavigationBar"
import { VeLiveSDKHelper } from "../../../helper/VeLiveSDKHelper"
import hilog from "@ohos.hilog"
import { VeLivePlayerObserverImp } from "../../../helper/VeLivePlayerObserverImp";
import { BusinessError, deviceInfo } from "@kit.BasicServicesKit";
import { KeyboardAvoidMode } from '@kit.ArkUI';
import VeLiveURLGenerator from "../../../helper/sign/VeLiveURLGenerator";
import { VeLivePullURLListModel } from "../../../helper/sign/VeLiveURLModel";
import { SelectDialog } from '@kit.ArkUI';

const DOMAIN = 0x0006;
const LOG_TAG = 'VeLivePullStream'

@Entry
@ComponentV2
struct VeLivePullStream {
  private navStack?: NavPathStack
  private statusBarContentColor?: string
  private livePlayer: Nullable<VeLivePlayer> = undefined
  private observer: VeLivePlayerObserverImp = new VeLivePlayerObserverImp()
  private fillModeDialog?: CustomDialogController
  @Local urlStrOrName: string = ''
  @Local infoString: ResourceStr = ''
  @Local playStatus: VeLivePlayerStatus = VeLivePlayerStatus.VeLivePlayerStatusStopped

  aboutToAppear(): void {
    VeLiveSDKHelper.getMainWindow()?.setWindowKeepScreenOn(true, () => {
    })
    this.storeStatusBar()
    this.setupLivePlayer()
    if (deviceInfo.sdkApiVersion >= 14) {
      this.getUIContext().setKeyboardAvoidMode(KeyboardAvoidMode.NONE)
    }
    this.observer.on<VeLivePlayerStatistics>('statistics', (data) => {
      this.infoString = VeLiveSDKHelper.getPlaybackInfoString(data)
    })
    this.observer.on<VeLivePlayerStatus>('playStatus', (status) => {
      this.playStatus = status
    })
  }

  aboutToDisappear(): void {
    VeLiveSDKHelper.getMainWindow()?.setWindowKeepScreenOn(false, () => {
    })
    this.reStoreStatusBar()
    // 销毁播放器
    this.livePlayer?.destroy()
    this.livePlayer = undefined
    this.observer.off('statistics')
  }

  onPageShow(): void {
    this.storeStatusBar()
  }

  onPageHide(): void {
    this.reStoreStatusBar()
  }

  private setupLivePlayer(): void {
    this.livePlayer = new VeLivePlayer(VeLiveSDKHelper.appContext())
    if (this.livePlayer == undefined) {
      hilog.error(DOMAIN, LOG_TAG, '%{public}s', 'LivePlayer Create Failed')
      return
    }
    const config = new VeLivePlayerConfiguration()
    // 打开周期性回调
    config.enableStatisticsCallback = true;
    // 初始化播放器
    this.livePlayer.setConfig(config);
    // 设置观察者
    this.livePlayer.setObserver(this.observer);
  }

  private onStartClick() {
    if (this.playStatus == VeLivePlayerStatus.VeLivePlayerStatusPlaying || this.livePlayer?.isPlaying()) {
      this.livePlayer?.stop();
      return
    }

    if (this.urlStrOrName.length == 0) {
      this.infoString = $r('app.string.input_stream_name_placeholder')
      return
    }

    if (this.urlStrOrName.startsWith("http") || this.urlStrOrName.startsWith("rtmp")) {
      this.startPlay(this.urlStrOrName)
      return
    }

    this.infoString = $r('app.string.generated_pull_url')
    VeLiveURLGenerator.GenPullUrl(this.urlStrOrName, 'flv').then((value) => {
      if (value instanceof VeLivePullURLListModel && value.URL.length > 0) {
        this.startPlay(value.URL)
        this.infoString = value.URL;
      }
    }).catch((e: BusinessError<void>) => {
      this.infoString = e.message;
    })
  }

  private startPlay(url: string) {
    this.livePlayer?.setPlayUrl(url)
    this.livePlayer?.play()
  }

  private onFillModeClick() {
    if (this.fillModeDialog == undefined) {
      this.fillModeDialog = this.makeFillModeDialog((mode) => {
        this.livePlayer?.setRenderFillMode(mode)
      })
    }
    this.fillModeDialog.open()
  }

  private onMuteClick() {
    this.livePlayer?.setMute(!this.isMute);
    this.isMute = !this.isMute;
  }

  build() {
    NavDestination() {
      Stack({ alignContent: Alignment.TopStart }) {
        // 导航栏
        VeLiveNavigationBar({ navStack: this.navStack, tintColor: $r('app.color.white_color') })
        // 播放器视图
        VeLivePlayerViewV2({
          onLoad: (window) => {
            // 设置播放器渲染视图
            this.livePlayer?.setWindow(window);
          }
        })
        // 控制层 UI
        this.buildControls()
      }
      .width('100%')
      .height('100%')
      .expandSafeArea()
      .backgroundColor(Color.Black)
      .onClick(() => VeLiveSDKHelper.hideGlobalKeyboard())
    }.onReady((ctx) => {
      this.navStack = ctx.pathStack
    }).hideTitleBar(true)
  }

  @Local isMute: boolean = false

  @Computed
  get muteTitle(): ResourceStr {
    return this.isMute ? $r('app.string.un_mute') : $r('app.string.mute')
  }

  @Computed
  get playTitle(): ResourceStr {
    return this.playStatus == VeLivePlayerStatus.VeLivePlayerStatusPlaying ? $r('app.string.stop_play') :
      $r('app.string.start_play')
  }

  @Builder
  private buildControls() {
    Column({ space: 8 }) {
      Row() {
        Text($r('app.string.stream_name')).fontColor($r('app.color.white_color'))
        Blank().width(8)
        TextInput({ placeholder: $r('app.string.input_stream_name_placeholder'), text: $$this.urlStrOrName })
          .layoutWeight(1)
          .type(InputType.URL)
          .fontColor($r('app.color.white_color'))
          .placeholderColor($r('app.color.white_color'))
          .backgroundColor($r('app.color.input_background'))
          .borderRadius(8)
      }.margin({ top: 10 })

      Text(this.infoString)
        .fontColor($r('app.color.white_color'))
        .fontSize(14)
        .textAlign(TextAlign.Start)
        .align(Alignment.Start)
        .width('100%')

      Blank().layoutWeight(1)

      Row({ space: 8 }) {
        Button() {
          Text(this.playTitle).fontColor($r('app.color.white_color'))
        }.height(30).layoutWeight(1).onClick((_) => this.onStartClick())
      }.alignItems(VerticalAlign.Center).justifyContent(FlexAlign.Center)

      Row({ space: 8 }) {
        Button() {
          Text($r('app.string.play_fill_mode')).fontColor($r('app.color.white_color'))
        }.height(30).layoutWeight(1).onClick((_) => this.onFillModeClick())

        Button() {
          Text(this.muteTitle).fontColor($r('app.color.white_color'))
        }.height(30).layoutWeight(1).onClick((_) => this.onMuteClick())
      }.alignItems(VerticalAlign.Center).justifyContent(FlexAlign.Center).margin({ bottom: 10 })
    }
    .margin({ left: 30, top: VeLiveNavigationBarHeight, right: 30 })
  }

  private storeStatusBar() {
    try {
      if (this.statusBarContentColor == undefined) {
        this.statusBarContentColor =
          VeLiveSDKHelper.getMainWindow()?.getWindowSystemBarProperties().statusBarContentColor
      }
      VeLiveSDKHelper.getMainWindow()?.setWindowSystemBarProperties({
        statusBarContentColor: "#FFFFFF"
      }).catch((_: BusinessError<void>) => {
      })
    } catch (error) {
    }
  }

  private reStoreStatusBar() {
    VeLiveSDKHelper.getMainWindow()?.setWindowSystemBarProperties({
      statusBarContentColor: this.statusBarContentColor
    }).catch((_: BusinessError<void>) => {
    })
  }

  @Local fillModeIndex: number = 1

  private makeFillModeDialog(onFillModeCallback: (fillMode: VeLivePlayerFillMode) => void): CustomDialogController {
    return new CustomDialogController({
      alignment: DialogAlignment.Bottom,
      isModal: true,
      builder: SelectDialog({
        title: '',
        selectedIndex: this.fillModeIndex,
        radioContent: [
          {
            title: $r('app.string.fill_mode_aspect_fill'),
            action: () => {
              this.fillModeIndex = 0
              onFillModeCallback(VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill)
            }
          },
          {
            title: $r('app.string.fill_mode_aspect_fit'),
            action: () => {
              this.fillModeIndex = 1
              onFillModeCallback(VeLivePlayerFillMode.VeLivePlayerFillModeAspectFit)
            }
          },
          {
            title: $r('app.string.fill_mode_full_fill'),
            action: () => {
              this.fillModeIndex = 2
              onFillModeCallback(VeLivePlayerFillMode.VeLivePlayerFillModeFullFill)
            }
          },
        ]
      }),
    })
  }
}

@Builder
export function VeLivePullStreamBuilder() {
  VeLivePullStream();
}