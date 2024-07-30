/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLModel.m
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/07/30.
//
//  Copyright (c) 2024/07/30 Beijing Volcano Engine Technology Ltd.
//
//

#import "VeLiveURLModel.h"
@interface NSDictionary (VeLiveAddition)
@end
@implementation NSDictionary (VeLiveAddition)
- (nullable id)objectIngoreLeadUpperForKey:(id)aKey {
    id obj = [self objectForKey:aKey];
    if ([aKey isKindOfClass:NSString.class] && obj == nil) {
        obj = [self objectForKey:[aKey uppercaseString]];
        if (obj == nil) {
            obj = [self objectForKey:[aKey lowercaseString]];
        }
        if (obj == nil && [aKey length] > 1) {
            NSString *firstChar = [aKey substringToIndex:1];
            NSString *newKey = [aKey stringByReplacingCharactersInRange:NSMakeRange(0, 1) withString:firstChar.uppercaseString];
            obj = [self objectForKey:newKey];
            if (obj == nil) {
                newKey = [aKey stringByReplacingCharactersInRange:NSMakeRange(0, 1) withString:firstChar.lowercaseString];
                obj = [self objectForKey:newKey];
            }
        }
    }
    return obj;
}
- (nullable NSString *)stringValueForKey:(NSString *)key {
    id obj = [self objectIngoreLeadUpperForKey:key];
    if (obj == nil || [obj isKindOfClass:NSString.class]) {
        return obj;
    }
    return [NSString stringWithFormat:@"%@", obj];
}
- (nullable NSArray *)arrayValueForKey:(NSString *)key {
    id obj = [self objectIngoreLeadUpperForKey:key];
    if (obj == nil || [obj isKindOfClass:NSArray.class]) {
        return obj;
    }
    return nil;
}
- (nullable NSDictionary *)dictValueForKey:(NSString *)key {
    id obj = [self objectIngoreLeadUpperForKey:key];
    if (obj == nil || [obj isKindOfClass:NSDictionary.class]) {
        return obj;
    }
    return nil;
}
@end
@interface VeLiveURLRootModel ()
@end
@implementation VeLiveURLRootModel
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary resultClass:(Class<VeLiveModelProtocol>)resultCls {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLiveURLRootModel *model = [[VeLiveURLRootModel alloc] init];
    model.responseMetadata = [VeLiveURLResponseMetadata modelWithDictionary:[dictionary dictValueForKey:@"ResponseMetadata"]];
    model.result = [resultCls modelWithDictionary:[dictionary dictValueForKey:@"result"]];
    return model;
}

+ (instancetype)modelWithData:(id)data resultClass:(Class<VeLiveModelProtocol>)resultCls {
    if ([data isKindOfClass:NSDictionary.class]) {
        return [self modelWithDictionary:(NSDictionary *)data resultClass:resultCls];
    } else if ([data isKindOfClass:NSData.class]) {
        @try {
            id obj = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
            return [self modelWithData:obj resultClass:resultCls];
        } @catch (NSException *exception) {
            return nil;
        }
    } else if ([data isKindOfClass:NSString.class]) {
        return [self modelWithData:[(NSString *)data dataUsingEncoding:NSUTF8StringEncoding] resultClass:resultCls];
    }
    return nil;
}
@end

@implementation VeLiveURLError
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLiveURLError *error = [[VeLiveURLError alloc] init];
    error.code = [dictionary stringValueForKey:@"Code"];
    error.codeN = [dictionary stringValueForKey:@"CodeN"];
    error.message = [dictionary stringValueForKey:@"Message"];
    return error;
}
@end

@implementation VeLiveURLResponseMetadata
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLiveURLResponseMetadata *medaData = [[VeLiveURLResponseMetadata alloc] init];
    medaData.requestId = [dictionary stringValueForKey:@"RequestId"];
    medaData.action = [dictionary stringValueForKey:@"Action"];
    medaData.version = [dictionary stringValueForKey:@"Version"];
    medaData.service = [dictionary stringValueForKey:@"Service"];
    medaData.region = [dictionary stringValueForKey:@"Region"];
    medaData.error = [VeLiveURLError modelWithDictionary:[dictionary dictValueForKey:@"Error"]];
    return medaData;
}
@end

@implementation VeLivePushURLDetailModel
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLivePushURLDetailModel *urlDetail = [[VeLivePushURLDetailModel alloc] init];
    urlDetail.url = [dictionary stringValueForKey:@"URL"];
    urlDetail.domainApp = [dictionary stringValueForKey:@"DomainApp"];
    urlDetail.streamSign = [dictionary stringValueForKey:@"StreamSign"];
    return urlDetail;
}
@end

@implementation VeLivePushURLModel
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLivePushURLModel *urlModel = [[VeLivePushURLModel alloc] init];
    urlModel.pushUrlList = [dictionary arrayValueForKey:@"PushURLList"];
    NSMutableArray <VeLivePushURLDetailModel *>* pushUrlListDetail = @[].mutableCopy;
    [[dictionary arrayValueForKey:@"PushURLListDetail"] enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        VeLivePushURLDetailModel *model = [VeLivePushURLDetailModel modelWithDictionary:obj];
        if (model != nil) {
            [pushUrlListDetail addObject:model];
        }
    }];
    urlModel.pushUrlListDetail = pushUrlListDetail;;
    urlModel.tsOverSrtURLList = [dictionary arrayValueForKey:@"TsOverSrtURLList"];
    urlModel.rtmpOverSrtURLList = [dictionary arrayValueForKey:@"RtmpOverSrtURLList"];
    urlModel.rtmURLList = [dictionary arrayValueForKey:@"RtmURLList"];
    urlModel.webTransportURLList = [dictionary arrayValueForKey:@"WebTransportURLList"];
    return urlModel;
}
- (NSString *)getRtmpPushUrl {
    return [self getUrlForProtocol:@"rtmp" format:nil];
}
- (NSString *)getRtmPushUrl {
    return self.rtmURLList.firstObject;
}
- (NSString *)getUrlForProtocol:(NSString *)protocol format:(nullable NSString *)format {
    if (protocol == nil || protocol.length <= 1) {
        return nil;
    }
    __block NSString *url = nil;
    [self.pushUrlListDetail enumerateObjectsUsingBlock:^(VeLivePushURLDetailModel * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (obj.url != nil && obj.url.length > 5) {
            if ([obj.url hasPrefix:protocol]) {
                if (format != nil && format.length > 1) {
                    if ([[NSURL URLWithString:obj.url].path hasSuffix:format]) {
                        url = obj.url;
                        *stop = YES;
                    }
                } else {
                    url = obj.url;
                    *stop = YES;
                }
            }
        }
    }];
    return url;
}
@end

@implementation VeLivePullURLListModel
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLivePullURLListModel *urlListModel = [[VeLivePullURLListModel alloc] init];
    urlListModel.url = [dictionary stringValueForKey:@"URL"];
    urlListModel.type = [dictionary stringValueForKey:@"Type"];
    urlListModel.cdn = [dictionary stringValueForKey:@"CDN"];
    urlListModel.protocol = [dictionary stringValueForKey:@"Protocol"];
    return urlListModel;
}
@end

@implementation VeLivePullURLModel
+ (instancetype)modelWithDictionary:(NSDictionary *)dictionary {
    if (![dictionary isKindOfClass:NSDictionary.class]) {
        return nil;
    }
    VeLivePullURLModel *pullUrlModel = [[VeLivePullURLModel alloc] init];
    NSMutableArray <VeLivePullURLListModel *> *urlListModel = @[].mutableCopy;
    [[dictionary arrayValueForKey:@"URLList"] enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        VeLivePullURLListModel *model = [VeLivePullURLListModel modelWithDictionary:obj];
        if (model != nil) {
            [urlListModel addObject:model];
        }
    }];
    pullUrlModel.urlList = urlListModel;
    return pullUrlModel;
}

- (NSString *)getUrlWithProtocol:(NSString *)protocol {
    if (self.urlList.count == 0 || protocol == nil || protocol.length == 0) {
        return nil;
    }
    __block NSString *url = nil;
    [self.urlList enumerateObjectsUsingBlock:^(VeLivePullURLListModel * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([obj.protocol.lowercaseString isEqualToString:protocol.lowercaseString]) {
            url = obj.url;
            *stop = YES;
        }
    }];
    return url;
}
@end
