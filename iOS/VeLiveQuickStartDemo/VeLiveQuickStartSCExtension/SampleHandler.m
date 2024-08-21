/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
//  SampleHandler.m
//  VeLiveQuickStartSCExtension
//
//  Created by Volcano Engine Team on 2024/8/5.
//


#import "SampleHandler.h"
#import <VeLiveReplayKitExtension/VeLiveReplayKitExtension.h>
@interface SampleHandler () <VeLiveReplayKitExtensionDelegate>
@end
@implementation SampleHandler

- (void)broadcastStartedWithSetupInfo:(NSDictionary<NSString *,NSObject *> *)setupInfo {
    // User has requested to start the broadcast. Setup info from the UI extension can be supplied but optional. 
#ifdef APP_SC_GROUP_ID
    [[VeLiveReplayKitExtension sharedInstance] startWithAppGroup:@APP_SC_GROUP_ID delegate:self];
#else
    [self finishBroadcastWithError:[NSError errorWithDomain:@"SampleHandler" code:-1 userInfo:@{NSLocalizedFailureReasonErrorKey:@"please config APP_SC_GROUP_ID in Common.xcconfig"}]];
    NSAssert(NO, @"please config APP_SC_GROUP_ID in Common.xcconfig");
#endif
}

- (void)broadcastPaused {
    // User has requested to pause the broadcast. Samples will stop being delivered.
    [[VeLiveReplayKitExtension sharedInstance] broadcastPaused];
}

- (void)broadcastResumed {
    // User has requested to resume the broadcast. Samples delivery will resume.
    [[VeLiveReplayKitExtension sharedInstance] broadcastResumed];
}

- (void)broadcastFinished {
    // User has requested to finish the broadcast.
    [[VeLiveReplayKitExtension sharedInstance] broadcastFinished];
}

- (void)processSampleBuffer:(CMSampleBufferRef)sampleBuffer withType:(RPSampleBufferType)sampleBufferType {
    [[VeLiveReplayKitExtension sharedInstance] processSampleBuffer:sampleBuffer withType:sampleBufferType];
}

// VeLiveReplayKitExtensionDelegate
- (void)broadcastFinished:(VeLiveReplayKitExtension *)broadcast reason:(VeLiveReplayKitExtensionReason)reason {
    NSString *tip = @"";
    switch (reason) {
        case VeLiveReplayKitExtensionReasonMainStop:
            tip = @"main app stops screen capture";
            break;
    }

    NSError *error = [NSError errorWithDomain:NSStringFromClass(self.class)
                                             code:0
                                         userInfo:@{
                                             NSLocalizedFailureReasonErrorKey:tip
                                         }];
    [self finishBroadcastWithError:error];

}
@end
