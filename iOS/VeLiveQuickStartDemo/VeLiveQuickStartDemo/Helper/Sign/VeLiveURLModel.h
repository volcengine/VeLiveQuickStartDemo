/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLModel.h
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/07/30.
//

#import <Foundation/Foundation.h>
NS_ASSUME_NONNULL_BEGIN
@protocol VeLiveModelProtocol <NSObject>
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary;
@end
@class VeLiveURLResponseMetadata;
@interface VeLiveURLRootModel <__covariant ObjectType> : NSObject
@property (nonatomic, strong) VeLiveURLResponseMetadata *responseMetadata;
@property (nonatomic, strong) ObjectType result;
+ (instancetype)modelWithData:(id)data resultClass:(nullable Class<VeLiveModelProtocol>)resultCls;
@end

@interface VeLiveURLError : NSObject <VeLiveModelProtocol>
@property (nonatomic, copy) NSString *code;
@property (nonatomic, copy) NSString *codeN;
@property (nonatomic, copy) NSString *message;
@end

@interface VeLiveURLResponseMetadata : NSObject <VeLiveModelProtocol>
@property (nonatomic, copy) NSString *requestId;
@property (nonatomic, copy) NSString *action;
@property (nonatomic, copy) NSString *version;
@property (nonatomic, copy) NSString *service;
@property (nonatomic, copy) NSString *region;
@property (nonatomic, strong) VeLiveURLError *error;
@end

@interface VeLivePushURLDetailModel : NSObject <VeLiveModelProtocol>
@property (nonatomic, copy) NSString *url;
@property (nonatomic, copy) NSString *domainApp;
@property (nonatomic, copy) NSString *streamSign;
@end

@interface VeLivePushURLModel : NSObject <VeLiveModelProtocol>
@property (nonatomic, strong) NSArray <NSString *> *pushUrlList;
@property (nonatomic, strong) NSArray <VeLivePushURLDetailModel *> *pushUrlListDetail;
@property (nonatomic, strong) NSArray <NSString *> *tsOverSrtURLList;
@property (nonatomic, strong) NSArray <NSString *> *rtmpOverSrtURLList;
@property (nonatomic, strong) NSArray <NSString *> *rtmURLList;
@property (nonatomic, strong) NSArray <NSString *> *webTransportURLList;
- (NSString *)getRtmpPushUrl;
- (NSString *)getRtmPushUrl;
@end


@interface VeLivePullURLListModel : NSObject <VeLiveModelProtocol>
@property (nonatomic, copy) NSString *url;
@property (nonatomic, copy) NSString *type;
@property (nonatomic, copy) NSString *cdn;
@property (nonatomic, copy) NSString *protocol;
@end

@interface VeLivePullURLModel : NSObject <VeLiveModelProtocol>
@property (nonatomic, strong) NSArray <VeLivePullURLListModel *> *urlList;
- (nullable NSString *)getUrlWithProtocol:(NSString *)protocol;
@end
NS_ASSUME_NONNULL_END
