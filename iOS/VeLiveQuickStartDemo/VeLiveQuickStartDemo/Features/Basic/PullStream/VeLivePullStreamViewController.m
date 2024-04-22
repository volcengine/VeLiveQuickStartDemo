/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePullStreamViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
直播拉流
 本文件展示如何集成直播拉流功能
 1、初始化推流器 API: self.livePlayer = [[TVLManager alloc] initWithOwnPlayer:YES];
 2、配置推流器 API: [self.livePlayer setConfig:[[VeLivePlayerConfiguration alloc] init]];
 3、配置渲染视图 API：[self.view insertSubview:self.livePlayer.playerView atIndex:0];
 4、配置播放地址 API: [self.livePlayer setPlayUrl:@"http://pull.example.com/pull.flv"];
 5、开始播放 API: [self.livePlayer play];
 */
#import "VeLivePullStreamViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePullStreamViewController () <VeLivePlayerObserver>
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (nonatomic, strong) TVLManager *livePlayer;
@property (weak, nonatomic) IBOutlet UIButton *playControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *fillModeControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *muteControlBtn;
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@end

@implementation VeLivePullStreamViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupLivePlayer];
}

- (void)dealloc {
    //  销毁直播播放器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePlayer destroy];
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
            sender.selected = !sender.isSelected;
        }];
    }
}

- (IBAction)fillModeControl:(UIButton *)sender {
    [self showFillModeAlert:^(VeLivePlayerFillMode mode) {
        //  设置填充模式  
        [self.livePlayer setRenderFillMode:mode];
    }];
}

- (IBAction)muteControl:(UIButton *)sender {
    //  静音/取消静音  
    [self.livePlayer setMute:!sender.isSelected];
    sender.selected = !sender.isSelected;
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

- (void)showFillModeAlert:(void (^)(VeLivePlayerFillMode mode))block {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Pull_Stream_Fill_Mode_Alert_Title", nil)
                                                                   message:nil
                                                            preferredStyle:(UIAlertControllerStyleActionSheet)];
    [alert addAction:[UIAlertAction actionWithTitle:NSLocalizedString(@"Pull_Stream_Fill_Mode_Alert_AspectFill", nil)
                                              style:(UIAlertActionStyleDefault)
                                            handler:^(UIAlertAction * _Nonnull action) {
        block(VeLivePlayerFillModeAspectFill);
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:NSLocalizedString(@"Pull_Stream_Fill_Mode_Alert_AspectFit", nil)
                                              style:(UIAlertActionStyleDefault)
                                            handler:^(UIAlertAction * _Nonnull action) {
        block(VeLivePlayerFillModeAspectFit);
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:NSLocalizedString(@"Pull_Stream_Fill_Mode_Alert_FullFill", nil)
                                              style:(UIAlertActionStyleDefault)
                                            handler:^(UIAlertAction * _Nonnull action) {
        block(VeLivePlayerFillModeFullFill);
    }]];
    [self showDetailViewController:alert sender:nil];
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Pull_Stream", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlLabel.text = NSLocalizedString(@"Pull_Stream_Url_Tip", nil);
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Start_Play", nil) forState:(UIControlStateNormal)];
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Stop_Play", nil) forState:(UIControlStateSelected)];
    
    [self.muteControlBtn setTitle:NSLocalizedString(@"Pull_Mute", nil) forState:((UIControlStateNormal))];
    [self.muteControlBtn setTitle:NSLocalizedString(@"Pull_UnMute", nil) forState:((UIControlStateSelected))];
    
    [self.fillModeControlBtn setTitle:NSLocalizedString(@"Pull_Fill_Mode", nil) forState:((UIControlStateNormal))];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
