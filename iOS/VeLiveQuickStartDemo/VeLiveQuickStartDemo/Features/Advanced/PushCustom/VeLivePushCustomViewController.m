/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePushCustomViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
自定义采集推流
 
 本文件展示如何集成自定义音视频采集推流
 1、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 2、设置预览视图 API: [self.livePusher setRenderView:self.view];
 3、打开麦克风外部采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureExternal)];
 4、打开摄像头外部采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureExternal)];
 5、送入外部音频帧数据 API: [self.livePusher pushExternalAudioFrame:[[VeLiveAudioFrame alloc] init]]
 6、送入外部视频帧数据 API: [self.livePusher pushExternalAudioFrame:[[pushExternalVideoFrame alloc] init]]
 7、开始推流 API：[self.livePusher startPush:@"rtmp://push.example.com/rtmp"];
 参考文档：https://www.volcengine.com/docs/6469/155317
 */
#import "VeLivePushCustomViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePushCustomViewController () <VeLiveDeviceCaptureDelegate, VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (nonatomic, strong) VeLiveDeviceCapture *deviceCapture;
@property (nonatomic, strong) VeLivePusher *livePusher;
@end

@implementation VeLivePushCustomViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupCustomCapture];
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
    
    //  开启外部视频采集  
    [self.livePusher startVideoCapture:(VeLiveVideoCaptureExternal)];
    
    //  开启外部音频采集  
    [self.livePusher startAudioCapture:(VeLiveAudioCaptureExternal)];
    
}

- (IBAction)pushControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config Push Url");
        return;
    }
    
    if (!sender.isSelected) {
        //  开始推流，推流地址支持： rtmp 协议，http 协议（RTM）  
        [self.livePusher startPush:self.urlTextField.text];
    } else {
        //  停止推流  
        [self.livePusher stopPush];
    }
    
    sender.selected = !sender.isSelected;
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

// MARK: - VeLiveDeviceCaptureDelegate
- (void)capture:(VeLiveDeviceCapture *)capture didOutputVideoSampleBuffer:(CMSampleBufferRef)sampleBuffer {
    VeLiveVideoFrame *videoFrame = [[VeLiveVideoFrame alloc] init];
    videoFrame.bufferType = VeLiveVideoBufferTypeSampleBuffer;
    videoFrame.sampleBuffer = sampleBuffer;
    videoFrame.rotation = VeLiveVideoRotation0;
    [self.livePusher pushExternalVideoFrame:videoFrame];
}

- (void)capture:(VeLiveDeviceCapture *)capture didOutputAudioSampleBuffer:(CMSampleBufferRef)sampleBuffer {
    VeLiveAudioFrame *frame = [[VeLiveAudioFrame alloc] init];
    frame.bufferType = VeLiveAudioBufferTypeSampleBuffer;
    frame.sampleBuffer = sampleBuffer;
    frame.pts = CMTimeMakeWithSeconds(CACurrentMediaTime(), 1000000000);
    [self.livePusher pushExternalAudioFrame:frame];
}

- (void)setupCustomCapture {
    self.deviceCapture = [[VeLiveDeviceCapture alloc] init];
    self.deviceCapture.delegate = self;
    [self.deviceCapture startCapture];
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Push_Custom", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlTextField.text = LIVE_PUSH_URL;
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
