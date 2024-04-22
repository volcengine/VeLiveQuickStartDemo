/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLRequestHelper.h
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/04/22.
//
//  Copyright (c) 2024/04/22 Beijing Volcano Engine Technology Ltd.
//
//

#import <Foundation/Foundation.h>
#import "VeLiveURLModel.h"
NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, VeLiveURLSceneType) {
    VeLiveURLSceneTypePull,
    VeLiveURLSceneTypePush
};
/**
不要在生产环境使用，生产环境的推拉流地址请在服务端生成
*/
@interface VeLiveURLGenerator : NSObject

/**
配置 URL 生成器
https://www.volcengine.com/docs/6291/65568 获取
- Parameters:
  - accessKey: ak
  - secretKey: sk
*/
+ (void)setupWithAccessKey:(NSString *)accessKey secretKey:(NSString *)secretKey;

/**
配置域名空间，推拉流域名
https://console.volcengine.com/live/main/domain/list
- Parameters:
  - vHost: 域名空间
  - pushDomain: 推流域名
  - pullDomain: 拉流域名
*/
+ (void)setupVhost:(NSString *)vHost pushDomain:(NSString *)pushDomain pullDomain:(NSString *)pullDomain;

/**
生成推流地址
- Parameters:
  - app: app 名称
  - streamName: 流名
  - sceneType: 推流/拉流
  - completion: 回调
*/
+ (void)genPushURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel <VeLivePushURLModel *>*_Nullable model, NSError *_Nullable error))completion;

/**
生成拉流地址
- Parameters:
  - app: app 名称
  - streamName: 流名
  - completion: 回调
*/
+ (void)genPullURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel <VeLivePullURLModel *>*_Nullable model, NSError *_Nullable error))completion;

@end

NS_ASSUME_NONNULL_END
