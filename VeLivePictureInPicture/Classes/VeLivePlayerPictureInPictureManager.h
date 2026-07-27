#import <Foundation/Foundation.h>
#import <AVKit/AVKit.h>
#import "VeLivePictureInPictureContentSourceController.h"

@class TVLManager;

API_AVAILABLE(ios(15.0))
@interface VeLivePlayerPictureInPictureManager : NSObject
@property (nonatomic, assign) BOOL enablePictureInPicture;
@property (nonatomic, assign) BOOL canStartPictureInPictureAutomaticallyFromInline;
@property (nonatomic, assign) BOOL requiresLinearPlayback;
@property (nonatomic, strong, readonly, nullable) AVPictureInPictureController *pictureInPictureController;
@property (nonatomic, weak, nullable) id<AVPictureInPictureControllerDelegate> pictureInPictureDelegate;
@property (nonatomic, weak, nullable) id<AVPictureInPictureSampleBufferPlaybackDelegate> pictureInPicturePlaybackDelegate;

- (instancetype)initWithPlayer:(TVLManager *)player;

+ (BOOL) isPictureInPictureSupported;
+ (BOOL) isPictureInPictureStarted;

- (BOOL) setupPictureInPicture;
- (void) startPictureInPicture;
- (void) stopPictureInPicture;
- (void) setRenderFillMode:(VePictureInPictureRenderMode)mode;
- (void) setVideoWidth:(int)width height:(int)height;
- (void) sendRenderViewFrame:(CVPixelBufferRef)pixelBuffer;
- (void) invalidatePlaybackState;

@end



@interface VeLiveDelegateForwardObject : NSObject
@property (nonatomic, weak) id veOriginDelegate;
@end
