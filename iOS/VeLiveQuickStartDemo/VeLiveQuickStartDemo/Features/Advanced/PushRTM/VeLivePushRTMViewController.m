/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePushRTMViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
摄像头超低延迟推流
 
 本文件展示如何集成摄像头超低延迟推流
 1、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 2、设置预览视图 API: [self.livePusher setRenderView:self.view];
 3、打开麦克风采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
 4、打开摄像头采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
 5、开始推流 API：[self.livePusher startPushWithUrls:@[@"http://push.example.com/rtm.sdp", @"rtmp://push.example.com/rtmp"]];
 */
#import "VeLivePushRTMViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePushRTMViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *rtmUrlTextField;
@property (weak, nonatomic) IBOutlet UITextField *rtmpUrlTextField;
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (nonatomic, strong) VeLivePusher *livePusher;
@end

@implementation VeLivePushRTMViewController

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
    if (self.rtmUrlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config RTM Url");
        return;
    }
    if (self.rtmpUrlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config RTMP Url");
        return;
    }
    if (!sender.isSelected) {
        //  开始推流，第一个推流地址填写RTM推流地址，后面填写降级推流地址  
        [self.livePusher startPushWithUrls:@[self.rtmUrlTextField.text, self.rtmpUrlTextField.text]];
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

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Push_RTM", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.rtmpUrlTextField.text = LIVE_PUSH_URL;
    self.rtmUrlTextField.text = LIVE_RTM_PUSH_URL;
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
