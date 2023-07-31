/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePush265CodecViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
H265编码推流
 
 本文件展示如何集成H265编码能力
 1、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 2、初始化编码配置 API: VeLiveVideoEncoderConfiguration *videoEncodeCfg = [[VeLiveVideoEncoderConfiguration alloc] initWithResolution:(VeLiveVideoResolution720P)]
 3、配置编码类型 API: videoEncodeCfg.codec = VeLiveVideoCodecByteVC1;
 4、设置编码配置 API: [self.livePusher setVideoEncoderConfiguration:videoEncodeCfg];
 5、设置预览视图 API: [self.livePusher setRenderView:self.view];
 6、打开麦克风采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
 7、打开摄像头采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
 8、开始推流 API：[self.livePusher startPush:@"rtmp://push.example.com/rtmp"];
 参考文档：https://www.volcengine.com/docs/6469/155317
 */
#import "VeLivePush265CodecViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePush265CodecViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;

@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (nonatomic, strong) VeLivePusher *livePusher;

@end

@implementation VeLivePush265CodecViewController

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
    //  更多配置参考：https://www.volcengine.com/docs/6469/155321#velivepusherconfiguration  
    self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
    
    //  视频编码配置  
    VeLiveVideoEncoderConfiguration *videoEncodeCfg = [[VeLiveVideoEncoderConfiguration alloc] initWithResolution:(VeLiveVideoResolution720P)];
    
    //  编码类型  
    videoEncodeCfg.codec = VeLiveVideoCodecByteVC1;
    
    //  配置编码  
    [self.livePusher setVideoEncoderConfiguration:videoEncodeCfg];
    
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
    self.title = NSLocalizedString(@"Push_Auto_Bitrate", nil);
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
