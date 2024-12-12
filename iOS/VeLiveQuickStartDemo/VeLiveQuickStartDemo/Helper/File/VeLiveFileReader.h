/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  VeLiveFileReader.h
//
//  Created by Volcano Engine Team on 2024/11/21.
//

#import <Foundation/Foundation.h>
#import <CoreMedia/CoreMedia.h>
#import "VeLiveFileConfig.h"
NS_ASSUME_NONNULL_BEGIN
///  数据回调  
typedef void (^VELFileDataBlock)(NSData *_Nullable data, CMTime pts);
///  文件读取结束会掉  
typedef void (^FLEFileReadCompletionBlock)(NSError *_Nullable error, BOOL isEnd);
@interface VeLiveFileReader : NSObject
///  是否重复读取文件，  
@property (atomic, assign) BOOL repeat;

///  创建裸数据文件读取工具  
///  - Parameter config: 配置  
+ (instancetype)readerWithConfig:(__kindof VeLiveFileConfig *)config;

///  开始读取文件  
- (void)startWithDataCallBack:(VELFileDataBlock)dataCallBack completion:(FLEFileReadCompletionBlock)completion;

///  停止读取文件  
- (void)stop;

///  暂停  
- (void)pause;

///  恢复  
- (void)resume;
@end

NS_ASSUME_NONNULL_END
