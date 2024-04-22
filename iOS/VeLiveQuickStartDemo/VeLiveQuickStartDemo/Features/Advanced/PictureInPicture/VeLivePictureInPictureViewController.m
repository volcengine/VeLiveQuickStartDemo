/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePictureInPictureViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/8/8.
//

#import "VeLivePictureInPictureViewController.h"
#import "VeLiveSDKHelper.h"
#import <UIKit/UIKit.h>
#import <AVKit/AVKit.h>
@interface VeLivePictureInPictureViewController ()<
VeLivePlayerObserver,
AVPictureInPictureControllerDelegate,
AVPictureInPictureSampleBufferPlaybackDelegate>
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (nonatomic, strong) TVLManager *livePlayer;
@property (weak, nonatomic) IBOutlet UIButton *playControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *fillModeControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *pipontrolBtn;
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@property (nonatomic, strong) AVPictureInPictureController *pipController;
@property (nonatomic, strong) AVSampleBufferDisplayLayer *sampleBufferDisplayLayer;
@end

@implementation VeLivePictureInPictureViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupLivePlayer];
}

- (void)dealloc {
    //  销毁直播播放器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePlayer destroy];
    //  销毁画中画控制器  
    self.pipController = nil;
    [self.sampleBufferDisplayLayer removeFromSuperlayer];
    [self.livePlayer stop];
    self.livePlayer = nil;
}

- (void)setupLivePlayer {
    //  创建直播播放器  
    self.livePlayer = [[TVLManager alloc] initWithOwnPlayer:YES];
    
    //  设置播放器回调  
    [self.livePlayer setObserver:self];
    
    //  配置播放器  
    VeLivePlayerConfiguration *cfg = [[VeLivePlayerConfiguration alloc] init];
    
    //  是否开启周期性信息回调  
    cfg.enableStatisticsCallback = YES;
    
    //  周期性信息回调间隔  
    cfg.statisticsCallbackInterval = 1;
    
    //  是否开启内部DNS解析  
    cfg.enableLiveDNS = YES;
    
    //  配置拉流播放器  
    [self.livePlayer setConfig:cfg];
    
    //  配置播放器视图  
    self.livePlayer.playerView.frame = self.view.bounds;
    [self.view insertSubview:self.livePlayer.playerView atIndex:0];
    
    //  设置渲染填充模式  
    [self.livePlayer setRenderFillMode:(VeLivePlayerFillModeAspectFill)];
    
    
    //  准备画中画  
    [self setupPictureInPicture];
}

- (void)setupPictureInPicture {
    if (@available(iOS 15.0, *)) {
        if ([AVPictureInPictureController isPictureInPictureSupported]) {
            [self setupSampleBufferDisplayLayer];
            [self.view.layer insertSublayer:self.sampleBufferDisplayLayer atIndex:0];
            AVPictureInPictureControllerContentSource *contentSource = [[AVPictureInPictureControllerContentSource alloc]
                                                                        initWithSampleBufferDisplayLayer:self.sampleBufferDisplayLayer
                                                                        playbackDelegate:self];
            self.pipController = [[AVPictureInPictureController alloc] initWithContentSource:contentSource];
            self.pipController.delegate = self;
            self.pipController.canStartPictureInPictureAutomaticallyFromInline = YES;
        } else {
            NSLog(@"pip only support iOS 15.0");
        }
    }
}

- (IBAction)playControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        self.infoLabel.text = NSLocalizedString(@"config_stream_name_tip", nil);
        return;
    }
    if (sender.isSelected) {
        //  停止播放  
        [self.livePlayer stop];
        sender.selected = !sender.isSelected;
    } else {
        self.infoLabel.text = NSLocalizedString(@"Generate_Pull_Url_Tip", nil);
        self.view.userInteractionEnabled = NO;
        [VeLiveURLGenerator genPullURLForApp:LIVE_APP_NAME streamName:self.urlTextField.text completion:^(VeLiveURLRootModel<VeLivePullURLModel *> * _Nullable model, NSError * _Nullable error) {
            self.infoLabel.text = error.localizedDescription;
            self.view.userInteractionEnabled = YES;
            if (error != nil) {
                return;
            }
            //  设置播放地址，支持 rtmp、http、https 协议，flv、m3u8 格式的地址  
            [self.livePlayer setPlayUrl:[model.result getUrlWithProtocol:@"flv"]];
            // 开始播放 
            [self.livePlayer play];
            // 开启监听视频帧回调 
            [self.livePlayer enableVideoFrameObserver:YES
                                          pixelFormat:(VeLivePlayerPixelFormatNV12)
                                           bufferType:(VeLivePlayerVideoBufferTypePixelBuffer)];
            sender.selected = !sender.isSelected;
        }];
    }
}

- (IBAction)pictureInPictureControl:(UIButton *)sender {
    if (@available(iOS 15.0, *)) {
        if (sender.isSelected) {
            if (self.pipController.isPictureInPictureActive) {
                [self.pipController stopPictureInPicture];
            }
        } else {
            if (self.pipController.isPictureInPicturePossible) {
                [self.pipController startPictureInPicture];
            }
        }
        sender.selected = !sender.isSelected;
    }
}

- (void)enqueuePixelBuffer:(CVPixelBufferRef)pixelBuffer {
    if (!pixelBuffer) {
        return;
    }
    CMSampleTimingInfo timing = {kCMTimeInvalid, kCMTimeInvalid, kCMTimeInvalid};
    CMVideoFormatDescriptionRef videoInfo = NULL;
    OSStatus result = CMVideoFormatDescriptionCreateForImageBuffer(NULL, pixelBuffer, &videoInfo);
    NSParameterAssert(result == 0 && videoInfo != NULL);
    
    CMSampleBufferRef sampleBuffer = NULL;
    result = CMSampleBufferCreateForImageBuffer(kCFAllocatorDefault,pixelBuffer, true, NULL, NULL, videoInfo, &timing, &sampleBuffer);
    NSParameterAssert(result == 0 && sampleBuffer != NULL);
    CFRelease(videoInfo);
    CFArrayRef attachments = CMSampleBufferGetSampleAttachmentsArray(sampleBuffer, YES);
    CFMutableDictionaryRef dict = (CFMutableDictionaryRef)CFArrayGetValueAtIndex(attachments, 0);
    CFDictionarySetValue(dict, kCMSampleAttachmentKey_DisplayImmediately, kCFBooleanTrue);
    [self enqueueSampleBuffer:sampleBuffer];
    CFRelease(sampleBuffer);
}

- (void)enqueueSampleBuffer:(CMSampleBufferRef)sampleBuffer {
    if (sampleBuffer) {
        CFRetain(sampleBuffer);
        [self.sampleBufferDisplayLayer enqueueSampleBuffer:sampleBuffer];
        CFRelease(sampleBuffer);
        if (self.sampleBufferDisplayLayer.status == AVQueuedSampleBufferRenderingStatusFailed) {
            [self.sampleBufferDisplayLayer flush];
            if (-11847 == self.sampleBufferDisplayLayer.error.code) {
                [self rebuildSampleBufferDisplayLayer];
            }
        }
    }
}

- (void)rebuildSampleBufferDisplayLayer {
    @synchronized(self) {
        [self teardownSampleBufferDisplayLayer];
        [self setupSampleBufferDisplayLayer];
    }
}
  
- (void)teardownSampleBufferDisplayLayer {
    if (self.sampleBufferDisplayLayer) {
        [self.sampleBufferDisplayLayer stopRequestingMediaData];
        [self.sampleBufferDisplayLayer removeFromSuperlayer];
        self.sampleBufferDisplayLayer = nil;
    }
}
  
- (void)setupSampleBufferDisplayLayer {
    if (!self.sampleBufferDisplayLayer) {
        self.sampleBufferDisplayLayer = [[AVSampleBufferDisplayLayer alloc] init];
        self.sampleBufferDisplayLayer.frame = UIApplication.sharedApplication.keyWindow.bounds;
        self.sampleBufferDisplayLayer.position = CGPointMake(CGRectGetMidX(self.sampleBufferDisplayLayer.bounds),
                                                             CGRectGetMidY(self.sampleBufferDisplayLayer.bounds));
        self.sampleBufferDisplayLayer.videoGravity = AVLayerVideoGravityResizeAspect;
        self.sampleBufferDisplayLayer.opaque = YES;
        [self.view.layer addSublayer:self.sampleBufferDisplayLayer];
    } else {
        [CATransaction begin];
        [CATransaction setDisableActions:YES];
        self.sampleBufferDisplayLayer.frame = self.view.bounds;
        self.sampleBufferDisplayLayer.position = CGPointMake(CGRectGetMidX(self.view.bounds), CGRectGetMidY(self.view.bounds));
        [CATransaction commit];
    }
}


// MARK: - VeLiveVideoFrameListener
- (void)onRenderVideoFrame:(TVLManager *)player videoFrame:(VeLivePlayerVideoFrame *)videoFrame {
    if (videoFrame.pixelBuffer) {
        [self enqueuePixelBuffer:videoFrame.pixelBuffer];
    }
}

// MARK: - VeLivePictureInPictureDelegate
- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"pictureInPictureControllerWillStartPictureInPicture");
}

- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    [self.pipontrolBtn setSelected:YES];
    self.livePlayer.playerView.hidden = YES;
    NSLog(@"pictureInPictureControllerDidStartPictureInPicture");
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
failedToStartPictureInPictureWithError:(NSError *)error {
    NSLog(@"failedToStartPictureInPictureWithError");
    self.livePlayer.playerView.hidden = NO;
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL))completionHandler {
    NSLog(@"restoreUserInterfaceForPictureInPictureStopWithCompletionHandler");
    completionHandler(true);
    self.livePlayer.playerView.hidden = NO;
}

- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"pictureInPictureControllerWillStopPictureInPicture");
}

- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    [self.pipontrolBtn setSelected:NO];
    NSLog(@"pictureInPictureControllerDidStopPictureInPicture");
    self.livePlayer.playerView.hidden = NO;
}


#pragma mark - AVPictureInPictureSampleBufferPlaybackDelegate
- (BOOL)pictureInPictureControllerIsPlaybackPaused:(nonnull AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"pictureInPictureControllerIsPlaybackPaused");
    return NO;
}

- (CMTimeRange)pictureInPictureControllerTimeRangeForPlayback:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"pictureInPictureControllerTimeRangeForPlayback");
    return  CMTimeRangeMake(kCMTimeZero, kCMTimePositiveInfinity); // for live streaming
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
         didTransitionToRenderSize:(CMVideoDimensions)newRenderSize {
    NSLog(@"didTransitionToRenderSize");
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController setPlaying:(BOOL)playing {
    NSLog(@"setPlaying");
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
                    skipByInterval:(CMTime)skipInterval
                 completionHandler:(void (^)(void))completionHandler {
    NSLog(@"skipByInterval");
}

// MARK: - VeLivePlayerObserver
- (void)onStatistics:(TVLManager *)player statistics:(VeLivePlayerStatistics *)statistics {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.infoLabel.attributedText = [VeLiveSDKHelper getPlaybackInfoString:statistics];
    });
}

- (void)onError:(TVLManager *)player error:(VeLivePlayerError *)error {
    NSLog(@"VeLiveQuickStartDemo: Error %ld, %@", error.code, error.errorMsg);
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Pull_Stream", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlLabel.text = NSLocalizedString(@"Pull_Stream_Url_Tip", nil);
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Start_Play", nil) forState:(UIControlStateNormal)];
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Stop_Play", nil) forState:(UIControlStateSelected)];
    self.playControlBtn.selected = NO;
    
    [self.pipontrolBtn setTitle:NSLocalizedString(@"Start_Picture_In_Picture", nil) forState:((UIControlStateNormal))];
    [self.pipontrolBtn setTitle:NSLocalizedString(@"Stop_Picture_In_Picture", nil) forState:((UIControlStateSelected))];
    
    [self.fillModeControlBtn setTitle:NSLocalizedString(@"Pull_Fill_Mode", nil) forState:((UIControlStateNormal))];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
