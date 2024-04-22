/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveRTCTokenMaker.mm
// VeLiveSolution
//
//  Created by Volcano Engine Team on 2024/04/22.
//
//  Copyright (c) 2024/04/22 Beijing Volcano Engine Technology Ltd.
//
//

#import "VeLiveRTCTokenMaker.h"
#import <CommonCrypto/CommonCrypto.h>
#import "VeLiveSDKHelper.h"
typedef NS_ENUM(NSInteger, VeLiveRTCPrivilege) {
    VeLiveRTCPrivilegePublishStream = 0,
    VeLiveRTCPrivilegePublishAudioStream = 1,
    VeLiveRTCPrivilegePublishVideoStream = 2,
    VeLiveRTCPrivilegePublishDataStream = 3,
    VeLiveRTCPrivilegeSubscribeStream = 4,
};
@interface VeLiveRTCAccessToken : NSObject
@property (nonatomic, copy) NSString *appID;
@property (nonatomic, copy) NSString *appKey;
@property (nonatomic, copy) NSString *roomID;
@property (nonatomic, copy) NSString *userID;
+ (instancetype)tokenWith:(NSString *)appID appKey:(NSString *)appKey;
- (void)addPrivilege:(VeLiveRTCPrivilege)privilege expireTime:(NSInteger)expireTime;
- (NSString *)serialize;
- (void)expireTime:(int)expireTime;
+ (VeLiveRTCAccessToken *)parse:(NSString *)token;
- (BOOL)verify:(NSString *)key;
@end

@interface VeLiveRTCTokenMaker ()
@property (nonatomic, copy) NSString *appId;
@property (nonatomic, copy) NSString *appKey;
@end
@implementation VeLiveRTCTokenMaker
+ (instancetype)shareMaker {
    static dispatch_once_t onceToken;
    static VeLiveRTCTokenMaker *instance = nil;
    dispatch_once(&onceToken, ^{
        instance = [[VeLiveRTCTokenMaker alloc] init];
        [instance setupWithAppID:RTC_APPID appKey:RTC_APPKEY];
    });
    return instance;
}

- (void)setupWithAppID:(NSString *)appid appKey:(NSString *)appKey {
    self.appId = appid;
    self.appKey = appKey;
}

- (NSString *)genDefaultTokenWithRoomID:(NSString *)roomId userId:(NSString *)userId {
    VeLiveRTCAccessToken *token = [VeLiveRTCAccessToken tokenWith:self.appId appKey:self.appKey];
    token.roomID = roomId;
    token.userID = userId;
    [token addPrivilege:(VeLiveRTCPrivilegePublishStream) expireTime:3600];
    [token addPrivilege:(VeLiveRTCPrivilegeSubscribeStream) expireTime:3600];
    return [token serialize];
}
@end

static int VERSION_LENGTH = 3;
static int APP_ID_LENGTH = 24;
@interface VeLiveRTCTokenUtil : NSObject
+ (int)getTimestamp;
+ (int)randomInt;
+ (NSData *)base64Decode:(NSString *)string;
+ (NSString *)base64Encode:(NSData *)data;
+ (NSData *)hmacSign:(NSString *)key msg:(NSData *)msg;
@end

@interface VeLiveRTCBuffer : NSObject
@property (nonatomic, strong) NSMutableData *data;
@property (nonatomic, assign) int position;
- (instancetype)initWithData:(NSData *)data;
- (void)appendInt:(int)value;
- (void)appendShort:(short)value;
- (void)appendInteger:(NSInteger)value;
- (void)appendString:(NSString *)str;
- (void)appendData:(NSData *)data;
- (void)appendStringDictionary:(NSDictionary <NSNumber *, NSString *>*)dict;
- (void)appendIntDictionary:(NSDictionary <NSNumber *, NSNumber *>*)dict;
- (int)readInt;
- (short)readShort;
- (NSInteger)readInteger;
- (NSString *)readString;
- (NSData *)readData;
- (NSDictionary <NSNumber *, NSString *>*)readStringDictionary;
- (NSDictionary <NSNumber *, NSNumber *>*)readIntDictionary;
@end

@interface VeLiveRTCAccessToken ()
@property (nonatomic, strong) NSMutableDictionary <NSNumber *, NSNumber *>*privileges;
@property (nonatomic, assign) int issuedAt;
@property (nonatomic, assign) int expireAt;
@property (nonatomic, assign) int nonce;
@property (nonatomic, copy) NSString *version;
@property (nonatomic, strong) NSData *signature;
@end
@implementation VeLiveRTCAccessToken
- (instancetype)init {
    if (self = [super init]) {
        self.issuedAt = [VeLiveRTCTokenUtil getTimestamp];
        self.nonce = [VeLiveRTCTokenUtil randomInt];
        self.privileges = [[NSMutableDictionary alloc] init];
        self.version = @"001";
    }
    return self;
}

+ (instancetype)tokenWith:(NSString *)appID appKey:(NSString *)appKey {
    VeLiveRTCAccessToken *token = [[VeLiveRTCAccessToken alloc] init];
    token.appID = appID;
    token.appKey = appKey;
    return token;
}

- (void)addPrivilege:(VeLiveRTCPrivilege)privilege expireTime:(NSInteger)expireTime {
    NSInteger expireTimestamp = [VeLiveRTCTokenUtil getTimestamp] + expireTime;
    [self.privileges setObject:@(expireTimestamp) forKey:@(privilege)];
    if (privilege == VeLiveRTCPrivilegePublishStream) {
        [self.privileges setObject:@(expireTimestamp) forKey:@(VeLiveRTCPrivilegePublishAudioStream)];
        [self.privileges setObject:@(expireTimestamp) forKey:@(VeLiveRTCPrivilegePublishVideoStream)];
        [self.privileges setObject:@(expireTimestamp) forKey:@(VeLiveRTCPrivilegePublishDataStream)];
    }
}

- (VeLiveRTCBuffer *)packMsg {
    VeLiveRTCBuffer *buffer = [[VeLiveRTCBuffer alloc] init];
    [buffer appendInt:(int)self.nonce];
    [buffer appendInt:(int)self.issuedAt];
    [buffer appendInt:(int)self.expireAt];
    [buffer appendString:self.roomID];
    [buffer appendString:self.userID];
    [buffer appendIntDictionary:self.privileges];
    return buffer;
}

- (void)expireTime:(int)expireTime {
    self.expireAt = expireTime;
}

- (NSString *)serialize {
    VeLiveRTCBuffer *outputBuffer = [[VeLiveRTCBuffer alloc] init];
    VeLiveRTCBuffer *buffer = [self packMsg];
    self.signature = [VeLiveRTCTokenUtil hmacSign:self.appKey msg:buffer.data];
    [outputBuffer appendData:buffer.data];
    [outputBuffer appendData:self.signature];
    return [NSString stringWithFormat:@"%@%@%@", self.version, self.appID, [VeLiveRTCTokenUtil base64Encode:outputBuffer.data]];
}

+ (VeLiveRTCAccessToken *)parse:(NSString *)token {
    VeLiveRTCAccessToken *accessToken = [[VeLiveRTCAccessToken alloc] init];
    if (token.length <= VERSION_LENGTH + APP_ID_LENGTH) {
        return accessToken;
    }
    if (![accessToken.version isEqualToString:[token substringToIndex:VERSION_LENGTH]]) {
        return accessToken;
    }
    accessToken.appID = [token substringWithRange:NSMakeRange(VERSION_LENGTH, APP_ID_LENGTH)];
    NSString *contentBase64 = [token substringFromIndex:VERSION_LENGTH + APP_ID_LENGTH];
    NSData *content = [VeLiveRTCTokenUtil base64Decode:contentBase64];
    VeLiveRTCBuffer *buffer = [[VeLiveRTCBuffer alloc] initWithData:content];
    NSData *msgData = [buffer readData];
    accessToken.signature = [buffer readData];
    
    VeLiveRTCBuffer *msgBuffer = [[VeLiveRTCBuffer alloc] initWithData:msgData];
    accessToken.nonce = [msgBuffer readInt];
    accessToken.issuedAt = [msgBuffer readInt];
    accessToken.expireAt = [msgBuffer readInt];
    accessToken.roomID = [msgBuffer readString];
    accessToken.userID = [msgBuffer readString];
    accessToken.privileges = [msgBuffer readIntDictionary].mutableCopy;
    return accessToken;
}

- (BOOL)verify:(NSString *)key {
    if (self.expireAt > 0 && VeLiveRTCTokenUtil.getTimestamp > self.expireAt) {
        return NO;
    }
    self.appKey = key;
    NSData *signature = [VeLiveRTCTokenUtil hmacSign:self.appKey msg:[self packMsg].data];
    return [signature isEqualToData:self.signature];
}
@end

@implementation VeLiveRTCTokenUtil
+ (int)getTimestamp {
    return (int)(CFAbsoluteTimeGetCurrent() + kCFAbsoluteTimeIntervalSince1970);
}

+ (int)randomInt {
    return (int)arc4random_uniform(UINT32_MAX - 1);
}

+ (NSString *)base64Encode:(NSData *)data {
    return [data base64EncodedStringWithOptions:0];
}

+ (NSData *)base64Decode:(NSString *)base64EncodedString {
    return [[NSData alloc] initWithBase64EncodedString:base64EncodedString options:0];
}

+ (NSData *)hmacSign:(NSString *)key msg:(NSData *)msg {
    NSData *keyData = [key dataUsingEncoding:NSUTF8StringEncoding];
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    const char *cKey = (const char *)keyData.bytes;
    CCHmac(kCCHmacAlgSHA256, cKey, keyData.length, msg.bytes, msg.length, result);
    return [[NSData alloc] initWithBytes:result length:CC_SHA256_DIGEST_LENGTH];
}
@end

@implementation VeLiveRTCBuffer
- (instancetype)init {
    if (self = [super init]) {
        self.data = [[NSMutableData alloc] init];
        self.position = -1;
    }
    return self;
}
- (instancetype)initWithData:(NSData *)data {
    if (self = [super init]) {
        self.data = [[NSMutableData alloc] initWithData:data];
        self.position = 0;
    }
    return self;
}

- (void)appendInt:(int)value {
    [self.data appendBytes:&value length:sizeof(int)];
    self.position += sizeof(int);
}

- (void)appendShort:(short)value {
    [self.data appendBytes:&value length:sizeof(short)];
    self.position += sizeof(short);
}

- (void)appendInteger:(NSInteger)value {
    [self.data appendBytes:&value length:sizeof(long)];
    self.position += sizeof(long);
}

- (void)appendString:(NSString *)str {
    [self appendData:[str dataUsingEncoding:NSUTF8StringEncoding]];
}

- (void)appendData:(NSData *)data {
    [self appendShort:data.length];
    [self.data appendData:data];
    self.position += (int)data.length;
}

- (void)appendStringDictionary:(NSDictionary <NSNumber *, NSString *>*)dict {
    [self appendShort:(short)dict.count];
    NSArray <NSNumber *>* sortedKeys = [dict.allKeys sortedArrayUsingSelector:@selector(compare:)];
    [sortedKeys enumerateObjectsUsingBlock:^(NSNumber * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self appendShort:obj.shortValue];
        [self appendString:dict[obj]];
    }];
}

- (void)appendIntDictionary:(NSDictionary <NSNumber *, NSNumber *>*)dict {
    [self appendShort:(short)dict.count];
    NSArray <NSNumber *>* sortedKeys = [dict.allKeys sortedArrayUsingSelector:@selector(compare:)];
    [sortedKeys enumerateObjectsUsingBlock:^(NSNumber * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        [self appendShort:obj.shortValue];
        [self appendInt:dict[obj].intValue];
    }];
}

- (int)readInt {
    int value = 0;
    [self.data getBytes:&value range:NSMakeRange(self.position, sizeof(int))];
    self.position += sizeof(int);
    return value;
}

- (short)readShort {
    short value = 0;
    [self.data getBytes:&value range:NSMakeRange(self.position, sizeof(short))];
    self.position += sizeof(short);
    return value;
}

- (NSInteger)readInteger {
    NSInteger value = 0;
    [self.data getBytes:&value range:NSMakeRange(self.position, sizeof(long))];
    self.position += sizeof(long);
    return value;
}

- (NSString *)readString {
    NSData *data = [self readData];
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSData *)readData {
    short dataLen = [self readShort];
    NSData *data = [self.data subdataWithRange:NSMakeRange(self.position, dataLen)];
    self.position += dataLen;
    return data;
}

- (NSDictionary <NSNumber *, NSString *>*)readStringDictionary {
    short dictCount = [self readShort];
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    for (int i = 0; i < dictCount; i++) {
        short key = [self readShort];
        NSString *v = [self readString];
        [dict setObject:v forKey:@(key)];
    }
    return dict.copy;
}

- (NSDictionary <NSNumber *, NSNumber *>*)readIntDictionary {
    short dictCount = [self readShort];
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    for (int i = 0; i < dictCount; i++) {
        short key = [self readShort];
        NSInteger v = [self readInt];
        [dict setObject:@(v) forKey:@(key)];
    }
    return dict.copy;
}
@end
