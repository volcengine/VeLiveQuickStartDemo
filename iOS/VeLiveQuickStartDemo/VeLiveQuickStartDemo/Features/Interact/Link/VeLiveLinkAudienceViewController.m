/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveLinkAudienceViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import "VeLiveLinkAudienceViewController.h"
#import "VeLiveAudienceManager.h"
@interface VeLiveLinkAudienceViewController () <VeLiveAudienceDelegate>
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UIButton *interactControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *beautyControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *filterControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *seiControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *stickerControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *playControlBtn;
@property (weak, nonatomic) IBOutlet UIStackView *remoteStackView;
//  参与连麦的用户列表  
@property (nonatomic, strong) NSMutableArray <NSString *> *usersInRoom;
//  连麦过程中本地用户View  
@property (nonatomic, strong) IBOutlet UIView *localView;
//  连麦过程中远端用户视图列表  
@property (nonatomic, strong) NSMutableDictionary <NSString *, UIView *> *remoteUserViews;
@property (nonatomic, strong) VeLiveAudienceManager *audienceManager;
@end

@implementation VeLiveLinkAudienceViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupAudienceManager];
    [self setupEffectSDK];
}

- (void)dealloc {
    //  销毁连麦管理器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    if (self.audienceManager != nil) {
        [self.audienceManager destory];
        self.audienceManager = nil;
    }
}

- (void)setupAudienceManager {
    self.usersInRoom = [NSMutableArray arrayWithCapacity:6];
    self.remoteUserViews = [[NSMutableDictionary alloc] initWithCapacity:6];
    self.audienceManager = [[VeLiveAudienceManager alloc] initWithAppId:RTC_APPID
                                                                 userId:self.userID];
    [self.audienceManager setLocalVideoView:self.localView];
    [self startPlay];
}

- (void)startPlay {
    if (self.urlTextField.text <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please config pull url");
        return;
    }
    [self.audienceManager startPlay:self.urlTextField.text];
}

- (void)clearInteractUsers {
    //  开始连麦  
    //  清空历史用户，业务逻辑处理  
    [self.usersInRoom removeAllObjects];
    [self.remoteUserViews.allValues enumerateObjectsUsingBlock:^(UIView * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self.remoteStackView removeArrangedSubview:obj];
    }];
}

- (void)startInteractive {
    [self clearInteractUsers];
    [self.remoteUserViews removeAllObjects];
    [self.audienceManager startInteract:self.roomID token:self.token delegate:self];
}

- (void)stopInteractive {
    //  清空历史用户，业务逻辑处理  
    [self clearInteractUsers];
    
    //  停止连麦  
    [self.audienceManager stopInteract];
}

- (IBAction)playControl:(UIButton *)sender {
    if (sender.isSelected) {
        [self.audienceManager stopPlay];
    } else {
        [self.audienceManager startPlay:self.urlTextField.text];
    }
    sender.selected = !sender.isSelected;
}

- (IBAction)interactControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config Url");
        return;
    }
    if (!sender.isSelected) {
        [self startInteractive];
    } else {
        [self stopInteractive];
    }
    sender.selected = !sender.isSelected;
}

- (IBAction)seiControl:(UIButton *)sender {
    [self.audienceManager sendSeiMessage:@"audience_test_sei_for_interactive" repeat:20];
}


- (void)setupEffectSDK {
    //  注意：本方法只在工程中集成过智能美化特效的SDK时生效  
    ByteRTCVideo *rtcVideo = self.audienceManager.rtcVideo;
    
    //  特效鉴权License路径，请根据工程配置查找正确的路径  
    NSString *licensePath = [NSBundle.mainBundle pathForResource:@"LicenseBag.bundle/xxx.licbag" ofType:nil];
    
    if (![NSFileManager.defaultManager fileExistsAtPath:licensePath]) {
        return;
    }
    
    [rtcVideo checkVideoEffectLicense:licensePath];
    
    //  特效算法资源包路径  
    NSString *algoModelPath = [NSBundle.mainBundle pathForResource:@"ModelResource.bundle" ofType:nil];
    [rtcVideo setVideoEffectAlgoModelPath:algoModelPath];
    
    //  开启美颜特效处理  
    if ([rtcVideo enableVideoEffect:YES] != 0) {
        NSLog(@"VeLiveQuickStartDemo: license unavailabel, please check");
    }
}

- (IBAction)beautyControl:(UIButton *)sender {
    //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
    NSString *beautyPath = [NSBundle.mainBundle pathForResource:@"ComposeMakeup.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:beautyPath]) {
        return;
    }
    //  设置美颜美型特效资源包  
    [self.audienceManager.rtcVideo setVideoEffectNodes:@[beautyPath]];
    //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
    [self.audienceManager.rtcVideo updateVideoEffectNode:beautyPath nodeKey:@"whiten" nodeValue:0.5];
}

- (IBAction)filterControl:(UIButton *)sender {
    //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
    NSString *filterPath = [NSBundle.mainBundle pathForResource:@"FilterResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:filterPath]) {
        return;
    }
    [self.audienceManager.rtcVideo setVideoEffectColorFilter:filterPath];
    [self.audienceManager.rtcVideo setVideoEffectColorFilterIntensity:0.5];
}

- (IBAction)stickerControl:(UIButton *)sender {
    //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
    NSString *stickerPath = [NSBundle.mainBundle pathForResource:@"StickerResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:stickerPath]) {
        return;
    }
    [self.audienceManager.rtcVideo appendVideoEffectNodes:@[stickerPath]];
}


// MARK: - VeLiveAudienceDelegate
- (void)manager:(VeLiveAudienceManager *)manager onUserJoined:(NSString *)uid {
    
}

- (void)manager:(VeLiveAudienceManager *)manager onUserLeave:(NSString *)uid {
    
}

- (void)manager:(VeLiveAudienceManager *)manager onJoinRoom:(NSString *)uid {
    
}

- (void)manager:(VeLiveAudienceManager *)manager onUserPublishStream:(nonnull NSString *)uid type:(ByteRTCMediaStreamType)streamType {
    if (streamType == ByteRTCMediaStreamTypeAudio) {
        return;
    }
    //  设置远端用户view  
    UIView *remoteView = [self.remoteUserViews objectForKey:uid];
    if (remoteView == nil) {
        remoteView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 130, 130)];
        [self.remoteUserViews setObject:remoteView forKey:uid];
    }
    [self.remoteStackView addArrangedSubview:remoteView];
    [manager setRemoteVideoView:remoteView forUid:uid];
}

- (void)manager:(VeLiveAudienceManager *)manager onUserUnPublishStream:(nonnull NSString *)uid type:(ByteRTCMediaStreamType)streamType reason:(ByteRTCStreamRemoveReason)reason {
    if (streamType == ByteRTCMediaStreamTypeAudio) {
        return;
    }
    UIView *remoteView = [self.remoteUserViews objectForKey:uid];
    if (remoteView != nil) {
        [self.remoteStackView removeArrangedSubview:remoteView];
        [self.remoteUserViews removeObjectForKey:uid];
    }
    [manager setRemoteVideoView:nil forUid:uid];
}


- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Interact_Link_Audience_Title", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlTextField.text = LIVE_PULL_URL;
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Start_Play", nil) forState:(UIControlStateNormal)];
    [self.playControlBtn setTitle:NSLocalizedString(@"Pull_Stream_Stop_Play", nil) forState:(UIControlStateSelected)];
    
    [self.interactControlBtn setTitle:NSLocalizedString(@"Interact_Start_Link", nil) forState:(UIControlStateNormal)];
    [self.interactControlBtn setTitle:NSLocalizedString(@"Interact_Stop_Link", nil) forState:(UIControlStateSelected)];
    [self.seiControlBtn setTitle:NSLocalizedString(@"Interact_Send_SEI", nil) forState:(UIControlStateNormal)];
    [self.beautyControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Compose", nil) forState:(UIControlStateNormal)];
    [self.filterControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Filter", nil) forState:(UIControlStateNormal)];
    [self.stickerControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Sticker", nil) forState:(UIControlStateNormal)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}


@end
