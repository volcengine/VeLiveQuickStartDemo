/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePushMixStreamViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2024/11/21.
//

/*
 推流同时混入音视频流
 
 本文件展示如何集成摄像头推流和音视频混流功能
 1、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 2、设置预览视图 API: [self.livePusher setRenderView:self.view];
 3、打开麦克风采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
 4、打开摄像头采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
 5、开始推流 API：[self.livePusher startPush:@"rtmp://push.example.com/rtmp"];
 6、设置音频混流 API:
    self.videoMixID = [self.livePusher.getMixerManager addVideoStream];
    [self.livePusher.getMixerManager sendCustomAudioFrame:audioFrame streamId:self.audioMixID];
 7、设置视频混流 API:
    self.videoMixID = [self.livePusher.getMixerManager addVideoStream];
    [self.livePusher.getMixerManager updateStreamMixDescription:dec];
    [self.livePusher.getMixerManager sendCustomVideoFrame:videoFrame streamId:self.videoMixID];
 */

#import "VeLivePushMixStreamViewController.h"
#import "VeLiveSDKHelper.h"
#import "VeLiveFileReader.h"
@interface VeLivePushMixStreamViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *mixAudioControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *mixVideoControlBtn;
@property (nonatomic, strong) VeLivePusher *livePusher;
@property (nonatomic, strong) VeLiveFileReader *auidoFileReader;
@property (nonatomic, strong) VeLiveFileReader *videoFileReader;
@property (nonatomic) int audioMixID;
@property (nonatomic) int videoMixID;
@end

@implementation VeLivePushMixStreamViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupLivePusher];
}

- (void)dealloc {
    //  销毁推流器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePusher destroy];
}

- (void)setupLivePusher {
    
    //  创建推流器  
    self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
    
    //  配置预览视图  
    [self.livePusher setRenderView:self.view];
    
    //  设置推流器回调  
    [self.livePusher setObserver:self];
    
    //  设置周期性信息回调  
    [self.livePusher setStatisticsObserver:self interval:3];
    
    //  请求摄像头和麦克风权限  
    [VeLiveDeviceCapture requestCameraAndMicroAuthorization:^(BOOL cameraGranted, BOOL microGranted) {
        if (cameraGranted) {
            //  开始视频采集  
            [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
        } else {
            NSLog(@"VeLiveQuickStartDemo: Please Allow Camera Auth");
        }
        if (microGranted) {
            //  开始音频采集  
            [self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
        } else {
            NSLog(@"VeLiveQuickStartDemo: Please Allow Microphone Auth");
        }
    }];
}

- (IBAction)pushControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        self.infoLabel.text = NSLocalizedString(@"config_stream_name_tip", nil);
        return;
    }
    if (!sender.isSelected) {
        self.infoLabel.text = NSLocalizedString(@"Generate_Push_Url_Tip", nil);
        self.view.userInteractionEnabled = NO;
        [VeLiveURLGenerator genPushURLForApp:LIVE_APP_NAME
                                  streamName:self.urlTextField.text
                                  completion:^(VeLiveURLRootModel<VeLivePushURLModel *> * _Nullable model, NSError * _Nullable error) {
            self.infoLabel.text = error.localizedDescription;
            self.view.userInteractionEnabled = YES;
            if (error != nil) {
                return;
            }
            //  开始推流，推流地址支持： rtmp 协议，http 协议（RTM）  
            [self.livePusher startPush:[model.result getRtmpPushUrl]];
            sender.selected = !sender.isSelected;
        }];
    } else {
        //  停止推流  
        [self.livePusher stopPush];
        sender.selected = !sender.isSelected;
    }
}

- (IBAction)audioStreamControl:(UIButton *)sender {
    BOOL isSelected = sender.isSelected;
    if (!isSelected) {
        //  添加音频流, 同时本地渲染  
        self.audioMixID = [self.livePusher.getMixerManager addAudioStream:VeLiveAudioMixPlayAndPush];
        VELAudioFileConfig *config = [[VELAudioFileConfig alloc] init];
        config.fileType = VELAudioFileType_PCM;
        config.channels = 2;
        config.name = @"audio_44100_16bit2ch.pcm";
        config.path = [NSBundle.mainBundle pathForResource:@"audio_44100_16bit_2ch.pcm" ofType:nil];
        self.auidoFileReader = [VeLiveFileReader readerWithConfig:config];
        self.auidoFileReader.repeat = YES;
        [self.auidoFileReader startWithDataCallBack:^(NSData * _Nullable data, CMTime pts) {
            VeLiveAudioFrame *audioFrame = [[VeLiveAudioFrame alloc] init];
            audioFrame.bufferType = VeLiveAudioBufferTypeNSData;
            audioFrame.data = data;
            audioFrame.sampleRate = config.sampleRate;
            audioFrame.channels = config.channels;
            audioFrame.pts = pts;
            //  混入音频流  
            [self.livePusher.getMixerManager sendCustomAudioFrame:audioFrame streamId:self.audioMixID];
        } completion:^(NSError * _Nullable error, BOOL isEnd) {
            vel_sync_main_queue(^{
                [self.auidoFileReader stop];
                [self.livePusher.getMixerManager removeAudioStream:self.audioMixID];
                sender.selected = NO;
                
            });
        }];
        sender.selected = YES;
    } else {
        [self.auidoFileReader stop];
        [self.livePusher.getMixerManager removeAudioStream:self.audioMixID];
        sender.selected = NO;
    }
}

- (IBAction)videoStreamControl:(UIButton *)sender {
    BOOL isSelected = sender.isSelected;
    if (!isSelected) {
        //  添加视频流  
        self.videoMixID = [self.livePusher.getMixerManager addVideoStream];
        
        VeLiveStreamMixDescription *dec = [[VeLiveStreamMixDescription alloc] init];
        VeLiveMixVideoLayout *layout = [[VeLiveMixVideoLayout alloc] init];
        layout.streamId = self.videoMixID;
        layout.x = 0.5;
        layout.y = 0.5;
        layout.zOrder = 10;
        layout.width = 0.3;
        layout.height = 0.3;
        layout.renderMode = VeLivePusherRenderModeHidden;
        dec.mixVideoStreams = @[layout];
        //  更新视频流配置  
        [self.livePusher.getMixerManager updateStreamMixDescription:dec];
        
        VELVideoFileConfig *config = [[VELVideoFileConfig alloc] init];
        config.fileType = VELVideoFileType_YUV;
        config.name = @"video_320x180_25fps_yuv420.yuv";
        config.path = [NSBundle.mainBundle pathForResource:@"video_320x180_25fps_yuv420.yuv" ofType:nil];
        config.fps = 25;
        config.width = 320;
        config.height = 180;
        self.videoFileReader = [VeLiveFileReader readerWithConfig:config];
        self.videoFileReader.repeat = YES;
        [self.videoFileReader startWithDataCallBack:^(NSData * _Nullable data, CMTime pts) {
            VeLiveVideoFrame *videoFrame = [[VeLiveVideoFrame alloc] init];
            videoFrame.pts = pts;
            videoFrame.width = config.width;
            videoFrame.height = config.height;
            videoFrame.bufferType = VeLiveVideoBufferTypeNSData;
            videoFrame.data = data;
            videoFrame.pixelFormat = VeLivePixelFormatI420;
            //  混入视频流  
            [self.livePusher.getMixerManager sendCustomVideoFrame:videoFrame streamId:self.videoMixID];
        } completion:^(NSError * _Nullable error, BOOL isEnd) {
            vel_sync_main_queue(^{
                [self.videoFileReader stop];
                [self.livePusher.getMixerManager removeVideoStream:self.videoMixID];
                sender.selected = NO;
            });
        }];
        sender.selected = YES;
    } else {
        [self.videoFileReader stop];
        [self.livePusher.getMixerManager removeVideoStream:self.videoMixID];
        sender.selected = NO;
    }
}

// MARK: - VeLivePusherObserver
- (void)onError:(int)code subcode:(int)subcode message:(nullable NSString *)msg {
    NSLog(@"VeLiveQuickStartDemo: Error %d-%d-%@", code, subcode, msg?:@"");
}

- (void)onStatusChange:(VeLivePushStatus)status {
    NSLog(@"VeLiveQuickStartDemo: Status %@", @(status));
}

// MARK: - VeLivePusherStatisticsObserver
- (void)onStatistics:(VeLivePusherStatistics *)statistics {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.infoLabel.attributedText = [VeLiveSDKHelper getPushInfoString:statistics];
    });
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Push_Mix_Stream", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlLabel.text = NSLocalizedString(@"Push_Url_Tip", nil);
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
    [self.mixAudioControlBtn setTitle:NSLocalizedString(@"Push_Mix_Stream_Audio", nil) forState:(UIControlStateNormal)];
    [self.mixAudioControlBtn setTitle:NSLocalizedString(@"Push_Mix_Stream_Audio", nil) forState:(UIControlStateSelected)];
    [self.mixVideoControlBtn setTitle:NSLocalizedString(@"Push_Mix_Stream_Video", nil) forState:(UIControlStateNormal)];
    [self.mixVideoControlBtn setTitle:NSLocalizedString(@"Push_Mix_Stream_Video", nil) forState:(UIControlStateSelected)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
