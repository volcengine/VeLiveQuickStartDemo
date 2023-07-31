/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VELiveConfig.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/3/2.
//

#import "VeLiveConfig.h"

@implementation VeLiveConfig

- (instancetype)init {
    if (self = [super init]) {
        //  以下默认值，仅供参考  
        self.captureWidth = 720;
        self.captureHeight = 1280;
        self.captureFps = 15;
        self.audioCaptureSampleRate = 44100;
        self.audioCaptureChannel = 2;
        
        self.videoEncoderWith = 720;
        self.videoEncoderHeight = 1280;
        self.videoEncoderFps = 15;
        
        self.videoEncoderKBitrate = 1600;
        self.videoHardwareEncoder = YES;
        self.audioEncoderSampleRate = 44100;
        self.audioEncoderChannel = 2;
        self.audioEncoderKBitrate = 64;
    }
    return self;
}

@end
