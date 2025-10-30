import {
  VeLivePlayer,
  VeLivePlayerError,
  VeLivePlayerObserver,
  VeLivePlayerResolution,
  VeLivePlayerResolutionSwitchReason,
  VeLivePlayerStatistics,
  VeLivePlayerStatus,
  VeLivePlayerStreamType
} from "@livecore/liveplayer";
import { image } from "@kit.ImageKit";
import { emitter } from '@kit.BasicServicesKit';

export type  VeLivePlayerEventId = 'error' | 'statistics' | 'videoSize' | 'playStatus'

export class VeLivePlayerObserverImp implements VeLivePlayerObserver {

  onError(player: VeLivePlayer, error: VeLivePlayerError): void {
    // 错误回调
    this.post<VeLivePlayerError>('error', error)
  }

  onFirstVideoFrameRender(player: VeLivePlayer, isFirstFrame: boolean): void {
    // 视频首帧回调
  }

  onFirstAudioFrameRender(player: VeLivePlayer, isFirstFrame: boolean): void {
    // 音频首帧回调
  }

  onStallStart(player: VeLivePlayer): void {
    // 卡顿开始回调
  }

  onStallEnd(player: VeLivePlayer): void {
    // 卡顿结束回调
  }

  onVideoRenderStall(player: VeLivePlayer, stallTime: number): void {
    // 视频渲染卡顿
  }

  onAudioRenderStall(player: VeLivePlayer, stallTime: number): void {
    // 音频渲染卡顿
  }

  onResolutionSwitch(player: VeLivePlayer, resolution: VeLivePlayerResolution, error: VeLivePlayerError,
    reason: VeLivePlayerResolutionSwitchReason): void {
    // 清晰度切换回调
  }

  onVideoSizeChanged(player: VeLivePlayer, width: number, height: number): void {
    // 视频尺寸变化回调
    this.post<Size>('videoSize', {width: width, height: height})
  }

  onMainBackupSwitch(player: VeLivePlayer, streamType: VeLivePlayerStreamType, error: VeLivePlayerError): void {
    // 主备切换回调
  }

  onPlayerStatusUpdate(player: VeLivePlayer, status: VeLivePlayerStatus): void {
    // 播放器状态变更回调
    this.post<VeLivePlayerStatus>('playStatus', status)
  }

  onStatistics(player: VeLivePlayer, statistics: VeLivePlayerStatistics): void {
    // 拉流周期性信息回调
    this.post<VeLivePlayerStatistics>('statistics', statistics)
  }

  onSnapshotComplete(player: VeLivePlayer, bitmap: image.PixelMap): void {
    // 截图完成回调
  }

  public on<T>(eventId: VeLivePlayerEventId, callback: Callback<T>) {
    emitter.on(eventId.toString(), (eventData: emitter.GenericEventData<T>) => {
      callback(eventData.data);
    });
  }

  public off(eventId: VeLivePlayerEventId) {
    emitter.off(eventId.toString())
  }
  private  post<T>(eventId: VeLivePlayerEventId, eventData: T, priority: emitter.EventPriority = emitter.EventPriority.HIGH) {
    const genericEventData: emitter.GenericEventData<T> = { data: eventData };
    const options: emitter.Options = { priority: priority };
    emitter.emit(eventId.toString(), options, genericEventData);
  }
}

