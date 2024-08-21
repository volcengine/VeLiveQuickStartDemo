/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveURLRequestHelper.m
// VeLiveLiveDemo
// 
//  Created by Volcano Engine Team on 2024/07/30.
//

#import "VeLiveURLGenerator.h"
#import "VeLiveURLSignTool.h"
#import "VeLiveURLModel.h"
#import "VeLiveSDKHelper.h"
@interface VeLiveURLGenerator ()
@property (nonatomic, copy) NSString *accessKey;
@property (nonatomic, copy) NSString *secretKey;
@property (nonatomic, copy) NSString *vHost;
@property (nonatomic, copy) NSString *pushDomain;
@property (nonatomic, copy) NSString *pullDomain;
@property (nonatomic, strong) VeLiveURLSignTool *defaultSignTool;
@end
@implementation VeLiveURLGenerator
+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    static VeLiveURLGenerator *instance = nil;
    dispatch_once(&onceToken, ^{
        instance = [[VeLiveURLGenerator alloc] init];
    });
    return instance;
}

- (instancetype)init {
    if (self = [super init]) {
        [self setupDefaultValues];
    }
    return self;
}

+ (void)setupWithAccessKey:(NSString *)accessKey secretKey:(NSString *)secretKey {
    [[self sharedInstance] setupWithAccessKey:accessKey secretKey:secretKey];
}

+ (void)setupVhost:(NSString *)vHost pushDomain:(NSString *)pushDomain pullDomain:(NSString *)pullDomain {
    [[self sharedInstance] setupVhost:vHost pushDomain:pushDomain pullDomain:pullDomain];
}

+ (BOOL)isValid {
    return [[self sharedInstance] isValid];
}

+ (void)genPushURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel <VeLivePushURLModel *>*_Nullable model, NSError *_Nullable error))completion {
    [[self sharedInstance] genPushURLForApp:app streamName:streamName completion:completion];
}

+ (void)genPullURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel <VeLivePullURLModel *>*_Nullable model, NSError *_Nullable error))completion {
    [[self sharedInstance] genPullURLForApp:app streamName:streamName completion:completion];
}

+ (void)updateSignForApp:(NSString *)app streamName:(NSString *)streamName enableSign:(BOOL)enableSign sceneType:(VeLiveURLSceneType)sceneType completion:(void (^)(VeLiveURLRootModel *_Nullable model, NSError *_Nullable error))completion {
    [[self sharedInstance] updateSignForApp:app streamName:streamName enableSign:enableSign sceneType:sceneType completion:completion];
}

- (void)setupWithAccessKey:(NSString *)accessKey secretKey:(NSString *)secretKey {
    self.accessKey = accessKey;
    self.secretKey = secretKey;
}

- (void)setupVhost:(NSString *)vHost pushDomain:(NSString *)pushDomain pullDomain:(NSString *)pullDomain {
    self.vHost = vHost;
    self.pushDomain = pushDomain;
    self.pullDomain = pullDomain;
}

- (BOOL)isValid {
    [self setupDefaultValues];
    return self.accessKey != nil
    && self.secretKey != nil
    && self.vHost != nil
    && self.pushDomain != nil
    && self.pullDomain != nil;
}

- (void)setupDefaultValues {
    if (self.accessKey == nil) {
        self.accessKey = ACCESS_KEY_ID;
    }
    if (self.secretKey == nil) {
        self.secretKey = SECRET_ACCESS_KEY;
    }
    if (self.vHost == nil) {
        self.vHost = LIVE_VHOST;
    }
    if (self.pushDomain == nil) {
        self.pushDomain = LIVE_PUSH_DOMAIN;
    }
    if (self.pullDomain == nil) {
        self.pullDomain = LIVE_PULL_DOMAIN;
    }
}

- (VeLiveURLSignTool *)defaultSignTool {
    if (!_defaultSignTool) {
        [self setupDefaultValues];
        _defaultSignTool = [[VeLiveURLSignTool alloc] initWithAccessKey:self.accessKey secretKey:self.secretKey];
    }
    return _defaultSignTool;
}

- (void)genPushURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel <VeLivePushURLModel *>*_Nullable model, NSError *_Nullable error))completion {
    if (![self isValid]) {
        if (completion) {
            completion(nil, [NSError errorWithDomain:NSURLErrorDomain code:-1 userInfo:@{NSLocalizedDescriptionKey : @"not valid"}]);
        }
        return;
    }
    NSURLRequest *request = [self.defaultSignTool signRequestWithAction:@"GeneratePushURL" body:@{
        @"Vhost" : self.vHost,
        @"Domain" : [self getDomainWithSceneType:(VeLiveURLSceneTypePush)],
        @"App" : [self getAppNameWith:app],
        @"Stream" : [self getStreamNameWith:streamName],
    }];
    [self sendRequest:request modelCreator:^id(NSData *data) {
        return [VeLiveURLRootModel modelWithData:data resultClass:VeLivePushURLModel.class];
    } completion:completion];
}

- (void)genPullURLForApp:(NSString *)app streamName:(NSString *)streamName completion:(void (^)(VeLiveURLRootModel<VeLivePullURLModel *> * _Nullable, NSError * _Nullable))completion {
    if (![self isValid]) {
        if (completion) {
            completion(nil, [NSError errorWithDomain:NSURLErrorDomain code:-1 userInfo:@{NSLocalizedDescriptionKey : @"not valid"}]);
        }
        return;
    }
    NSURLRequest *request = [self.defaultSignTool signRequestWithAction:@"GeneratePlayURL" body:@{
        @"Vhost" : self.vHost,
        @"Domain" : [self getDomainWithSceneType:(VeLiveURLSceneTypePull)],
        @"App" : [self getAppNameWith:app],
        @"Stream" : [self getStreamNameWith:streamName],
        @"Type" : @"fcdn",
    }];
    [self sendRequest:request modelCreator:^id(NSData *data) {
        return [VeLiveURLRootModel modelWithData:data resultClass:VeLivePullURLModel.class];
    } completion:completion];
}

- (void)updateSignForApp:(NSString *)app streamName:(NSString *)streamName enableSign:(BOOL)enableSign sceneType:(VeLiveURLSceneType)sceneType completion:(void (^)(VeLiveURLRootModel * _Nullable, NSError * _Nullable))completion {
    if (![self isValid]) {
        if (completion) {
            completion(nil, [NSError errorWithDomain:NSURLErrorDomain code:-1 userInfo:@{NSLocalizedDescriptionKey : @"not valid"}]);
        }
        return;
    }
    NSURLRequest *request = [self.defaultSignTool signRequestWithAction:@"UpdateAuthKey" body:@{
        @"Domain" : [self getDomainWithSceneType:(VeLiveURLSceneTypePull)],
        @"App" : [self getAppNameWith:app],
        @"SceneType" : [self getSceneTypeDescript:sceneType],
        @"AuthDetailList" : @{
            @"SecretKey" : @"U0zk2qtmhLzaKcnwNzvZJRds",
            @"EncryptionAlgorithm" : @"md5"
        },
        @"PushPullEnable" : @(enableSign),
    }];
    [self sendRequest:request modelCreator:^id(NSData *data) {
        return [VeLiveURLRootModel modelWithData:data resultClass:nil];
    } completion:completion];
}

- (void)sendRequest:(NSURLRequest *)request modelCreator:(id (^)(NSData *data))modelFilter completion:(void (^)(VeLiveURLRootModel *_Nullable model, NSError *_Nullable error))completion {
    [[NSURLSession.sharedSession dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        VeLiveURLRootModel * model = nil;
        if (data != nil) {
            model = modelFilter(data);
        }
        if (model == nil || model.result == nil || model.responseMetadata.error != nil) {
            error = [NSError errorWithDomain:NSURLErrorDomain code:model.responseMetadata.error.codeN.integerValue userInfo:@{
                NSLocalizedDescriptionKey : model.responseMetadata.error.message ?: model.responseMetadata.error.code ?: @"mode error"
            }];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (completion) {
                completion(model, error);
            }
        });
    }] resume];
}

/// MARK: - Private
- (NSString *)getStreamNameWith:(NSString *)streamName {
    if (streamName != nil && streamName.length > 0) {
        return streamName;
    }
    NSDictionary *info = NSBundle.mainBundle.infoDictionary;
    return [info objectForKey:@"CFBundleVersion"];
}

- (NSString *)getAppNameWith:(NSString *)appName {
    if (appName != nil && appName.length > 0) {
        return appName;
    }
    return @"live";
}

- (NSString *)getDomainWithSceneType:(VeLiveURLSceneType)sceneType {
    switch (sceneType) {
        case VeLiveURLSceneTypePull: return self.pullDomain;
        case VeLiveURLSceneTypePush: return self.pushDomain;
    }
    NSAssert(NO, @"not found scene");
    return nil;
}

- (NSString *)getSceneTypeDescript:(VeLiveURLSceneType)sceneType {
    switch (sceneType) {
        case VeLiveURLSceneTypePull: return @"pull";
        case VeLiveURLSceneTypePush: return @"push";
    }
    NSAssert(NO, @"not found scene");
    return nil;
}

@end
