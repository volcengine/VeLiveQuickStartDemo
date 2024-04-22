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
 *  API访问密钥 https://console.volcengine.com/iam/keymanage/
 */
#define ACCESS_KEY_ID @""
#define SECRET_ACCESS_KEY @""

/*
 * 直播推拉流 VHOST
 * https://console.volcengine.com/iam/resourcemanage/project/default/
 */
#define LIVE_VHOST @""

/*
 * 生成直播推拉流地址时使用
 * 举例: https://pull.example.com/live/abc.flv
 */
#define LIVE_APP_NAME @"live"


/*
 * 直播推流域名 https://console.volcengine.com/live/main/domain/list
 */
#define LIVE_PUSH_DOMAIN @""

/*
 * 直播拉流域名 https://console.volcengine.com/live/main/domain/list
 */
#define LIVE_PULL_DOMAIN @""

/*
 互动直播AppID
 */
#define RTC_APPID @""

/*
 互动直播 AppKey
 */
#define RTC_APPKEY @""

/*
 * CV License 名称，必须放到 App 的根目录下
 */
/*
 * CV License name，Must be placed in the root directory of the App
 */
#define EFFECT_LICENSE_NAME @""

#import <TTSDKFramework/TTSDKFramework.h>
#import <VolcEngineRTC/VolcEngineRTC.h>

#import "VeLiveDeviceCapture.h"
#import "VeLiveURLGenerator.h"
#import "VeLiveRTCTokenMaker.h"

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
