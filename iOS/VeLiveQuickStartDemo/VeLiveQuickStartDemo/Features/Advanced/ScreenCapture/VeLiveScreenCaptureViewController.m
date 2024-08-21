/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePushBeautyViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import "VeLiveScreenCaptureViewController.h"
#import "VeLiveSDKHelper.h"

@interface VeLiveScreenCaptureViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver, VeLiveScreenCaptureStatusObserver>
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (nonatomic, strong) VeLivePusher *livePusher;
@property (nonatomic, copy) NSString *streamUrl;
@property (weak, nonatomic) IBOutlet UILabel *timeLabel;
@property (nonatomic, strong) NSTimer *timer;
@property (nonatomic, strong) NSDateFormatter *dateFormatter;
@property (nonatomic, strong) RPSystemBroadcastPickerView *broadcastPickerView API_AVAILABLE(ios(12.0));
@end

@implementation VeLiveScreenCaptureViewController


- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupLivePusher];
    [self setupBroadcastPicker];
}

- (void)dealloc {
    [self stopTimer];
    //  销毁推流器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePusher destroy];
}

- (void)setupBroadcastPicker {
    if (@available(iOS 12.0, *)) {
        self.broadcastPickerView = [[RPSystemBroadcastPickerView alloc] initWithFrame:CGRectMake(0, 0, 44, 44)];
        self.broadcastPickerView.hidden = YES;
        self.broadcastPickerView.showsMicrophoneButton = NO;
        self.broadcastPickerView.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin;
#ifdef APP_SC_GROUP_ID
        self.broadcastPickerView.preferredExtension = @SC_PRODUCT_BUNDLE_IDENTIFIER;
#else
        NSAssert(NO, @"please config APP_SC_GROUP_ID in Common.xcconfig");
#endif
        [self.view addSubview:self.broadcastPickerView];
    }
}

- (void)setupLivePusher {
    
    //  创建推流器  
    self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
    
    // 设置屏幕推流分辨率 
    VeLiveVideoEncoderConfiguration *video = [[VeLiveVideoEncoderConfiguration alloc] initWithResolution:VeLiveVideoResolutionScreen];
    [self.livePusher setVideoEncoderConfiguration:video];
    
    //  设置推流器回调  
    [self.livePusher setObserver:self];
    
    //  设置周期性信息回调  
    [self.livePusher setStatisticsObserver:self interval:3];
    
    // 设置录屏推流监听 
    [self.livePusher setScreenCaptureObserver:self];
    
    //  请求麦克风权限  
    [VeLiveDeviceCapture requestMicrophoneAuthorization:^(BOOL granted) {
        if (!granted) {
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
            self.streamUrl = [model.result getRtmpPushUrl];
            [self startBroadcast];
#ifdef APP_SC_GROUP_ID
            // 开始屏幕采集 
            [self.livePusher startScreenCapture:@APP_SC_GROUP_ID];
#else
            NSAssert(NO, @"please config APP_SC_GROUP_ID in Common.xcconfig");
#endif
        }];
    } else {
        [self stopBroadcast];
        //  停止推流  
        [self.livePusher stopScreenCapture];
    }
}

- (void)startBroadcast {
    if (@available(iOS 12.0, *)) {
        if (self.broadcastPickerView != nil) {
            for (UIView* view in self.broadcastPickerView.subviews) {
                UIButton* button = (UIButton*)view;
                [button sendActionsForControlEvents:UIControlEventAllTouchEvents];
            }
        }
    } else {
        NSLog(@"VeLiveQuickStartDemo: below iOS 12 not support screen sharing");
    }
}

- (void)stopBroadcast {
    if (@available(iOS 12.0, *)) {
        if (self.broadcastPickerView != nil) {
            for (UIView* view in self.broadcastPickerView.subviews) {
                UIButton* button = (UIButton*)view;
                [button sendActionsForControlEvents:UIControlEventAllTouchEvents];
            }
        }
    } else {
        NSLog(@"VeLiveQuickStartDemo: below iOS 12 not support screen sharing");
    }
}

// MARK: -  VeLiveScreenCaptureStatusObserver delegate

- (void)broadcastStarted {
    if (self.streamUrl == nil) {
        [self stopBroadcast];
        [self.livePusher stopScreenCapture];
        return;
    }
    if (!self.pushControlBtn.isSelected) {
        self.pushControlBtn.selected = YES;
    }
    [self startTimer];
    // 开始音频采集 
    [self.livePusher startAudioCapture:VeLiveAudioCaptureMicrophone];
    // 开始推流 
    [self.livePusher startPush:self.streamUrl];
}

- (void)broadcastPaused {
}

- (void)broadcastResumed {
}

- (void)broadcastFinished {
    [self stopTimer];
    if (self.pushControlBtn.isSelected) {
        self.pushControlBtn.selected = NO;
    }
    // 停止音频采集 
    [self.livePusher stopAudioCapture];
    // 停止推流 
    [self.livePusher stopPush];
}

- (void)startTimer {
    if (self.timer == nil) {
        self.dateFormatter = [[NSDateFormatter alloc] init];
        self.dateFormatter.dateFormat = @"HH:mm:ss.SSS";
        __weak __typeof__(self)weakSelf = self;
        self.timer = [NSTimer timerWithTimeInterval:1/30.0 repeats:YES block:^(NSTimer * _Nonnull timer) {
            __strong __typeof__(weakSelf)self = weakSelf;
            self.timeLabel.text = [self.dateFormatter stringFromDate:NSDate.date];
        }];
        [[NSRunLoop mainRunLoop] addTimer:self.timer forMode:NSRunLoopCommonModes];
        [self.timer fire];
    }
}

- (void)stopTimer {
    if (self.timer != nil) {
        if (self.timer.isValid) {
            [self.timer invalidate];
        }
        self.timer = nil;
        self.timeLabel.text = nil;
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
    self.title = NSLocalizedString(@"Home_Screen_Push", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlLabel.text = NSLocalizedString(@"Push_Url_Tip", nil);
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}

@end
