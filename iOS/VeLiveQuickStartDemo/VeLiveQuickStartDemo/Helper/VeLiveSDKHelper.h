/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveSDKHelper.h
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import <UIKit/UIKit.h>

/*
 本文件存放 SDK 基础配置信息，不分信息在进入相应页面是可以在界面上进行修改
 本文件存放SDK基础配置信息，包含 SDK AppID， License 文件名，推拉流地址，连麦互动房间ID、主播/观众 UID及临时Token
 SDK 配置信息申请：https://console.volcengine.com/live/main/sdk
 推拉流地址生成参考文档：https://console.volcengine.com/live/main/locationGenerate
 互动直播相关参考文档：https://console.volcengine.com/rtc/listRTC
 */
// AppID
#define TTSDK_APP_ID @""
/*
 License 名称，当前 Demo文件存放在与本文件同级目录下，如果做SDK快速验证，可直接替换 ttsdk.lic 文件内容
 */
#define TTSDK_LICENSE_NAME @"ttsdk.lic"

/*
 rtmp, RTM, Quic 推流地址
 生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
 */
#define LIVE_PUSH_URL @""
#define LIVE_RTM_PUSH_URL @""

/*
 RTM, rtmp、flv、m3u8 拉流地址
 生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
 */
#define LIVE_PULL_URL @""
#define LIVE_RTM_PULL_URL @""


/*
 互动直播AppID
 */
#define RTC_APPID @""

/*
 互动直播主播房间ID
 */
#define RTC_ROOM_ID @""

/*
 互动直播主播用户ID
 */
#define RTC_USER_ID @""

/*
 互动直播主播用户 Token
 生成方式：使用主播房间ID和主播用户ID在RTC控制台生成
 https://console.volcengine.com/rtc/listRTC
 */
#define RTC_USER_TOKEN @""

/*
 主播与主播PK时，对方主播的房间ID
 */
#define RTC_OTHER_ROOM_ID @""

/*
 主播与主播PK时，当前主播加入对方主播房间内的Token
 生成方式：使用当前主播的用户ID和对方主播的房间ID在控制台生成
 */
#define RTC_OTHER_ROOM_TOKEN @""

#import <TTSDK/VeLivePusher.h>
#import <TTSDK/VeLivePlayer.h>
#import <TTSDK/TTSDKManager.h>
#import <VolcEngineRTC/VolcEngineRTC.h>

#import "VeLiveDeviceCapture.h"

NS_ASSUME_NONNULL_BEGIN
@class VeLivePlayerStatistics;
@class VeLivePusherStatistics;
@interface VeLiveSDKHelper : NSObject
/*
 初始化 SDK 相关配置
 */
+ (void)initTTSDK;

/*
 获取推流信息字符串，方便多处使用
 */
+ (NSAttributedString *)getPushInfoString:(VeLivePusherStatistics *)statistics;

/*
 获取拉流信息字符串，方便多处使用
 */
+ (NSAttributedString *)getPlaybackInfoString:(VeLivePlayerStatistics *)statistics;
@end

NS_ASSUME_NONNULL_END
