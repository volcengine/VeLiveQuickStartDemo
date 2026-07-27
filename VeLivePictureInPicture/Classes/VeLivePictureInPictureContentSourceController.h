//
//  VeLivePictureInPictureContentSourceController.h
//  VELPictureInPictureDemo
//
//  Created by ByteDance on 2025/2/20.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVKit/AVKit.h>

typedef NS_CLOSED_ENUM(NSInteger, VePictureInPictureRenderMode) {
    VePictureInPictureRenderModeAspectFill = 0,
    VePictureInPictureRenderModeAspectFit = 1,
    VePictureInPictureRenderModeFullFill = 2,
};

void VePipAsyncRunOnMainThread(void (^ _Nullable block)(void));
void VePipSyncRunOnMainThread(void (^ _Nullable block)(void));


NS_ASSUME_NONNULL_BEGIN
API_AVAILABLE(ios(15.0))
@interface VeLivePictureInPictureContentSourceController : NSObject
@property (nonatomic, assign) BOOL canStartPictureInPictureAutomaticallyFromInline;
@property (nonatomic, weak, nullable) id<AVPictureInPictureSampleBufferPlaybackDelegate> pictureInPicturePlaybackDelegate;
@property (nonatomic, weak, nullable) id<AVPictureInPictureControllerDelegate> pictureInPictureDelegate;
@property (nonatomic, assign) CGSize videoSize;
// 是否使用快进、快退按钮， 默认 NO
@property (nonatomic, assign) BOOL requiresLinearPlayback API_AVAILABLE(ios(14.0));
@property (nonatomic, strong, readonly, nullable) AVPictureInPictureController *pictureInPictureController;
// 设置视频画中画填充模式
@property (nonatomic, assign) VePictureInPictureRenderMode renderMode;

// 是否支持画中画
@property (class, nonatomic, assign, readonly) BOOL isPictureInPictureSupported;

// 当前是否已经有画中画在播放
@property (class, nonatomic, assign, readonly) BOOL isPictureInPictureStarted;

- (instancetype)initWithContentView:(UIView *)playerView;

- (BOOL)setupPictureInPicture;

// 开启画中画，只要画中画可用时才能调用成功
- (void)startPictureInPicture;

// 关闭画中画
- (void)stopPictureInPicture;

// 持续送入视频帧
- (void)enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer API_AVAILABLE(ios(15.0));

- (void)enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer callback:(void(^)(BOOL))callback API_AVAILABLE(ios(15.0));

- (void)invalidatePlaybackState;

- (void)enablePicutreInPictureView:(BOOL)enable;
@end

NS_ASSUME_NONNULL_END
