/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveFileConfig.m
//
//  Created by Volcano Engine Team on 2024/11/21.
//

#import "VeLiveFileConfig.h"

@implementation VeLiveFileConfig
- (NSTimeInterval)interval {
    return 1;
}

- (int)packetSize {
    return 0;
}

- (BOOL)isValid {
    return (VEL_IS_NOT_EMPTY_STRING(self.path) && [NSFileManager.defaultManager fileExistsAtPath:self.path]);
}
@end

@implementation VELVideoFileConfig
- (instancetype)init {
    if (self = [super init]) {
        _fps = 25;
        _width = 640;
        _height = 360;
        _fileType = VELVideoFileType_UnKnown;
    }
    return self;
}
- (NSTimeInterval)interval {
    return 1.0 / MAX(self.fps, 1);
}

- (int)packetSize {
    if (self.fileType == VELVideoFileType_BGRA) {
        return self.width * self.height * 4;
    }
    return self.width * self.height * 3 / 2;
}
- (NSString *)fileTypeDes {
    switch (self.fileType) {
        case VELVideoFileType_UnKnown : return @"UnKnown";
        case VELVideoFileType_BGRA : return @"bgra";
        case VELVideoFileType_NV12 : return @"nv12";
        case VELVideoFileType_NV21 : return @"nv21";
        case VELVideoFileType_YUV : return @"yuv420";
    }
    return @"UnKnown";
}
- (BOOL)isValid {
    return [super isValid] && self.fileType != VELVideoFileType_UnKnown;
}
@end

@implementation VELAudioFileConfig
- (instancetype)init {
    if (self = [super init]) {
        _readCountPerSecond = 100;
        _sampleRate = 44100;
        _bitDepth = 16;
        _channels = 2;
        _fileType = VELAudioFileType_UnKnown;
        _playable = YES;
    }
    return self;
}
- (NSTimeInterval)interval {
    return 1.0 / _readCountPerSecond;
}

- (int)packetSize {
    return self.sampleRate * (self.bitDepth / 8.0) * self.channels / _readCountPerSecond;
}

- (BOOL)isValid {
    return [super isValid] && self.fileType != VELAudioFileType_UnKnown;
}
@end

