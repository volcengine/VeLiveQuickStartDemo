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
/*
摄像头推流，集成智能美化特效
 
 本文件展示如何集成摄像头推流和智能美化特效
 1、首先集成智能美化特效SDK，推荐集成动态库
 2、初始化推流器 API: self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
 3、设置预览视图 API: [self.livePusher setRenderView:self.view];
 4、打开麦克风采集 API:[self.livePusher startAudioCapture:(VeLiveAudioCaptureMicrophone)];
 5、打开摄像头采集 API: [self.livePusher startVideoCapture:(VeLiveVideoCaptureFrontCamera)];
 6、开始推流 API：[self.livePusher startPush:@"rtmp://push.example.com/rtmp"];
 7、初始化美颜相关参数 API: [[self.livePusher getVideoEffectManager] setupWithConfig:[[VeLiveVideoEffectLicenseConfiguration alloc] initWithPath:licensePath]];
 8、设置模型资源包 API: [[self.livePusher getVideoEffectManager] setAlgoModelPath:algoModelPath];
 9、设置美颜美型 API: [[self.livePusher getVideoEffectManager] setComposeNodes:@[]];
 10、设置滤镜 API: [[self.livePusher getVideoEffectManager] setFilter:@""];
 11、设置贴纸 API: [[self.livePusher getVideoEffectManager] setSticker:@""];
 */
#import "VeLivePushBeautyViewController.h"
#import "VeLiveSDKHelper.h"
@interface VeLivePushBeautyViewController () <VeLivePusherObserver, VeLivePusherStatisticsObserver>
@property (weak, nonatomic) IBOutlet UILabel *urlLabel;
@property (weak, nonatomic) IBOutlet UILabel *infoLabel;
@property (weak, nonatomic) IBOutlet UITextField *urlTextField;
@property (weak, nonatomic) IBOutlet UIButton *pushControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *beautyControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *filterControlBtn;
@property (weak, nonatomic) IBOutlet UIButton *stickerControlBtn;
@property (nonatomic, strong) VeLivePusher *livePusher;
@end

@implementation VeLivePushBeautyViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupCommonUIConfig];
    [self setupLivePusher];
    [self setupEffectSDK];
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

- (void)setupEffectSDK {
    //  注意：本方法只在工程中集成过智能美化特效的SDK时生效  
    VeLiveVideoEffectManager *effectManager = [self.livePusher getVideoEffectManager];
    //  特效鉴权License路径，请根据工程配置查找正确的路径  
    NSString *licensePath =  [NSString stringWithFormat:@"LicenseBag.bundle/%@", EFFECT_LICENSE_NAME];
    licensePath = [NSBundle.mainBundle pathForResource:licensePath ofType:nil];
    //  特效模型资源包路径  
    NSString *algoModelPath = [NSBundle.mainBundle pathForResource:@"ModelResource.bundle" ofType:nil];
    
    if (![NSFileManager.defaultManager fileExistsAtPath:licensePath]) {
        return;
    }
    //  创建美颜配置  
    VeLiveVideoEffectLicenseConfiguration *licConfig = [[VeLiveVideoEffectLicenseConfiguration alloc] initWithPath:licensePath];
    //  设置美颜配置  
    [effectManager setupWithConfig:licConfig];
    //  设置算法包路径  
    [effectManager setAlgoModelPath:algoModelPath];
    
    //  开启美颜特效处理  
    if ([effectManager setEnable:YES] != 0) {
        NSLog(@"VeLiveQuickStartDemo: license unavailabel, please check");
    }
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

- (IBAction)beautyControl:(UIButton *)sender {
    //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
    NSString *beautyPath = [NSBundle.mainBundle pathForResource:@"ComposeMakeup.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:beautyPath]) {
        return;
    }
    //  设置美颜美型特效资源包  
    [self.livePusher.getVideoEffectManager setComposeNodes:@[beautyPath]];
    //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
    [self.livePusher.getVideoEffectManager updateComposerNodeIntensity:beautyPath nodeKey:@"whiten" intensity:0.5];
}

- (IBAction)filterControl:(UIButton *)sender {
    //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
    
    NSString *filterPath = [NSBundle.mainBundle pathForResource:@"FilterResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:filterPath]) {
        return;
    }
    //  设置滤镜资源包路径  
    [self.livePusher.getVideoEffectManager setFilter:filterPath];
    //  设置滤镜特效强度  
    [self.livePusher.getVideoEffectManager updateFilterIntensity:0.5];
}

- (IBAction)stickerControl:(UIButton *)sender {
    //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  

    NSString *stickerPath = [NSBundle.mainBundle pathForResource:@"StickerResource.bundle/xxx" ofType:nil];
    if (![NSFileManager.defaultManager fileExistsAtPath:stickerPath]) {
        return;
    }
    //  设置贴纸资源包路径  
    [self.livePusher.getVideoEffectManager setSticker:stickerPath];
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
    self.title = NSLocalizedString(@"Push_Beauty", nil);
    self.navigationItem.backBarButtonItem.title = nil;
    self.navigationItem.backButtonTitle = nil;
    self.urlLabel.text = NSLocalizedString(@"Push_Url_Tip", nil);
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Start_Push", nil) forState:(UIControlStateNormal)];
    [self.pushControlBtn setTitle:NSLocalizedString(@"Push_Stop_Push", nil) forState:(UIControlStateSelected)];
    
    [self.beautyControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Compose", nil) forState:(UIControlStateNormal)];
    [self.filterControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Filter", nil) forState:(UIControlStateNormal)];
    [self.stickerControlBtn setTitle:NSLocalizedString(@"Push_Beauty_Sticker", nil) forState:(UIControlStateNormal)];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}
@end
