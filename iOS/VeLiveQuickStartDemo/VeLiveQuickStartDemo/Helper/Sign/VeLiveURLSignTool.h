/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLSignTool.h
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/07/30.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN
/**
不要在生产环境使用，生产环境的推拉流地址请在服务端生成
*/
@interface VeLiveURLSignTool : NSObject
@property (nonatomic, copy) NSString *baseUrl;
@property (nonatomic, copy) NSString *relativePath;
@property (nonatomic, copy) NSString *region;
@property (nonatomic, copy) NSString *version;
@property (nonatomic, copy) NSString *service;
@property (nonatomic, copy) NSString *method;
@property (nonatomic, copy) NSString *contentType;
@property (nonatomic, copy) NSString *accessKey;
@property (nonatomic, copy) NSString *secretKey;
@property (nonatomic, strong) NSDictionary *queryParams;
@property (nonatomic, strong) NSDictionary *body;
@property (nonatomic, copy) NSString *action;
- (instancetype)initWithAccessKey:(NSString *)accessKey secretKey:(NSString *)secretKey;
- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action;
- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action query:(nullable NSDictionary *)query;
- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action query:(nullable NSDictionary *)query body:(nullable NSDictionary *)body;
- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action body:(nullable NSDictionary *)body;
- (NSMutableURLRequest *)signRequestWithMethod:(nullable NSString *)method action:(NSString *)action;
- (NSMutableURLRequest *)signRequestWithMethod:(nullable NSString *)method action:(NSString *)action query:(nullable NSDictionary *)query;
- (NSMutableURLRequest *)signRequestWithMethod:(nullable NSString *)method action:(NSString *)action query:(nullable NSDictionary *)query body:(nullable NSDictionary *)body;
- (NSMutableURLRequest *)signRequestWithMethod:(nullable NSString *)method action:(NSString *)action body:(nullable NSDictionary *)body;
- (NSMutableURLRequest *)signReuqest;
@end

NS_ASSUME_NONNULL_END
