/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveDeviceCapture.h
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//
/*
 本类提供基本的摄像头采集能力，用于自定义采集时使用。
 自己的业务中请自行实现相关采集，不建议直接使用本文件做音视频采集。
 */
#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN
@class VeLiveDeviceCapture;
@protocol VeLiveDeviceCaptureDelegate <NSObject>
- (void)capture:(VeLiveDeviceCapture *)capture didOutputVideoSampleBuffer:(CMSampleBufferRef)sampleBuffer;
- (void)capture:(VeLiveDeviceCapture *)capture didOutputAudioSampleBuffer:(CMSampleBufferRef)sampleBuffer;
@end

@interface VeLiveDeviceCapture : NSObject

@property (weak, nonatomic) id <VeLiveDeviceCaptureDelegate> delegate;

- (void)startCapture;

- (void)stopCapture;

+ (void)requestCameraAndMicroAuthorization:(void (^)(BOOL cameraGranted, BOOL microGranted))handler;

+ (void)requestCameraAuthorization:(void (^)(BOOL granted))handler;

+ (void)requestMicrophoneAuthorization:(void (^)(BOOL granted))handler;
@end

NS_ASSUME_NONNULL_END
