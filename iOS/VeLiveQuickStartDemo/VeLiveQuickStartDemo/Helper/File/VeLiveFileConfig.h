/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//
// VeLiveFileConfig.h
//
//  Created by Volcano Engine Team on 2024/11/21.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN


@interface VeLiveFileConfig : NSObject
///  每次读取数据间隔  
@property (nonatomic, assign, readonly) NSTimeInterval interval;
///  每次读取数据大小  
@property (nonatomic, assign, readonly) int packetSize;
///  文件路径  
@property (nonatomic, copy) NSString *path;
///  文件名  
@property (nonatomic, copy) NSString *name;

///  是否有效  
- (BOOL)isValid;

@end

///  视频文件类型  
typedef NS_ENUM(NSInteger, VELVideoFileType) {
    VELVideoFileType_UnKnown,
    VELVideoFileType_BGRA,
    VELVideoFileType_NV12,
    VELVideoFileType_NV21,
    VELVideoFileType_YUV
};

typedef NS_ENUM(NSInteger, VELVideoFileConvertType) {
    VELVideoFileConvertTypeUnKnown,
    VELVideoFileConvertTypeTextureID = 1,
    VELVideoFileConvertTypeEncodeData,
    VELVideoFileConvertTypePixelBuffer,
    VELVideoFileConvertTypeSampleBuffer,
};
@interface VELVideoFileConfig : VeLiveFileConfig
///  采集帧率，默认 25  
@property (nonatomic, assign) int fps;
///  视频宽度，默认640  
@property (nonatomic, assign) int width;
///  视频高度，默认360  
@property (nonatomic, assign) int height;
///  文件类型，默认VELVideoFileType_UnKnown  
@property (nonatomic, assign) VELVideoFileType fileType;
///  文件类型描述  
@property (nonatomic, copy, readonly) NSString *fileTypeDes;
///  需要转换的类型  
@property (nonatomic, assign) VELVideoFileConvertType convertType;
@end

///  音频文件类型  
typedef NS_ENUM(NSInteger, VELAudioFileType) {
    VELAudioFileType_UnKnown,
    VELAudioFileType_PCM,
};

@interface VELAudioFileConfig : VeLiveFileConfig
///  每秒读取多少次，默认 100  
@property (nonatomic, assign) int readCountPerSecond;
///  采样率, 默认：44100  
@property (nonatomic, assign) int sampleRate;
///  位深， 默认：16  
@property (nonatomic, assign) int bitDepth;
///  通道数，默认：2  
@property (nonatomic, assign) int channels;
///  文件类型，当前仅支持 pcm 数据，默认 VELAudioFileType_UnKnown  
@property (nonatomic, assign) VELAudioFileType fileType;

@property (nonatomic, assign) BOOL playable;
@end

UIKIT_STATIC_INLINE void vel_sync_main_queue(dispatch_block_t block) {
    if (NSThread.isMainThread) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

//  错误  
#define VEL_ERROR(c, des) [NSError errorWithDomain:NSURLErrorDomain code:c userInfo:@{NSLocalizedDescriptionKey : des?:@""}]
//  当前的CMTime，纳秒  
#define VEL_CURRENT_CMTIME CMTimeMakeWithSeconds(CACurrentMediaTime(), 1000000000)
//  是否是空对象  
#define VEL_IS_NULL_OBJ(obj) (obj == nil || ((id)obj == NSNull.null) || [obj isKindOfClass:NSNull.class])
//  是否是空字符串  
#define VEL_IS_EMPTY_STRING(s) (VEL_IS_NULL_OBJ(s) || s.length == 0)
#define VEL_IS_NOT_EMPTY_STRING(s) !VEL_IS_EMPTY_STRING(s)

NS_ASSUME_NONNULL_END
