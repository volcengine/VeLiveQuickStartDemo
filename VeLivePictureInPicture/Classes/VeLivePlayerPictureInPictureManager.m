
#include "VeLivePlayerPictureInPictureManager.h"
#include "VeLivePictureInPictureContentSourceController.h"
#include "VeLivePlayer+PictureInPicture.h"
#if __has_include(<TTSDK/VeLivePlayer.h>)
#import <TTSDK/VeLivePlayer.h>
#elif __has_include(<TTSDKFramework/TTSDKFramework.h>)
#import <TTSDKFramework/TTSDKFramework.h>
#endif

@interface VeLivePlayerPictureInPictureManager()
@property (nonatomic, weak) TVLManager *player;
- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController;
- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController;

@end



@interface VeLivePictureInPictureDelegate: VeLiveDelegateForwardObject <AVPictureInPictureControllerDelegate>
@property (nonatomic, weak) VeLivePlayerPictureInPictureManager *playerManager;
- (instancetype)initWithPlayerManager:(VeLivePlayerPictureInPictureManager *)playerManager;
@end

@implementation VeLivePictureInPictureDelegate

- (instancetype)initWithPlayerManager:(VeLivePlayerPictureInPictureManager *)playerManager {
   if (self = [super init]) {
       _playerManager = playerManager;
   }
   return self;
}

- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    [self.playerManager pictureInPictureControllerWillStartPictureInPicture:pictureInPictureController];
    id<AVPictureInPictureControllerDelegate> origin = (id<AVPictureInPictureControllerDelegate>)self.veOriginDelegate;
    if ([origin respondsToSelector:_cmd]) {
        [origin pictureInPictureControllerWillStartPictureInPicture:pictureInPictureController];
    }
}

- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    [self.playerManager pictureInPictureControllerDidStopPictureInPicture:pictureInPictureController];
    id<AVPictureInPictureControllerDelegate> origin = (id<AVPictureInPictureControllerDelegate>)self.veOriginDelegate;
    if ([origin respondsToSelector:_cmd]) {
        [origin pictureInPictureControllerDidStopPictureInPicture:pictureInPictureController];
    }
}
@end

@interface VeLivePictureInPicturePlayBackDelegate: VeLiveDelegateForwardObject <AVPictureInPictureSampleBufferPlaybackDelegate>
@property (nonatomic, weak) VeLivePlayerPictureInPictureManager *playerManager;
@property (nonatomic, assign) BOOL pipSetPlaying;
- (instancetype)initWithPlayerManager:(VeLivePlayerPictureInPictureManager *)playerManager;
@end

@implementation VeLivePictureInPicturePlayBackDelegate
- (instancetype)initWithPlayerManager:(VeLivePlayerPictureInPictureManager *)playerManager {
   if (self = [super init]) {
       _playerManager = playerManager;
   }
   return self;
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController setPlaying:(BOOL)playing {
    self.pipSetPlaying = playing;
    if (self.playerManager) {
        id<AVPictureInPictureSampleBufferPlaybackDelegate> origin = (id<AVPictureInPictureSampleBufferPlaybackDelegate>)self.veOriginDelegate;
        if (origin && [origin respondsToSelector:_cmd]) {
            [origin pictureInPictureController:pictureInPictureController setPlaying:playing];
        } else {
            if (playing) {
                [self.playerManager.player play];
            } else {
                [self.playerManager.player stop];
            }
        }
    }
}

- (BOOL)pictureInPictureControllerIsPlaybackPaused:(AVPictureInPictureController *)pictureInPictureController {
    if (self.playerManager.player) {
        id<AVPictureInPictureSampleBufferPlaybackDelegate> origin = (id<AVPictureInPictureSampleBufferPlaybackDelegate>)self.veOriginDelegate;
        if (origin && [origin respondsToSelector:_cmd]) {
            return [origin pictureInPictureControllerIsPlaybackPaused:pictureInPictureController];
        } else {
            BOOL paused = !(self.playerManager.player.isPlaying || self.pipSetPlaying);
            return paused;
        }
    }
    return NO;
}

- (CMTimeRange)pictureInPictureControllerTimeRangeForPlayback:(AVPictureInPictureController *)pictureInPictureController {
    if (self.playerManager.player) {
        if (self.playerManager.player.pictureInPicturePlaybackDelegate && [self.playerManager.player.pictureInPicturePlaybackDelegate respondsToSelector:@selector(pictureInPictureControllerTimeRangeForPlayback:)]) {
            return [self.playerManager.player.pictureInPicturePlaybackDelegate pictureInPictureControllerTimeRangeForPlayback:pictureInPictureController];
        } else {
            if (self.playerManager.requiresLinearPlayback) {
                return CMTimeRangeMake(CMTimeMake(0, 1), CMTimeMake(100, 1));
            } else {
                return CMTimeRangeMake(kCMTimeNegativeInfinity, kCMTimePositiveInfinity);
            }
        }
    }
    return CMTimeRangeMake(kCMTimeNegativeInfinity, kCMTimePositiveInfinity);
}

@end

@interface VeLivePlayerPictureInPictureManager()
@property (nonatomic, strong) VeLivePictureInPictureContentSourceController *controler;
@property (nonatomic) NSInteger width;
@property (nonatomic) NSInteger height;
@property (nonatomic, strong) VeLivePictureInPicturePlayBackDelegate *pipPlaybackDelegate;
@property (nonatomic, strong) VeLivePictureInPictureDelegate *pipDelegate;
@property (nonatomic) BOOL isPipManaalStarted;
@property (nonatomic) BOOL isPipDidStarted;
@property (nonatomic) NSUInteger frameCount;
@end

@implementation VeLivePlayerPictureInPictureManager

+ (BOOL)isPictureInPictureSupported {
    return [VeLivePictureInPictureContentSourceController isPictureInPictureSupported];
}

+ (BOOL)isPictureInPictureStarted {
    return [VeLivePictureInPictureContentSourceController isPictureInPictureStarted];
}

- (instancetype)initWithPlayer:(TVLManager *)player {
    self = [super init];
    if (self) {
        self.player = player;
    }
    return self;
}

- (void)setEnablePictureInPicture:(BOOL)enablePictureInPicture {
    _enablePictureInPicture = enablePictureInPicture;
    if (!self.pipPlaybackDelegate) {
        self.pipPlaybackDelegate = [[VeLivePictureInPicturePlayBackDelegate alloc]initWithPlayerManager:self];
        self.controler.pictureInPicturePlaybackDelegate = self.pipPlaybackDelegate;
    }
    [self.controler enablePicutreInPictureView:enablePictureInPicture];
}

- (void)setPictureInPicturePlaybackDelegate:(id<AVPictureInPictureSampleBufferPlaybackDelegate>)pictureInPicturePlaybackDelegate {
    if (!self.pipPlaybackDelegate) {
        self.pipPlaybackDelegate = [[VeLivePictureInPicturePlayBackDelegate alloc]initWithPlayerManager:self];
        self.controler.pictureInPicturePlaybackDelegate = self.pipPlaybackDelegate;
    }
    self.pipPlaybackDelegate.veOriginDelegate = pictureInPicturePlaybackDelegate;
}

- (id<AVPictureInPictureSampleBufferPlaybackDelegate>)pictureInPicturePlaybackDelegate {
    return self.pipPlaybackDelegate.veOriginDelegate;
}

- (void)setCanStartPictureInPictureAutomaticallyFromInline:(BOOL)canStartPictureInPictureAutomaticallyFromInline {
    if (@available(iOS 14.2, *)) {
        self.controler.canStartPictureInPictureAutomaticallyFromInline = canStartPictureInPictureAutomaticallyFromInline;
        [self.controler enablePicutreInPictureView:canStartPictureInPictureAutomaticallyFromInline];
    }
}

- (BOOL)canStartPictureInPictureAutomaticallyFromInline {
    return self.controler.canStartPictureInPictureAutomaticallyFromInline;
}

- (BOOL)setupPictureInPicture {
    if (!self.pipPlaybackDelegate) {
        self.pipPlaybackDelegate = [[VeLivePictureInPicturePlayBackDelegate alloc]initWithPlayerManager:self];
    }
    if (!self.pipDelegate) {
        self.pipDelegate = [[VeLivePictureInPictureDelegate alloc] initWithPlayerManager:self];
    }
    self.controler = [[VeLivePictureInPictureContentSourceController alloc] initWithContentView:self.player.playerView];
    self.controler.pictureInPictureDelegate = self.pipDelegate;
    self.controler.pictureInPicturePlaybackDelegate = self.pipPlaybackDelegate;
    self.canStartPictureInPictureAutomaticallyFromInline = YES;
    self.controler.videoSize = CGSizeMake(self.width, self.height);
    if (@available(iOS 14.0, *)) {
        self.controler.requiresLinearPlayback = self.requiresLinearPlayback;
    }
    return [self.controler setupPictureInPicture];
}

- (void)setRequiresLinearPlayback:(BOOL)requiresLinearPlayback {
    _requiresLinearPlayback = requiresLinearPlayback;
    self.controler.requiresLinearPlayback = requiresLinearPlayback;
}

- (void)startPictureInPicture {
    @synchronized (self) {
        self.isPipManaalStarted = YES;
    }
    [self.controler enablePicutreInPictureView:YES];
    [self.controler startPictureInPicture];
}

- (void)stopPictureInPicture {
    @synchronized (self) {
        self.isPipManaalStarted = NO;
    }
    [self.controler stopPictureInPicture];
}

- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    @synchronized (self) {
        self.isPipDidStarted = YES;
    }
}

- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    self.isPipManaalStarted = NO;
    self.isPipDidStarted = NO;
    if (!self.canStartPictureInPictureAutomaticallyFromInline) {
        [self.controler enablePicutreInPictureView:NO];
    }
}

- (AVPictureInPictureController *)pictureInPictureController {
    return self.controler.pictureInPictureController;
}

- (void) setVideoWidth:(int)width height:(int)height {
    BOOL needUpdate = NO;
    @synchronized (self) {
        if (self.width != width || self.height != height) {
            needUpdate = YES;
            self.width = width;
            self.height = height;
        }
    }
    
    if (needUpdate) {
        VePipAsyncRunOnMainThread(^{
            self.controler.videoSize = CGSizeMake(width, height);
        });
    }
}

- (void) setRenderFillMode:(VePictureInPictureRenderMode)mode {
    self.controler.renderMode = mode;
}

- (void)sendRenderViewFrame:(CVPixelBufferRef)pixelBuffer {
    if (pixelBuffer) {
        CVPixelBufferRetain(pixelBuffer);
        [self.controler enqueuePixelBuffer:pixelBuffer callback:^(BOOL) {
            CVPixelBufferRelease(pixelBuffer);
        }];
        
        @synchronized (self) {
            if (self.frameCount % 20 == 0) {
                VePipAsyncRunOnMainThread(^{
                    [self invalidatePlaybackState];
                });
            }
            self.frameCount ++;
        }
    }
}

- (void)dealloc {
}

- (void)invalidatePlaybackState {
    [self.controler invalidatePlaybackState];
}

- (void)setPictureInPictureDelegate:(id<AVPictureInPictureControllerDelegate>)pictureInPictureDelegate {
    if (!self.pipDelegate) {
        self.pipDelegate = [[VeLivePictureInPictureDelegate alloc] initWithPlayerManager:self];
        self.controler.pictureInPictureDelegate = self.pipDelegate;
    }
    self.pipDelegate.veOriginDelegate = pictureInPictureDelegate;
}

- (id<AVPictureInPictureControllerDelegate>)pictureInPictureDelegate {
    return self.pipDelegate.veOriginDelegate;
}

@end


@implementation VeLiveDelegateForwardObject

- (BOOL)respondsToSelector:(SEL)aSelector {
    if ([super respondsToSelector:aSelector]) {
        return YES;
    }
    return self.veOriginDelegate && [self.veOriginDelegate respondsToSelector:aSelector];
}

- (id)forwardingTargetForSelector:(SEL)aSelector {
    if (self.veOriginDelegate && [self.veOriginDelegate respondsToSelector:aSelector]) {
        return self.veOriginDelegate;
    }
    return [super forwardingTargetForSelector:aSelector];
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector {
    NSMethodSignature *signature = [super methodSignatureForSelector:aSelector];
    if (signature) {
        return signature;
    }
    return [(NSObject *)self.veOriginDelegate methodSignatureForSelector:aSelector];
}

- (void)forwardInvocation:(NSInvocation *)anInvocation {
    if (self.veOriginDelegate && [self.veOriginDelegate respondsToSelector:anInvocation.selector]) {
        [anInvocation invokeWithTarget:self.veOriginDelegate];
        return;
    }
    [super forwardInvocation:anInvocation];
}

@end
