/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLSignTool.m
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/04/22.
//
//  Copyright (c) 2024/04/22 Beijing Volcano Engine Technology Ltd.
//
//

#import "VeLiveURLSignTool.h"
#import <CommonCrypto/CommonCrypto.h>
@interface VeLiveURLSignTool ()
@property (nonatomic, copy) NSString *host;
@property (nonatomic, strong) NSDateFormatter *dateFormatter;
@property (nonatomic, copy) NSString *signHeader;
@end
@implementation VeLiveURLSignTool
- (instancetype)init {
    if (self = [super init]) {
        [self setupDefaultValues];
    }
    return self;
}

- (instancetype)initWithAccessKey:(NSString *)accessKey secretKey:(NSString *)secretKey {
    if (self = [super init]) {
        self.accessKey = accessKey;
        self.secretKey = secretKey;
        [self setupDefaultValues];
    }
    return self;
}

- (void)setupDefaultValues {
    self.baseUrl = @"https://live.volcengineapi.com";
    self.region = @"cn-north-1";
    self.version = @"2023-01-01";
    self.relativePath = @"/";
    self.service = @"live";
    self.method = @"POST";
    self.contentType = @"application/x-www-form-urlencoded; charset=utf-8";
    self.signHeader = @"content-type;host;x-content-sha256;x-date";
}


- (void)setBaseUrl:(NSString *)baseUrl {
    _baseUrl = baseUrl.copy;
    if (_baseUrl != nil) {
        self.host = [NSURL URLWithString:baseUrl].host;
    }
}

- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action {
    return [self signRequestWithAction:action query:nil body:nil];
}

- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action query:(NSDictionary *)query {
    return [self signRequestWithAction:action query:query body:nil];
}

- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action body:(NSDictionary *)body {
    return [self signRequestWithAction:action query:nil body:body];
}

- (NSMutableURLRequest *)signRequestWithAction:(NSString *)action query:(NSDictionary *)query body:(NSDictionary *)body {
    return [self signRequestWithMethod:nil action:action query:query body:body];
}

- (NSMutableURLRequest *)signRequestWithMethod:(NSString *)method action:(NSString *)action {
    return [self signRequestWithMethod:method action:action query:@{} body:@{}];
}

- (NSMutableURLRequest *)signRequestWithMethod:(NSString *)method action:(NSString *)action query:(NSDictionary *)query {
    return [self signRequestWithMethod:method action:action query:query body:@{}];
}

- (NSMutableURLRequest *)signRequestWithMethod:(NSString *)method action:(NSString *)action body:(NSDictionary *)body {
    return [self signRequestWithMethod:method action:action query:@{} body:body];
}

- (NSMutableURLRequest *)signRequestWithMethod:(NSString *)method action:(NSString *)action query:(NSDictionary *)query body:(NSDictionary *)body {
    if (method != nil) {
        self.method = method;
    }
    self.action = action;
    if (query != nil && query.count > 0) {
        self.queryParams = query;
    }
    if (body != nil && body.count > 0) {
        self.body = body;
    }
    return [self signReuqest];
}


- (NSMutableURLRequest *)signReuqest {
    // 构建请求
    NSMutableDictionary *queryParams = [NSMutableDictionary dictionaryWithDictionary:self.queryParams ?:@{}];
    NSMutableDictionary *bodyDict = [NSMutableDictionary dictionaryWithDictionary:self.body ?:@{}];
    if ([self.queryParams objectForKey:@"Version"] == nil) {
        [queryParams setObject:self.version forKey:@"Version"];
    }
    [queryParams setObject:self.action forKey:@"Action"];
    
    if ([bodyDict objectForKey:@"Version"] != nil) {
        [queryParams setObject:[bodyDict objectForKey:@"Version"] forKey:@"Version"];
        [bodyDict removeObjectForKey:@"Version"];
    }
    
    NSString *queryStr = [self getQueryEncodeParams:queryParams sortKeys:YES urlEncode:YES];
    NSString *contentSha256 = [self hashSha256StringWithString:@""];
    NSData *bodyData = nil;
    if (bodyDict.count > 0) {
        bodyData = [NSJSONSerialization dataWithJSONObject:bodyDict options:0 error:nil];
        contentSha256 = [self hashSha256StringWithData:bodyData];
    }
    
    NSString *dateStr = [self.dateFormatter stringFromDate:NSDate.date];
    NSString *shortDateStr = [dateStr substringWithRange:NSMakeRange(0, 8)];
    NSMutableString *canonicalString = [[NSMutableString alloc] init];
    [canonicalString appendFormat:@"%@\n", self.method];
    [canonicalString appendFormat:@"%@\n", self.relativePath];
    [canonicalString appendFormat:@"%@\n", queryStr];
    [canonicalString appendFormat:@"content-type:%@\n", self.contentType];
    [canonicalString appendFormat:@"host:%@\n", self.host];
    [canonicalString appendFormat:@"x-content-sha256:%@\n", contentSha256];
    [canonicalString appendFormat:@"x-date:%@\n", dateStr];
    [canonicalString appendString:@"\n"];
    [canonicalString appendFormat:@"%@\n", self.signHeader];
    [canonicalString appendFormat:@"%@", contentSha256];
    NSString *hashcanonicalString = [self hashSha256StringWithString:canonicalString];
    NSString *credentialScope = [NSString stringWithFormat:@"%@/%@/%@/request", shortDateStr, self.region, self.service];
    NSString *signString = [NSString stringWithFormat:@"HMAC-SHA256\n%@\n%@\n%@", dateStr, credentialScope, hashcanonicalString];
    NSData *signKeyData = [self getSignKeyWithDate:shortDateStr];
    NSString *signature = [self hmacSha256StringWithString:signString keyData:signKeyData];
    
    NSString *authorization = [NSString stringWithFormat:@"HMAC-SHA256 Credential=%@/%@, SignedHeaders=%@, Signature=%@", self.accessKey, credentialScope, self.signHeader, signature];
    
    NSString *urlString = [NSString stringWithFormat:@"%@%@?%@", self.baseUrl, self.relativePath, queryStr];
    NSMutableURLRequest *urlRequest = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:urlString]];
    [urlRequest setHTTPMethod:self.method];
    [urlRequest setValue:self.host forHTTPHeaderField:@"Host"];
    [urlRequest setValue:dateStr forHTTPHeaderField:@"X-Date"];
    [urlRequest setValue:contentSha256 forHTTPHeaderField:@"X-Content-Sha256"];
    [urlRequest setValue:self.contentType forHTTPHeaderField:@"Content-Type"];
    [urlRequest setValue:authorization forHTTPHeaderField:@"Authorization"];
    if (![self.method isEqualToString:@"GET"] && bodyData != nil) {
        [urlRequest setHTTPBody:bodyData];
    }
    return urlRequest;
}

- (NSData *)getSignKeyWithDate:(NSString *)date {
    NSData *kData = [self hmacSha256DataWithString:date key:self.secretKey];
    NSData *kRegion = [self hmacSha256DataWithString:self.region keyData:kData];;
    NSData *kService = [self hmacSha256DataWithString:self.service keyData:kRegion];
    NSData *kSign = [self hmacSha256DataWithString:@"request" keyData:kService];
    return kSign;
}

- (NSString *)hashSha256StringWithString:(NSString *)string {
    return [self hashSha256StringWithData:[string dataUsingEncoding:NSUTF8StringEncoding]];
}

- (NSString *)hashSha256StringWithData:(NSData *)data {
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(data.bytes, (CC_LONG)data.length, result);
    NSMutableString *hash = [NSMutableString
                             stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++) {
        [hash appendFormat:@"%02x", result[i]];
    }
    return hash;
}

- (NSData *)hmacSha256DataWithString:(NSString *)string key:(NSString *)key {
    NSData *keyData = [key dataUsingEncoding:NSUTF8StringEncoding];
    return [self hmacSha256DataWithString:string keyData:keyData];
}

- (NSData *)hmacSha256DataWithString:(NSString *)string keyData:(NSData *)keyData {
    NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    const char *cKey = (const char *)keyData.bytes;
    CCHmac(kCCHmacAlgSHA256, cKey, keyData.length, data.bytes, data.length, result);
    return [[NSData alloc] initWithBytes:result length:CC_SHA256_DIGEST_LENGTH];
}

- (NSString *)hmacSha256StringWithString:(NSString *)string keyData:(NSData *)keyData {
    NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    const char *cKey = (const char *)keyData.bytes;
    CCHmac(kCCHmacAlgSHA256, cKey, keyData.length, data.bytes, data.length, result);
    NSMutableString *hash = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++) {
        [hash appendFormat:@"%02x", result[i]];
    }
    return hash;
}

- (NSDateFormatter *)dateFormatter {
    if (!_dateFormatter) {
        _dateFormatter = [[NSDateFormatter alloc] init];
        _dateFormatter.dateFormat = @"yyyyMMdd'T'HHmmss'Z'";
        _dateFormatter.timeZone = [NSTimeZone timeZoneWithName:@"GMT"];
    }
    return _dateFormatter;
}

- (NSString *)getQueryEncodeParams:(NSDictionary *)params sortKeys:(BOOL)sortKeys urlEncode:(BOOL)urlEncode {
    NSMutableArray *queryValues = [NSMutableArray arrayWithCapacity:params.count];
    NSArray *allKeys = params.allKeys;
    if (sortKeys) {
        allKeys = [allKeys sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {
            return [obj1 compare:obj2 options:NSNumericSearch];
        }];
    }
    for (NSObject *key in allKeys) {
        NSObject *value = [params objectForKey:key];
        if (urlEncode) {
            if ([value isKindOfClass:NSNull.class] || value == NSNull.null) {
                [queryValues addObject:[self urlEncodeString:key.description]];
            } else {
                [queryValues addObject:[NSString stringWithFormat:@"%@=%@", [self urlEncodeString:key.description], [self urlEncodeString:value.description]]];
            }
        } else {
            if ([value isKindOfClass:NSNull.class] || value == NSNull.null) {
                [queryValues addObject:key.description];
            } else {
                [queryValues addObject:[NSString stringWithFormat:@"%@=%@", key.description, value.description]];
            }
        }
    }
    return [queryValues componentsJoinedByString:@"&"];
}

- (NSString *)urlEncodeString:(NSString *)string {
    static NSString * const kAFCharactersGeneralDelimitersToEncode = @":#[]@";
    static NSString * const kAFCharactersSubDelimitersToEncode = @"!$&'()*+,;=";
    NSMutableCharacterSet * allowedCharacterSet = [[NSCharacterSet URLQueryAllowedCharacterSet] mutableCopy];
    [allowedCharacterSet removeCharactersInString:[kAFCharactersGeneralDelimitersToEncode stringByAppendingString:kAFCharactersSubDelimitersToEncode]];
    static NSUInteger const batchSize = 50;
    NSUInteger index = 0;
    NSMutableString *escaped = @"".mutableCopy;
    while (index < string.length) {
        NSUInteger length = MIN(string.length - index, batchSize);
        NSRange range = NSMakeRange(index, length);
        range = [string rangeOfComposedCharacterSequencesForRange:range];
        NSString *substring = [string substringWithRange:range];
        NSString *encoded = [substring stringByAddingPercentEncodingWithAllowedCharacters:allowedCharacterSet];
        [escaped appendString:encoded];
        index += range.length;
    }
    return escaped;
}
@end
