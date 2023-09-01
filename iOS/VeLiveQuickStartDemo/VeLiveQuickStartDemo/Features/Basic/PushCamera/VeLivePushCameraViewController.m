/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePushCameraViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
摄像头推流
 本文件展示如何集成摄像头推流功能
 1、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 2、设置预览视图 API: [self.livePusher setRenderView:self.view];
 3、打开麦克风采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
 4、打开摄像头采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
 5、开始推流 API：[self.livePusher startPush:@"rtmp://push.example.com/rtmp"];
 */
#import "VeLivePushCameraViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePushCameraViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *cameraControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *muteControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *captureMirrorBtn;
@property (weak, nonatomic) IBOutlet UIButton *previewMirrorBtn;
@property (weak, nonatomic) IBOutlet UIButton *pushMirrorBtn;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (nonatomic, strong) VeLivePusher *livePusher;
@end

@implementation VeLivePushCameraViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self addApplicationNotifaction];
    [self setupLivePusher];
}

- (void)dealloc {
    //  销毁推流器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePusher destroy];
}

- (void)applicationWillResignActive:(NSNotification *)noti {
    //  进入后台，持续推送最后一帧  
    //  也可以持续推送一张图片，或者推送黑帧，具体详见API  
    [self.livePusher switchVideoCapture:(VeLiveVideoCaptureLastFrame)];
}

- (void)applicationDidBecomeActive:(NSNotification *)noti {
    //  进入前台，切换至摄像头采集  
    [self.livePusher switchVideoCapture:(VeLiveVideoCaptureFrontCamera)];
}

- (void)setupLivePusher {
    //  推流配置  
    VeLivePusherConfiguration *config = [[VeLivePusherConfiguration alloc] init];
    //  失败重连次数  
    config.reconnectCount = 10;
    //  重连的时间间隔  
    config.reconnectIntervalSeconds = 5;
    
    //  创建推流器  
    self.livePusher = [[VeLivePusher alloc] initWithConfig:config];
    
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
        NSLog(@"VeLiveQuickStartDemo: Please Config Url");
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

- (IBAction)cameraControl:(UIButton *)sender {
    if (sender.isSelected) {
        //  切换至前置摄像头  
        [self.livePusher switchVideoCapture:(VeLiveVideoCaptureFrontCamera)];
    } else {
        //  切换至后置摄像头  
        [self.livePusher switchVideoCapture:(VeLiveVideoCaptureBackCamera)];
    }
    sender.selected = !sender.isSelected;
}

- (IBAction)muteControl:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    //  静音/取消静音  
    [self.livePusher setMute:sender.isSelected];
}

- (IBAction)captureMirror:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    //  开启/关闭采集镜像  
    [self.livePusher setVideoMirror:(VeLiveVideoMirrorCapture) enable:sender.isSelected];
}

- (IBAction)previewMirror:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    //  开启/关闭预览镜像  
    [self.livePusher setVideoMirror:(VeLiveVideoMirrorPreview) enable:sender.isSelected];
}

- (IBAction)pushMirror:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    //  开启/关闭推流镜像  
    [self.livePusher setVideoMirror:(VeLiveVideoMirrorPushStream) enable:sender.isSelected];
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
    self.title = NSLocalizedString(@"Camera_Push", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlTextField.text = LIVE_PUSH_URL;
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
    [self.cameraControlBtn setTitle:NSLocalizedString(@"Push_Camera_Front", nil) forState:(UIControlStateNormal)];
    [self.cameraControlBtn setTitle:NSLocalizedString(@"Push_Camera_Back", nil) forState:((UIControlStateSelected))];
    [self.muteControlBtn setTitle:NSLocalizedString(@"Push_Mute", nil) forState:((UIControlStateNormal))];
    [self.muteControlBtn setTitle:NSLocalizedString(@"Push_UnMute", nil) forState:((UIControlStateSelected))];
    [self.captureMirrorBtn setTitle:NSLocalizedString(@"Push_Capture_Mirror", nil) forState:((UIControlStateNormal))];
    [self.previewMirrorBtn setTitle:NSLocalizedString(@"Push_Preview_Mirror", nil) forState:((UIControlStateNormal))];
    [self.pushMirrorBtn setTitle:NSLocalizedString(@"Push_Push_Mirror", nil) forState:((UIControlStateNormal))];
}

- (void)addApplicationNotifaction {
    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(applicationWillResignActive:)
                                               name:UIApplicationWillResignActiveNotification
                                             object:nil];
    
    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(applicationDidBecomeActive:)
                                               name:UIApplicationDidBecomeActiveNotification
                                             object:nil];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
