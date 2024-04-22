/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveiveConfig.h
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/3/2.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface VeLiveConfig : NSObject
//  采集宽度  
@property (nonatomic, assign) int captureWidth;
//  采集高度  
@property (nonatomic, assign) int captureHeight;
//  采集帧率  
@property (nonatomic, assign) int captureFps;
//  音频采样率  
@property (nonatomic, assign) int audioCaptureSampleRate;
//  音频通道数  
@property (nonatomic, assign) int audioCaptureChannel;
//  视频编码宽  
@property (nonatomic, assign) int videoEncoderWith;
//  视频编码高  
@property (nonatomic, assign) int videoEncoderHeight;
//  视频编码帧率  
@property (nonatomic, assign) int videoEncoderFps;
//  视频编码比特率  
@property (nonatomic, assign) int videoEncoderKBitrate;
//  是否开启硬件编码  
@property (nonatomic, assign) BOOL videoHardwareEncoder;
//  音频编码采样率  
@property (nonatomic, assign) int audioEncoderSampleRate;
//  音频编码通道数  
@property (nonatomic, assign) int audioEncoderChannel;
//  音频编码比特率  
@property (nonatomic, assign) int audioEncoderKBitrate;
@end
NS_ASSUME_NONNULL_END
