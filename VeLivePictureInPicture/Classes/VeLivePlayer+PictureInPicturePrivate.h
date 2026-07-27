
/**
 Copyright (c) 2023 The VeLivePlayer project authors. All Rights Reserved.
 @brief VeLivePlayer
*/

#import "VeLivePlayer.h"
#import <AVKit/AVKit.h>


@interface TVLManager (PictureInPicturePrivate)
@property (nonatomic, assign) BOOL enablePictureInPicture;
- (void)setPictureInPictureRenderFillMode:(VeLivePlayerFillMode)fillMode;

- (void)setVideoSizeToPictureInPicture:(CGSize)size;

- (void)sendRenderViewFrame:(CVPixelBufferRef)videoFrame;

- (void)invalidatePictureInPicturePlaybackState;

@end
