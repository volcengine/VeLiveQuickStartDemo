
/**
 Copyright (c) 2023 The VeLivePlayer project authors. All Rights Reserved.
 @brief VeLivePlayer
*/

#if __has_include(<TTSDK/VeLivePlayer.h>)
#import <TTSDK/VeLivePlayer.h>
#elif __has_include(<TTSDKFramework/TTSDKFramework.h>)
#import <TTSDKFramework/TTSDKFramework.h>
#elif __has_include("VeLivePlayer.h")
#import "VeLivePlayer.h"
#endif
#import <AVKit/AVKit.h>
/**
 * @locale zh
 * @type api
 * @brief 直播播放器的画中画能力。
 */
/**
 * @locale en
 * @type api
 * @brief Picture-in-Picture (PiP) capability for the live player.
 */
@interface TVLManager (PictureInPicture)

/**
 * @locale zh
 * @type api
 * @brief 是否开启画中画功能，默认值为 `NO`。
 *        - `YES`：开启画中画功能，之后可调用画中画相关方法或设置画中画相关属性。
 *        - `NO`：禁用画中画功能。
 * @note 将该属性设为 `YES` 前，请先满足以下条件：
 *       - 当前系统为 iOS 15.0 及以上版本。
 *       - App 的 **Background Modes** 中已开启 **Audio, AirPlay, and Picture in Picture**。
 */
/**
 * @locale en
 * @type api
 * @brief Whether to enable the PiP feature. The default value is `NO`.
 *        - `YES`: Enables the PiP feature. Then, you can call PiP-related methods or set PiP-related properties.
 *        - `NO`: Disables the PiP feature.
 * @note Before setting this property to `YES`, make sure the following conditions are met:
 *       - The system is running iOS 15.0 or later.
 *       - The app has enabled **Audio, AirPlay, and Picture in Picture** in **Background Modes**.
 */
@property (nonatomic, assign) BOOL enablePictureInPicture;

/**
 * @locale zh
 * @type api
 * @brief 画中画窗口是否隐藏快进、快退按钮，默认值为 `YES`。取值如下：
 *        - `YES`：不显示快进、快退按钮。
 *        - `NO`：显示快进、快退按钮。
 */
/**
 * @locale en
 * @type api
 * @brief Whether fast-forward and rewind buttons are hidden in the PiP window. The default value is `YES`. Valid values:
 *        - `YES`: Fast-forward and rewind buttons are hidden.
 *        - `NO`: Fast-forward and rewind buttons are shown.
 */
@property (nonatomic, assign) BOOL pictureInPictureRequiresLinearPlayback;

/**
 * @locale zh
 * @type api
 * @brief 是否允许在 App 切到后台时自动启动画中画。默认值为 `YES`。取值如下：
 *        - `YES`：App 切到后台时，系统会尝试自动启动画中画。
 *        - `NO`：App 切到后台时，系统不会自动启动画中画。
 */
/**
 * @locale en
 * @type api
 * @brief Whether PiP can start automatically when the app moves to the background. The default value is `YES`. Valid values:
 *        - `YES`: Allows the system to attempt to start PiP automatically when the app moves to the background.
 *        - `NO`: Prevents the system from starting PiP automatically when the app moves to the background.
 */
@property (nonatomic, assign) BOOL canStartPictureInPictureAutomaticallyFromInline;

/**
 * @locale zh
 * @type api
 * @brief 当前播放器关联的 [AVPictureInPictureController](https://developer.apple.com/documentation/AVKit/AVPictureInPictureController) 实例，可用于读取系统级画中画状态等信息。
 */
/**
 * @locale en
 * @type api
 * @brief The [AVPictureInPictureController](https://developer.apple.com/documentation/AVKit/AVPictureInPictureController) instance associated with the current player, which can be used to read system-level PiP state and related information.
 */
@property (nonatomic, strong, readonly, nullable) AVPictureInPictureController *avPictureInPictureController;

/**
 * @locale zh
 * @type api
 * @brief 实现 [AVPictureInPictureControllerDelegate](https://developer.apple.com/documentation/avkit/avpictureinpicturecontrollerdelegate) 的代理对象，用于接收画中画开始、结束、失败等生命周期回调。
 * @note SDK 已提供基础画中画能力。如需自定义相关代理回调的处理逻辑，可通过该属性设置代理对象。
 */
/**
 * @locale en
 * @type api
 * @brief The delegate object that implements [AVPictureInPictureControllerDelegate](https://developer.apple.com/documentation/avkit/avpictureinpicturecontrollerdelegate), which is used to receive PiP lifecycle callbacks, such as start, stop, and failure events.
 * @note The SDK provides basic PiP features. To customize the handling of related delegate callbacks, set this property to your delegate object.
  */
@property (nonatomic, weak, nullable) id<AVPictureInPictureControllerDelegate> pictureInPictureDelegate;

/**
 * @locale zh
 * @type api
 * @brief 实现 [AVPictureInPictureSampleBufferPlaybackDelegate](https://developer.apple.com/documentation/avkit/avpictureinpicturesamplebufferplaybackdelegate) 的代理对象，用于处理画中画中的播放/暂停请求、指定画中画窗口中的播放控件显示为播放还是暂停，以及提供可播放时间范围。
 * @note SDK 已提供基础画中画能力。如需自定义相关代理回调的处理逻辑，可通过该属性设置代理对象。
 */
/**
 * @locale en
 * @type api
 * @brief The delegate object that implements [AVPictureInPictureSampleBufferPlaybackDelegate](https://developer.apple.com/documentation/avkit/avpictureinpicturesamplebufferplaybackdelegate), which is used to handle play and pause requests in PiP, determine whether the playback controls in the PiP window should display Play or Pause, and provide the playable time range.
 * @note The SDK provides basic PiP features. To customize the handling of related delegate callbacks, set this property to your delegate object.
 */
@property (nonatomic, weak, nullable) id<AVPictureInPictureSampleBufferPlaybackDelegate> pictureInPicturePlaybackDelegate;

/**
 * @locale zh
 * @type api
 * @brief 当前设备是否支持画中画。
 * @return  
 *         - `YES`：支持。
 *         - `NO`：不支持。
 */
/**
 * @locale en
 * @type api
 * @brief Whether PiP is supported on the current device.
 * @return  
 *         - `YES`: Supported.
 *         - `NO`: Not supported.
 */
+ (BOOL) isPictureInPictureSupported;

/**
 * @locale zh
 * @type api
 * @brief 是否已成功启动画中画。
 * @return  
 *         - `YES`：已成功启动画中画。
 *         - `NO`：未启动画中画。
 */
/**
 * @locale en
 * @type api
 * @brief Whether PiP has started successfully.
 * @return  
 *         - `YES`: PiP has started successfully.
 *         - `NO`: PiP has not started.
 */
+ (BOOL) isPictureInPictureStarted;

/**
 * @locale zh
 * @type api
 * @brief 手动启动画中画。
 */
/**
 * @locale en
 * @type api
 * @brief Manually start PiP.
 */
- (void) startPictureInPicture;

/**
 * @locale zh
 * @type api
 * @brief 手动停止画中画，关闭画中画窗口。
 */
/**
 * @locale en
 * @type api
 * @brief Manually stop PiP and close the PiP window.
 */
- (void) stopPictureInPicture;

/**
 * @locale zh
 * @type api
 * @brief 手动更新画中画的播放按钮状态和进度显示。
 */
/**
 * @locale en
 * @type api
 * @brief Manually update the play/pause button state and progress display in the PiP window.
 */
- (void) invalidatePictureInPicturePlaybackState;
@end
