/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveAudienceManager.h
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/2/22.
//

#import <UIKit/UIKit.h>
#import "VeLiveSDKHelper.h"
#import "VeLiveConfig.h"
NS_ASSUME_NONNULL_BEGIN

@class VeLiveAudienceManager;
@protocol VeLiveAudienceDelegate <NSObject>
//  用户加入房间  
- (void)manager:(VeLiveAudienceManager *)manager onUserJoined:(NSString *)uid;
//  用户离开房间事件回调  
- (void)manager:(VeLiveAudienceManager *)manager onUserLeave:(NSString *)uid;
//  本人加入房间回调  
- (void)manager:(VeLiveAudienceManager *)manager onJoinRoom:(NSString *)uid;
//  用户发布视频流回调，用于更新合流布局  
- (void)manager:(VeLiveAudienceManager *)manager onUserPublishStream:(NSString *)uid type:(ByteRTCMediaStreamType)streamType;
//  用户取消发布视频流回调，用于更新合流布局  
- (void)manager:(VeLiveAudienceManager *)manager onUserUnPublishStream:(NSString *)uid type:(ByteRTCMediaStreamType)streamType reason:(ByteRTCStreamRemoveReason)reason;
@end

@interface VeLiveAudienceManager : NSObject
//  播放器  
@property (nonatomic, strong, readonly) TVLManager *livePlayer;
//  拉流视图  
@property (nonatomic, strong) UIView *localVideoView;
//  直播+RTC 推流配置  
@property (nonatomic, strong, nullable) VeLiveConfig *config;
//  rtc 视频管理  
@property (nonatomic, strong, readonly, nullable) ByteRTCVideo *rtcVideo;
//  rtc 房间管理  
@property (nonatomic, strong, readonly, nullable) ByteRTCRoom *rtcRoom;
//  创建时传入的appid  
@property (nonatomic, copy, readonly, nullable) NSString *appId;
//  创建时，传入的 userId  
@property (nonatomic, copy, readonly, nullable) NSString *userId;
//  加入的房间ID  
@property (nonatomic, copy, readonly, nullable) NSString *roomId;
//  加入房间的Token  
@property (nonatomic, copy, readonly, nullable) NSString *token;
//  是否连麦中  
@property (nonatomic, assign, getter=isInteractive, readonly) BOOL interactive;
//  代理  
@property(nonatomic, weak) id <VeLiveAudienceDelegate> delegate;

//  初始化  
- (instancetype)initWithAppId:(NSString *)appId userId:(NSString *)userId NS_DESIGNATED_INITIALIZER;

//  配置远端用户视图  
- (void)setRemoteVideoView:(nullable UIView *)view forUid:(NSString *)uid;

//  开启视频采集  
- (void)startVideoCapture;

//  停止视频采集  
- (void)stopVideoCapture;

//  开启音频采集  
- (void)startAudioCapture;

//  停止音频采集  
- (void)stopAudioCapture;

//  开始连麦  
- (void)startInteract:(NSString *)roomId token:(NSString *)token delegate:(id <VeLiveAudienceDelegate>)delegate;

//  停止连麦  
- (void)stopInteract;

//  发送SEI消息  
// - Parameters:
//    - message: sei 消息长度，最长2kb    
//   - repeat: [0, 30]
- (void)sendSeiMessage:(NSString *)message repeat:(int)repeat;

//  销毁引擎  
- (void)destory;

//  开始播放  
- (void)startPlay:(NSString *)url;

//  停止播放  
- (void)stopPlay;
@end

NS_ASSUME_NONNULL_END
