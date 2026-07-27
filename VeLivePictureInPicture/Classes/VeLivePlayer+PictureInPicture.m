
/**
 Copyright (c) 2023 The VeLivePlayer project authors. All Rights Reserved.
 @brief VeLivePlayer
*/

#import "VeLivePlayer+PictureInPicture.h"
#import <objc/runtime.h>
#import "VeLivePlayerPictureInPictureManager.h"

@interface VeLivePlayerVideoFrame (Pip)
@property (nonatomic, assign) CVPixelBufferRef originalPixelBuffer;
@end

@interface TVLManager (PictureInPicture)
@property (nonatomic, strong) VeLivePlayerPictureInPictureManager *pipManager;
@property (nonatomic, assign) BOOL shouldReportVideoFrame;
@end

@implementation TVLManager (PictureInPicture)

+ (void) initialize {
    [self veLiveExchangeMethod:@selector(setRenderFillMode:)
                   newSelector:@selector(pipHooking_setRenderFillMode:)];
    [self veLiveExchangeMethod:@selector(playerController:videoSizeDidChange:)
                   newSelector:@selector(pipHooking_playerController:videoSizeDidChange:)];
        [self veLiveExchangeMethod:@selector(hooking_updateExternalRenderVideoFrame:)
                       newSelector:@selector(pipHooking_processVideoFrame:)];
}

+ (void)veLiveExchangeMethod:(SEL)originSelector newSelector:(SEL)newSelector {
    Class class = [self class];
    Method oriMethod = class_getInstanceMethod(class, originSelector);
    Method newMethod = class_getInstanceMethod(class, newSelector);
    if (newMethod) {
        BOOL isAddedMethod = class_addMethod(class, originSelector, method_getImplementation(newMethod), method_getTypeEncoding(newMethod));
        if (isAddedMethod) {
            IMP oriMethodIMP = method_getImplementation(oriMethod) ?: imp_implementationWithBlock(^(id selfObject) {});
            const char *oriMethodTypeEncoding = method_getTypeEncoding(oriMethod) ?: "v@:";
            class_replaceMethod(class, newSelector, oriMethodIMP, oriMethodTypeEncoding);
        } else {
            method_exchangeImplementations(oriMethod, newMethod);
        }
    }
}

- (void)pipHooking_setRenderFillMode:(VeLivePlayerFillMode)fillMode {
    [self pipHooking_setRenderFillMode:fillMode];
    [self setPictureInPictureRenderFillMode:fillMode];
}

- (void)pipHooking_playerController:(id)controller videoSizeDidChange:(CGSize)size {
    [self pipHooking_playerController:controller videoSizeDidChange:size];
    [self setVideoSizeToPictureInPicture:size];
}

- (void)pipHooking_processVideoFrame:(CVPixelBufferRef)frame {
    [self pipHooking_processVideoFrame:frame];
    
    [self sendRenderViewFrame:frame];
}

- (void)setPipManager:(VeLivePlayerPictureInPictureManager *)pipManager {
    objc_setAssociatedObject(self, "pipManager", pipManager, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (VeLivePlayerPictureInPictureManager *)pipManager {
    VeLivePlayerPictureInPictureManager *manager = objc_getAssociatedObject(self, "pipManager");
    if (!manager) {
        manager = [[VeLivePlayerPictureInPictureManager alloc] initWithPlayer:self];
        self.pipManager = manager;
        [manager setupPictureInPicture];
        manager.requiresLinearPlayback = YES;
        manager.canStartPictureInPictureAutomaticallyFromInline = YES;
    }
    return manager;
}

- (void)setEnablePictureInPicture:(BOOL)enablePictureInPicture {
    if (@available(iOS 15.0, *)) {
        self.pipManager.enablePictureInPicture = enablePictureInPicture;
        if (enablePictureInPicture) {
            if ([self respondsToSelector:@selector(setShouldReportVideoFrame:)]) {
                self.shouldReportVideoFrame = YES;
            }
        }
    }
}

- (BOOL)enablePictureInPicture {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.enablePictureInPicture;
    }
    return NO;
}

- (void)setPictureInPictureRequiresLinearPlayback:(BOOL)pictureInPictureRequiresLinearPlayback {
    if (@available(iOS 15.0, *)) {
        self.pipManager.requiresLinearPlayback = pictureInPictureRequiresLinearPlayback;
    }
}

- (BOOL)pictureInPictureRequiresLinearPlayback {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.requiresLinearPlayback;
    }
    return NO;
}

- (void)setCanStartPictureInPictureAutomaticallyFromInline:(BOOL)canStartPictureInPictureAutomaticallyFromInline {
    if (@available(iOS 15.0, *)) {
        self.pipManager.canStartPictureInPictureAutomaticallyFromInline = canStartPictureInPictureAutomaticallyFromInline;
    }
}

- (BOOL)canStartPictureInPictureAutomaticallyFromInline {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.canStartPictureInPictureAutomaticallyFromInline;
    }
    return NO;
}

+ (BOOL) isPictureInPictureSupported {
    if (@available(iOS 15.0, *)) {
        return [VeLivePlayerPictureInPictureManager isPictureInPictureSupported];
    }
    return NO;
    
}
+ (BOOL) isPictureInPictureStarted {
    if (@available(iOS 15.0, *)) {
        return [VeLivePlayerPictureInPictureManager isPictureInPictureStarted];
    }
    return NO;
}

- (void) startPictureInPicture {
    if (@available(iOS 15.0, *)) {
        if (self.pipManager && self.enablePictureInPicture) {
            [self.pipManager startPictureInPicture];
        }
    }
}
- (void) stopPictureInPicture {
    if (@available(iOS 15.0, *)) {
        if (self.pipManager) {
            [self.pipManager stopPictureInPicture];
        }
    }
}

- (AVPictureInPictureController *)avPictureInPictureController {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.pictureInPictureController;
    }
    return nil;
}

- (void)setPictureInPictureDelegate:(id<AVPictureInPictureControllerDelegate>)pictureInPictureDelegate {
    if (@available(iOS 15.0, *)) {
        self.pipManager.pictureInPictureDelegate = pictureInPictureDelegate;
    }
}

- (id<AVPictureInPictureControllerDelegate>)pictureInPictureDelegate {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.pictureInPictureDelegate;
    }
    return nil;
}

- (void)setPictureInPicturePlaybackDelegate:(id<AVPictureInPictureSampleBufferPlaybackDelegate>)pictureInPicturePlaybackDelegate {
    if (@available(iOS 15.0, *)) {
        self.pipManager.pictureInPicturePlaybackDelegate = pictureInPicturePlaybackDelegate;
    }
}

- (id<AVPictureInPictureSampleBufferPlaybackDelegate>)pictureInPicturePlaybackDelegate {
    if (@available(iOS 15.0, *)) {
        return self.pipManager.pictureInPicturePlaybackDelegate;
    }
    return nil;
}

- (void)setPictureInPictureRenderFillMode:(VeLivePlayerFillMode)fillMode {
    if (@available(iOS 15.0, *)) {
        VePictureInPictureRenderMode pipMode = VePictureInPictureRenderModeAspectFill;
        switch (fillMode) {
            case VeLivePlayerFillModeFullFill:
                pipMode = VePictureInPictureRenderModeFullFill;
                break;
            case VeLivePlayerFillModeAspectFit:
                pipMode = VePictureInPictureRenderModeAspectFit;
                break;
            case VeLivePlayerFillModeAspectFill:
                pipMode = VePictureInPictureRenderModeAspectFill;
                break;
            default:
                break;
        }
        [self.pipManager setRenderFillMode:pipMode];
    }
}

- (void)setVideoSizeToPictureInPicture:(CGSize)size {
    if (@available(iOS 15.0, *)) {
        [self.pipManager setVideoWidth:size.width height:size.height];
    }
}

- (void)sendRenderViewFrame:(CVPixelBufferRef)pixelBuffer {
    if (@available(iOS 15.0, *)) {
        if (self.enablePictureInPicture) {
            [self.pipManager sendRenderViewFrame:pixelBuffer];
        }
    }
}

- (void)invalidatePictureInPicturePlaybackState {
    if (@available(iOS 15.0, *)) {
        VePipAsyncRunOnMainThread(^{
            [self.pipManager invalidatePlaybackState];
        });
    }
}

@end
