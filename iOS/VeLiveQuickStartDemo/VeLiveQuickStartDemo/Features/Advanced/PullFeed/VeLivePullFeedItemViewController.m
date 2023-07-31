/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLivePullFeedItemViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/7/24.
//

#import "VeLivePullFeedItemViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePullFeedItemViewController () <VeLivePlayerObserver>
@property (nonatomic, strong) UILabel *infoLabel;
@property (nonatomic, strong) TVLManager *livePlayer;
@property (nonatomic, assign) BOOL preLoading;
@end

@implementation VeLivePullFeedItemViewController
@synthesize reuseIdentifier;

- (UIViewController *)initWithReuseIdentifier:(NSString *)reuseIdentifier {
    VeLivePullFeedItemViewController *vc = [[VeLivePullFeedItemViewController alloc] init];
    vc.reuseIdentifier = reuseIdentifier;
    return vc;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = UIColor.blackColor;
    [self setupLivePlayer];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self preLoadPlayer];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self play];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self stop];
}

- (void)dealloc {
    //  销毁直播播放器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    [self.livePlayer destroy];
    self.livePlayer = nil;
}

- (void)setUrl:(NSString *)url {
    if (![_url isEqualToString:url]) {
        _url = url.copy;
        [self preLoadPlayer];
    }
}

- (void)setupLivePlayer {
    if (self.livePlayer != nil) {
        return;
    }
    //  创建直播播放器  
    self.livePlayer = [[TVLManager alloc] initWithOwnPlayer:YES];
    
    //  设置播放器回调  
    [self.livePlayer setObserver:self];
    
    //  配置播放器  
    //  更多配置参考：https://www.volcengine.com/docs/6469/97858  
    VeLivePlayerConfiguration *cfg = [[VeLivePlayerConfiguration alloc] init];
    
    //  是否开启周期性信息回调  
    cfg.enableStatisticsCallback = YES;
    
    //  周期性信息回调间隔  
    cfg.statisticsCallbackInterval = 1;
    
    //  是否开启内部DNS解析  
    cfg.enableLiveDNS = YES;
    
    //  重试间隔  
    cfg.retryIntervalTimeMs = 1000;
    
    //  重试最大次数  
    cfg.retryMaxCount = 1;
    
    //  配置拉流播放器  
    [self.livePlayer setConfig:cfg];
    
    //  配置播放器视图  
    UIView *playerView = self.livePlayer.playerView;
    playerView.frame = self.view.bounds;
    playerView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view insertSubview:playerView atIndex:0];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-0-[playerView]-0-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(playerView)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-0-[playerView]-0-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(playerView)]];
    
    //  设置渲染填充模式  
    [self.livePlayer setRenderFillMode:(VeLivePlayerFillModeAspectFill)];
    
    //  播放器信息展示  
    [self setupInfoLabel];
}

- (void)setupInfoLabel {
    if (self.infoLabel != nil) {
        return;
    }
    self.infoLabel = [[UILabel alloc] init];
    self.infoLabel.textColor = UIColor.whiteColor;
    self.infoLabel.numberOfLines = 0;
    self.infoLabel.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.infoLabel];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-16-[_infoLabel]-16-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_infoLabel)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-100-[_infoLabel]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_infoLabel)]];
}

- (void)play {
    [self cancelPreLoadPlayer];
    if (self.url.length > 0) {
        [self.livePlayer play];
    }
}

- (void)stop {
    [self cancelPreLoadPlayer];
    [self.livePlayer stop];
    [self.livePlayer.playerView removeFromSuperview];
    [self.livePlayer destroy];
    self.livePlayer = nil;
}

- (void)preLoadPlayer {
    if (self.preLoading || self.livePlayer.isPlaying) {
        return;
    }
    self.preLoading = YES;
    if (self.livePlayer == nil) {
        [self setupLivePlayer];
    }
    self.infoLabel.attributedText = nil;
    self.livePlayer.playerView.hidden = YES;
    if (self.url.length > 0) {
        [self.livePlayer setPlayUrl:self.url];
        [self.livePlayer setMute:YES];
        [self.livePlayer play];
        NSLog(@"PreLoad Start %@ - %@", self.livePlayer, self.url);
    }
}

- (void)cancelPreLoadPlayer {
    if (self.livePlayer == nil) {
        return;
    }
    self.preLoading = NO;
    [self.livePlayer setMute:NO];
    NSLog(@"PreLoad Cancel %@ - %@", self.livePlayer, self.url);
}

- (void)stopPreLoadPlayer {
    if (self.livePlayer == nil) {
        return;
    }
    self.preLoading = NO;
    [self.livePlayer setMute:NO];
    [self.livePlayer stop];
    NSLog(@"PreLoad End %@ -  %@", self.livePlayer, self.url);
}


// MARK: -VeLivePlayerObserver
- (void)onFirstVideoFrameRender:(TVLManager *)player isFirstFrame:(BOOL)isFirstFrame {
    ///  首帧，展示播放器视图  
    self.livePlayer.playerView.hidden = NO;
    if (self.preLoading) {
        [self stopPreLoadPlayer];
    }
}

- (void)onError:(TVLManager *)player error:(VeLivePlayerError *)error {
    self.infoLabel.text = error.errorMsg;
}

- (void)onPlayerStatusUpdate:(TVLManager *)player status:(VeLivePlayerStatus)status {
    if (status == VeLivePlayerStatusPrepared) {
        self.infoLabel.text = @"Prepared";
    } else if (status == VeLivePlayerStatusPlaying) {
        self.infoLabel.text = @"Playing";
    } else if (status == VeLivePlayerStatusPaused) {
        self.infoLabel.text = @"Paused";
    } else if (status == VeLivePlayerStatusStopped) {
        self.infoLabel.text = @"Stopped";
    }
}

- (void)onStatistics:(TVLManager *)player statistics:(VeLivePlayerStatistics *)statistics {
    if (player.isPlaying) {
        self.infoLabel.attributedText = [VeLiveSDKHelper getPlaybackInfoString:statistics];
    }
}

@end
