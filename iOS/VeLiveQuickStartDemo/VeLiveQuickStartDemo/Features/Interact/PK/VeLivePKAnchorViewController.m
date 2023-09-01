/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLivePKAnchorViewController.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import "VeLivePKAnchorViewController.h"
#import "VeLiveAnchorManager.h"
@interface VeLivePKAnchorViewController () <VeLiveAnchorDelegate>
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *seiControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *pkControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *beautyControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *filterControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *stickerControlBtn;
//  参与连麦的用户列表  
@property (nonatomic, strong) NSMutableArray <NSString *> *usersInRoom;
//  当前主播视图  
@property (nonatomic, strong) IBOutlet UIView *localView;
//  对方主播视图  
@property (nonatomic, strong) IBOutlet UIView *remoteView;
@property (weak, nonatomic) IBOutlet UIView *previewView;
@property (weak, nonatomic) IBOutlet UIStackView *pkStackView;
//  主播+连麦管理器  
@property (nonatomic, strong) VeLiveAnchorManager *liveAnchorManager;

@end

@implementation VeLivePKAnchorViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupAnchorManager];
    [self setupEffectSDK];
}

- (void)dealloc {
    //  销毁连麦管理器  
    //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
    if (self.liveAnchorManager != nil) {
        [self.liveAnchorManager destory];
        self.liveAnchorManager = nil;
    }
}

- (void)setupAnchorManager {
    self.usersInRoom = [NSMutableArray arrayWithCapacity:2];
    self.liveAnchorManager = [[VeLiveAnchorManager alloc] initWithAppId:RTC_APPID
                                                                 userId:self.userID];
    //  配置本地预览视图  
    [self.liveAnchorManager setLocalVideoView:self.previewView];
    //  开启视频采集  
    [self.liveAnchorManager startVideoCapture];
    //  开启音频采集  
    [self.liveAnchorManager startAudioCapture];
    //  开始推流  
    [self startPush];
}

- (void)startPush {
    if (self.urlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please config push url");
        return;
    }
    //  开始推流  
    [self.liveAnchorManager startPush:self.urlTextField.text];
    [self.view sendSubviewToBack:self.previewView];
}

- (void)stopPush {
    [self.liveAnchorManager stopPush];
}

//  开始跨房间转推  
- (void)startForward {
    //  跨房间转推  
    ForwardStreamConfiguration *cfg = [[ForwardStreamConfiguration alloc] init];
    cfg.roomId = self.otherRoomID;
    cfg.token = self.otherRoomToken;
    [self.liveAnchorManager startForwardStream:@[cfg]];
}

//  停止跨房间转推  
- (void)stopForward {
    [self.liveAnchorManager stopForwardStream];
}

- (void)clearInteractUsers {
    //  开始连麦  
    //  清空历史用户，业务逻辑处理  
    [self.usersInRoom removeAllObjects];
}

- (void)startPK {
    [self clearInteractUsers];
    self.previewView.hidden = YES;
    self.pkStackView.hidden = NO;
    [self.liveAnchorManager setLocalVideoView:self.localView];
    //  加入房间，等加入房间回调后，开始跨房间转推  
    [self.liveAnchorManager startInteract:self.roomID
                                    token:self.token
                                 delegate:self];
}

- (void)stopPK {
    self.previewView.hidden = NO;
    self.pkStackView.hidden = YES;
    [self.liveAnchorManager setLocalVideoView:self.previewView];
    [self.view sendSubviewToBack:self.previewView];
    [self clearInteractUsers];
    //  停止跨房间转推  
    [self stopForward];
    //  离开房间  
    [self.liveAnchorManager stopInteract];
}

- (IBAction)pushControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config Url");
        return;
    }
    if (!sender.isSelected) {
        [self startPush];
    } else {
        [self stopPush];
    }
    sender.selected = !sender.isSelected;
}

- (IBAction)pkControl:(UIButton *)sender {
    if (self.urlTextField.text.length <= 0) {
        NSLog(@"VeLiveQuickStartDemo: Please Config Url");
        return;
    }
    if (!sender.isSelected) {
        [self startPK];
    } else {
        [self stopPK];
    }
    sender.selected = !sender.isSelected;
}

- (IBAction)seiControl:(UIButton *)sender {
    [self.liveAnchorManager sendSeiMessage:@"anchor_test_sei" repeat:20];
}

- (void)setupEffectSDK {
    //  注意：本方法只在工程中集成过智能美化特效的SDK时生效  
    ByteRTCVideo *rtcVideo = self.liveAnchorManager.rtcVideo;
    
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
    [self.liveAnchorManager.rtcVideo setVideoEffectNodes:@[beautyPath]];
    
    //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
    [self.liveAnchorManager.rtcVideo updateVideoEffectNode:beautyPath nodeKey:@"whiten" nodeValue:0.5];
}

- (IBAction)filterControl:(UIButton *)sender {
    //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
    NSString *filterPath = [NSBundle.mainBundle pathForResource:@"FilterResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:filterPath]) {
        return;
    }
    [self.liveAnchorManager.rtcVideo setVideoEffectColorFilter:filterPath];
    [self.liveAnchorManager.rtcVideo setVideoEffectColorFilterIntensity:0.5];
}

- (IBAction)stickerControl:(UIButton *)sender {
    //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
    NSString *stickerPath = [NSBundle.mainBundle pathForResource:@"StickerResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:stickerPath]) {
        return;
    }
    [self.liveAnchorManager.rtcVideo appendVideoEffectNodes:@[stickerPath]];
}


// MARK: - VeLiveAnchorDelegate
- (void)manager:(VeLiveAnchorManager *)manager onJoinRoom:(NSString *)uid state:(NSInteger)state {
    [self.usersInRoom addObject:[uid copy]];
    
    //  更新布局参数  
    [manager updateLiveTranscodingLayout:[self rtcLayout]];
    
    //  当前主播，开始跨房间转推  
    if ([uid isEqualToString:self.userID]) {
        [self startForward];
    }
}

- (void)manager:(VeLiveAnchorManager *)manager onUserJoined:(NSString *)uid {
    [self.usersInRoom addObject:uid.copy];
}

- (void)manager:(VeLiveAnchorManager *)manager onUserLeave:(NSString *)uid {
    //  更新连麦用户列表  
    [self.usersInRoom removeObject:uid];
    //  更新混流布局  
    [manager updateLiveTranscodingLayout:[self rtcLayout]];
}

- (void)manager:(VeLiveAnchorManager *)manager onUserPublishStream:(nonnull NSString *)uid type:(ByteRTCMediaStreamType)streamType {
    if (streamType == ByteRTCMediaStreamTypeAudio) {
        return;
    }
    //  设置远端用户view  
    [manager setRemoteVideoView:self.remoteView forUid:uid];
    
    //  更新布局参数  
    [manager updateLiveTranscodingLayout:[self rtcLayout]];
}

- (void)manager:(VeLiveAnchorManager *)manager onUserUnPublishStream:(nonnull NSString *)uid type:(ByteRTCMediaStreamType)streamType reason:(ByteRTCStreamRemoveReason)reason {
    if (streamType == ByteRTCMediaStreamTypeAudio) {
        return;
    }
    [self.usersInRoom removeObject:uid];
    //  移除远端视图  
    [manager setRemoteVideoView:nil forUid:uid];
    //  更新混流布局  
    [manager updateLiveTranscodingLayout:[self rtcLayout]];
}

- (ByteRTCVideoCompositingLayout *)rtcLayout {
    //  初始化布局  
    ByteRTCVideoCompositingLayout * layout = [[ByteRTCVideoCompositingLayout alloc]init];
    
    //  设置背景色  
    layout.backgroundColor = @"#000000"; // 仅供参考 
 
    NSMutableArray *regions = [[NSMutableArray alloc]initWithCapacity:6];
    CGFloat viewWidth = self.view.bounds.size.width;
    CGFloat viewHeight = self.view.bounds.size.height;
    CGFloat pkViewWidth = (viewWidth - 8) * 0.5 / viewWidth;;
    CGFloat pkViewHeight =  260 / viewHeight;
    CGFloat pkViewY =  209 / viewHeight;
    
    [self.usersInRoom enumerateObjectsUsingBlock:^(NSString * _Nonnull uid, NSUInteger idx, BOOL * _Nonnull stop) {
        
        ByteRTCVideoCompositingRegion *region = [[ByteRTCVideoCompositingRegion alloc]init];
        region.uid          = uid;
        region.roomId       = self.roomID;
        region.localUser    = [uid isEqualToString:self.userID]; //  判断是否为当前主播  
        region.renderMode   = ByteRTCRenderModeHidden;
        
        if (region.localUser) { // 当前主播位置，仅供参考 
            region.x        = 0.0;
            region.y        = pkViewY;
            region.width    = pkViewWidth;
            region.height   = pkViewHeight;
            region.zOrder   = 0;
            region.alpha    = 1.0;
        } else { //  远端用户位置，仅供参考  
            region.x        = (viewWidth * 0.5 + 8) / viewWidth;
            region.y        = pkViewY;
            region.width    = pkViewWidth;
            region.height   =  pkViewHeight;
            region.zOrder   = 1;
            region.alpha    = 1;
        }
        [regions addObject:region];
    }];
    layout.regions = regions;
    return layout;
}

- (void)setupCommonUIConfig {
    self.title = NSLocalizedString(@"Interact_Link_Anchor_Title", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlTextField.text = LIVE_PUSH_URL;
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
    
    [self.pkControlBtn setTitle:NSLocalizedString(@"Interact_Start_PK", nil) forState:(UIControlStateNormal)];
    [self.pkControlBtn setTitle:NSLocalizedString(@"Interact_Stop_PK", nil) forState:(UIControlStateSelected)];
    
    [self.seiControlBtn setTitle:NSLocalizedString(@"Interact_Send_SEI", nil) forState:(UIControlStateNormal)];
    
    [self.beautyControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Compose", nil) forState:(UIControlStateNormal)];
    [self.filterControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Filter", nil) forState:(UIControlStateNormal)];
    [self.stickerControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Sticker", nil) forState:(UIControlStateNormal)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
