//
//  VeLivePictureInPictureContentSourceController.m
//  VELPictureInPictureDemo
//
//  Created by ByteDance on 2025/2/20.
//

#import "VeLivePictureInPictureContentSourceController.h"
#import <VideoToolbox/VideoToolbox.h>
#define LOG_TAG @"VeLivePictureInPictureContentSourceController"


void VePipAsyncRunOnMainThread(void (^ _Nullable block)(void)) {
    if (block) {
        if ([NSThread isMainThread]) {
            block();
        } else {
            dispatch_async(dispatch_get_main_queue(), block);
        }
    }
}

void VePipSyncRunOnMainThread(void (^ _Nullable block)(void)) {
    if (block) {
        if ([NSThread isMainThread]) {
            block();
        } else {
            dispatch_sync(dispatch_get_main_queue(), block);
        }
    }
}

static BOOL _isPictureInPictureStarted = NO;

@interface VePictureInPictureVideoContainerView: UIView
@property (nonatomic, strong) AVSampleBufferDisplayLayer *sampleLayer;
@property (nonatomic, copy) AVLayerVideoGravity videoGravity;
@property (nonatomic, weak) UIView *playerView;
@property (nonatomic) CGSize videoSize;
- (void)setRenderMode:(VePictureInPictureRenderMode)renderMode;
@end

@implementation VePictureInPictureVideoContainerView

- (instancetype)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        [self addSampleLayer];
    }
    return self;
}

- (void)addSampleLayer {
    self.sampleLayer = [[AVSampleBufferDisplayLayer alloc] init];
    self.sampleLayer.videoGravity = AVLayerVideoGravityResizeAspect;
    self.sampleLayer.opaque = YES;
    self.sampleLayer.frame = self.bounds;
    [self.layer addSublayer:self.sampleLayer];
}

- (void)setVideoGravity:(AVLayerVideoGravity)videoGravity {
    _videoGravity = videoGravity;
    _sampleLayer.videoGravity = videoGravity;
}

- (void)setRenderMode:(VePictureInPictureRenderMode)renderMode {
    if (renderMode == VePictureInPictureRenderModeAspectFill) {
        self.sampleLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    } else if (renderMode == VePictureInPictureRenderModeAspectFit) {
        self.sampleLayer.videoGravity = AVLayerVideoGravityResizeAspect;
    } else if (renderMode == VePictureInPictureRenderModeFullFill) {
        self.sampleLayer.videoGravity = AVLayerVideoGravityResize;
    }
}

- (void)setVideoSize:(CGSize)videoSize {
    _videoSize = videoSize;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.sampleLayer.frame = self.bounds;
}

- (void)layoutWithPlayerViewIfNeed {
    if (!self.playerView || !self.playerView.superview) {
        return;
    }
    
    if ([self.playerView.superview isEqual:self.superview]) {
        if (fabsf(self.frame.origin.x - self.playerView.frame.origin.x) >= 1 || fabsf(self.frame.origin.y - self.playerView.frame.origin.y) >= 1
            || fabsf(self.frame.size.width - self.playerView.frame.size.width) > 1) {
            self.frame = self.playerView.frame;
        }
    } else {
        [self removeFromSuperview];
        [self.playerView.superview insertSubview:self belowSubview:self.playerView];
        self.frame = self.playerView.frame;
    }
}

- (void)dealloc {
}

@end

@class VeLivePictureInPictureContentSourceController;

API_AVAILABLE(ios(15.0))
@interface VePictureInPicturePlaybackDelegateProxy : NSObject <AVPictureInPictureSampleBufferPlaybackDelegate>
@property (nonatomic, weak) VeLivePictureInPictureContentSourceController *controller;
- (instancetype)initWithController:(VeLivePictureInPictureContentSourceController *)controller;
@end

@interface VeLivePictureInPictureContentSourceController ()<AVPictureInPictureControllerDelegate>
@property (nonatomic, strong) VePictureInPictureVideoContainerView *containerView;
@property (nonatomic, strong) AVPictureInPictureController *avPipController;
@property (nonatomic) BOOL isCheckLayer;
@end

@implementation VeLivePictureInPictureContentSourceController

- (instancetype)initWithContentView:(UIView *)playerView {
    self = [super init];
    if (self) {
        self.containerView = [[VePictureInPictureVideoContainerView alloc]init];
        self.containerView.frame = playerView.frame;
        self.containerView.playerView = playerView;
    }
    return self;
}

- (void)dealloc {
    VePipSyncRunOnMainThread(^{
        [self.containerView removeFromSuperview];
    });
}

- (BOOL)setupPictureInPicture {
    if (@available(iOS 15.0, *)) {
        id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegateProxy = [[VePictureInPicturePlaybackDelegateProxy alloc] initWithController:self];
        AVPictureInPictureControllerContentSource *contentSource = [[AVPictureInPictureControllerContentSource alloc]
                                                                    initWithSampleBufferDisplayLayer:self.containerView.sampleLayer
                                                                                    playbackDelegate:playbackDelegateProxy];
        self.avPipController = [[AVPictureInPictureController alloc] initWithContentSource:contentSource];
        self.avPipController.delegate = self;
        if (@available(iOS 14.0, *)) {
            self.avPipController.requiresLinearPlayback = self.requiresLinearPlayback;
        }
        if (@available(iOS 14.2, *)) {
            self.avPipController.canStartPictureInPictureAutomaticallyFromInline = self.canStartPictureInPictureAutomaticallyFromInline;
        }
        return self.avPipController != nil;
    }
    return NO;
}

- (void)startPictureInPicture {
    [_avPipController startPictureInPicture];
}

- (void)stopPictureInPicture {
    [_avPipController stopPictureInPicture];
}

- (AVPictureInPictureController *) pictureInPictureController {
    return self.avPipController;
}

- (void)setRenderMode:(VePictureInPictureRenderMode)renderMode {
    _renderMode = renderMode;
    if (renderMode == VePictureInPictureRenderModeAspectFill) {
        self.containerView.videoGravity = AVLayerVideoGravityResizeAspectFill;
    } else if (renderMode == VePictureInPictureRenderModeAspectFit) {
        self.containerView.videoGravity = AVLayerVideoGravityResizeAspect;
    } else if (renderMode == VePictureInPictureRenderModeFullFill) {
        self.containerView.videoGravity = AVLayerVideoGravityResize;
    }
}

- (void)setVideoSize:(CGSize)videoSize {
    _videoSize = videoSize;
    self.containerView.videoSize = videoSize;
}

- (void)enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer {
    [self enqueuePixelBuffer:pixelBuffer callback:nil];
}

- (void)enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer callback:(void(^)(BOOL))callback API_AVAILABLE(ios(15.0)) {
    [self _enqueuePixelBuffer:pixelBuffer callback:callback];
    
    BOOL check = NO;
    @synchronized (self) {
        check = self.isCheckLayer;
    }
    if (check) {
        VePipAsyncRunOnMainThread(^{
            [self.containerView layoutWithPlayerViewIfNeed];
        });
    }
}

- (void)invalidatePlaybackState {
    [self.avPipController invalidatePlaybackState];
}

- (void)enablePicutreInPictureView:(BOOL)enable {
    @synchronized (self) {
        self.isCheckLayer = enable;
    }
    if (enable) {
        [self.containerView layoutWithPlayerViewIfNeed];
    } else {
        [self.containerView removeFromSuperview];
    }
}

- (void)setRequiresLinearPlayback:(BOOL)requiresLinearPlayback {
    _requiresLinearPlayback = requiresLinearPlayback;
    _avPipController.requiresLinearPlayback = requiresLinearPlayback;
}

- (void)_enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer callback:(void(^)(BOOL))callback API_AVAILABLE(ios(15.0)) {
    if (pixelBuffer == NULL) {
        if (callback) {
            callback(NO);
        }
        return;
    }
    
    CMVideoFormatDescriptionRef videoInfo = NULL;
    OSStatus result = CMVideoFormatDescriptionCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, &videoInfo);
    if (result != noErr || videoInfo == NULL) {
        if (videoInfo) {
            CFRelease(videoInfo);
        }
        if (callback) {
            callback(NO);
        }
        return;
    }
    
    CMSampleTimingInfo timing = {kCMTimeInvalid, kCMTimeInvalid, kCMTimeInvalid};
    
    CMSampleBufferRef sampleBuffer = NULL;
    result = CMSampleBufferCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, true, NULL, NULL, videoInfo, &timing, &sampleBuffer);
    CFRelease(videoInfo);
    if (result != noErr || sampleBuffer == NULL) {
        if (callback) {
            callback(NO);
        }
        return;
    }
    
    CFArrayRef attachments = CMSampleBufferGetSampleAttachmentsArray(sampleBuffer, YES);
    if (attachments && CFArrayGetCount(attachments) > 0) {
        CFTypeRef first = CFArrayGetValueAtIndex(attachments, 0);
        if (first && CFGetTypeID(first) == CFDictionaryGetTypeID()) {
            CFMutableDictionaryRef dict = (CFMutableDictionaryRef)first;
            CFDictionarySetValue(dict, kCMSampleAttachmentKey_DisplayImmediately, kCFBooleanTrue);
        }
    }
    
    if (!sampleBuffer) {
        if (callback) {
            callback(NO);
        }
        return;
    }
    if (!self.containerView.sampleLayer.readyForMoreMediaData) {
        CFRelease(sampleBuffer);
        if (callback) {
            callback(NO);
        }
        return;
    }
    if (@available(iOS 16.0, *)) {
        if (self.containerView.sampleLayer.status == AVQueuedSampleBufferRenderingStatusFailed) {
            [self.containerView.sampleLayer flush];
        }
    } else {
        [self.containerView.sampleLayer flush];
    }
    
    if (@available(iOS 15.0, *)) {
        [self.containerView.sampleLayer enqueueSampleBuffer:sampleBuffer];
        CFRelease(sampleBuffer);
        if (callback) {
            callback(YES);
        }
    } else {
        VePipAsyncRunOnMainThread(^{
            [self.containerView.sampleLayer enqueueSampleBuffer:sampleBuffer];
            CFRelease(sampleBuffer);
            if (callback) {
                callback(YES);
            }
        });
    }
}

- (void)setCanStartPictureInPictureAutomaticallyFromInline:(BOOL)canStartPictureInPictureAutomaticallyFromInline API_AVAILABLE(ios(14.2)) {
    _canStartPictureInPictureAutomaticallyFromInline = canStartPictureInPictureAutomaticallyFromInline;
    _avPipController.canStartPictureInPictureAutomaticallyFromInline = canStartPictureInPictureAutomaticallyFromInline;
}

+ (BOOL)isPictureInPictureSupported {
    return [AVPictureInPictureController isPictureInPictureSupported];
}

+ (BOOL)isPictureInPictureStarted {
    return _isPictureInPictureStarted;
}

- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    self.containerView.playerView.hidden = YES;
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureControllerWillStartPictureInPicture:)]) {
        [self.pictureInPictureDelegate pictureInPictureControllerWillStartPictureInPicture:pictureInPictureController];
    }
}

- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    self.containerView.playerView.hidden = YES;
    _isPictureInPictureStarted = YES;
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureControllerDidStartPictureInPicture:)]) {
        [self.pictureInPictureDelegate pictureInPictureControllerDidStartPictureInPicture:pictureInPictureController];
    }
}

- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    _isPictureInPictureStarted = NO;
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureControllerWillStopPictureInPicture:)]) {
        [self.pictureInPictureDelegate pictureInPictureControllerWillStopPictureInPicture:pictureInPictureController];
    }
}

- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    _isPictureInPictureStarted = NO;
    self.containerView.playerView.hidden = NO;
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureControllerDidStopPictureInPicture:)]) {
        [self.pictureInPictureDelegate pictureInPictureControllerDidStopPictureInPicture:pictureInPictureController];
    }
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL restored))completionHandler {
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureController:restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:)]) {
        [self.pictureInPictureDelegate pictureInPictureController:pictureInPictureController restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:completionHandler];
    } else {
        completionHandler(YES);
    }
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController failedToStartPictureInPictureWithError:(NSError *)error {
    if (self.pictureInPictureDelegate && [self.pictureInPictureDelegate respondsToSelector:@selector(pictureInPictureController:failedToStartPictureInPictureWithError:)]) {
        [self.pictureInPictureDelegate pictureInPictureController:pictureInPictureController failedToStartPictureInPictureWithError:error];
    }
}

@end

@implementation VePictureInPicturePlaybackDelegateProxy

- (instancetype)initWithController:(VeLivePictureInPictureContentSourceController *)controller {    
    if (self = [super init]) {
        _controller = controller;
    }
    return self;
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController setPlaying:(BOOL)playing {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureController:setPlaying:)]) {
        [playbackDelegate pictureInPictureController:pictureInPictureController setPlaying:playing];
    }
}

- (CMTimeRange)pictureInPictureControllerTimeRangeForPlayback:(AVPictureInPictureController *)pictureInPictureController {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureControllerTimeRangeForPlayback:)]) {
        return [playbackDelegate pictureInPictureControllerTimeRangeForPlayback:pictureInPictureController];
    }
    return CMTimeRangeMake(kCMTimeNegativeInfinity, kCMTimePositiveInfinity);
}

- (BOOL)pictureInPictureControllerIsPlaybackPaused:(AVPictureInPictureController *)pictureInPictureController {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureControllerIsPlaybackPaused:)]) {
        return [playbackDelegate pictureInPictureControllerIsPlaybackPaused:pictureInPictureController];
    }
    return NO;
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController didTransitionToRenderSize:(CMVideoDimensions)newRenderSize {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureController:didTransitionToRenderSize:)]) {
        [playbackDelegate pictureInPictureController:pictureInPictureController didTransitionToRenderSize:newRenderSize];
    }
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController skipByInterval:(CMTime)skipInterval completionHandler:(void (^)(void))completionHandler {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureController:skipByInterval:completionHandler:)]) {
        [playbackDelegate pictureInPictureController:pictureInPictureController skipByInterval:skipInterval completionHandler:completionHandler];
        return;
    }
    if (completionHandler) {
        completionHandler();
    }
}

- (BOOL)pictureInPictureControllerShouldProhibitBackgroundAudioPlayback:(AVPictureInPictureController *)pictureInPictureController {
    id<AVPictureInPictureSampleBufferPlaybackDelegate> playbackDelegate = self.controller.pictureInPicturePlaybackDelegate;
    if ([playbackDelegate respondsToSelector:@selector(pictureInPictureControllerShouldProhibitBackgroundAudioPlayback:)]) {
        return [playbackDelegate pictureInPictureControllerShouldProhibitBackgroundAudioPlayback:pictureInPictureController];
    }
    return NO;
}

@end
