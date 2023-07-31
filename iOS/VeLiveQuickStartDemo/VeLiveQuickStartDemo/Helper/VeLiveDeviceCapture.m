/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveDeviceCapture.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/6/28.
//

#import "VeLiveDeviceCapture.h"
@interface VeLiveDeviceCapture () <AVCaptureVideoDataOutputSampleBufferDelegate, AVCaptureAudioDataOutputSampleBufferDelegate>
@property (nonatomic, strong) dispatch_queue_t sessionQueue;
@property (nonatomic, strong) dispatch_queue_t captureQueue;
@property (atomic, strong) AVCaptureSession *captureSession;
@property (atomic, strong) AVCaptureDevice *inputCamera;

@property (atomic, strong) AVCaptureDeviceInput *videoInput;
@property (atomic, strong) AVCaptureVideoDataOutput *videoOutput;
@property (atomic, strong) AVCaptureConnection *videoConnection;

@property (atomic, strong) AVCaptureDeviceInput *audioInput;
@property (atomic, strong) AVCaptureAudioDataOutput *audioOutput;

@property (atomic, assign) BOOL cameraGranted;
@property (atomic, assign) BOOL microGranted;
@end

@implementation VeLiveDeviceCapture
- (instancetype)init {
    self = [super init];
    if (self) {
        self.sessionQueue = dispatch_queue_create("com.ttsdk.quickstartdemo.session", DISPATCH_QUEUE_SERIAL);
        self.captureQueue = dispatch_queue_create("com.ttsdk.quickstartdemo.capture", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (void)createSession {
    dispatch_async(self.sessionQueue, ^{
        [self configureSession];
    });
}

// Call this on the session queue.
- (void)configureSession {
    self.captureSession = [[AVCaptureSession alloc] init];
    NSError *error = nil;
    [self.captureSession beginConfiguration];
    
    if (self.cameraGranted) {
        self.captureSession.sessionPreset = AVCaptureSessionPreset1280x720;
        
        // Add video input.
        AVCaptureDevice* videoDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera
                                                                          mediaType:AVMediaTypeVideo
                                                                           position:AVCaptureDevicePositionFront];
        if (!videoDevice) {
            videoDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera
                                                             mediaType:AVMediaTypeVideo
                                                              position:AVCaptureDevicePositionBack];
        }
        AVCaptureDeviceInput* videoDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:videoDevice error:&error];
        if (!videoDeviceInput) {
            NSLog(@"VeLiveQuickStartDemo: Could not create video device input: %@", error);
            [self.captureSession commitConfiguration];
            return;
        }
        
        if ([self.captureSession canAddInput:videoDeviceInput]) {
            [self.captureSession addInput:videoDeviceInput];
            self.videoInput = videoDeviceInput;
            self.videoOutput = [[AVCaptureVideoDataOutput alloc] init];
            [self.videoOutput setSampleBufferDelegate:self queue:self.captureQueue];
            NSDictionary *captureSettings = @{(NSString *) kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)};
            self.videoOutput.videoSettings = captureSettings;
            self.videoOutput.alwaysDiscardsLateVideoFrames = YES;
            if ([self.captureSession canAddOutput:self.videoOutput]) {
                [self.captureSession addOutput:self.videoOutput];
            }
        } else {
            NSLog(@"VeLiveQuickStartDemo: Could not add video device input to the session");
            [self.captureSession commitConfiguration];
            return;
        }
        
        self.videoConnection = [self.videoOutput connectionWithMediaType:AVMediaTypeVideo];
        if ([self.videoConnection isVideoOrientationSupported]) {
            self.videoConnection.videoOrientation = AVCaptureVideoOrientationPortrait;
        }
    }
    if (self.microGranted) {
        // Add Audio Input
        AVCaptureDevice *audioDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeAudio];
        AVCaptureDeviceInput *audioDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:audioDevice error:&error];
        if (!audioDeviceInput) {
            NSLog(@"VeLiveQuickStartDemo: Could not create audio device input: %@", error);
            [self.captureSession commitConfiguration];
            return;
        }
        
        if ([self.captureSession canAddInput:audioDeviceInput]) {
            [self.captureSession addInput:audioDeviceInput];
            self.audioInput = audioDeviceInput;
            
            self.audioOutput = [[AVCaptureAudioDataOutput alloc] init];
            [self.audioOutput setSampleBufferDelegate:self queue:self.captureQueue];
            if ([self.captureSession canAddOutput:self.audioOutput]) {
                [self.captureSession addOutput:self.audioOutput];
            }
        } else {
            NSLog(@"VeLiveQuickStartDemo: Could not add audio device input to the session");
            [self.captureSession commitConfiguration];
            return;
        }
    }
    [self.captureSession commitConfiguration];
}

- (void)startCapture {
    [VeLiveDeviceCapture requestCameraAndMicroAuthorization:^(BOOL cameraGranted, BOOL microGranted) {
        self.cameraGranted = cameraGranted;
        self.microGranted = microGranted;
        [self createSession];
        dispatch_async(self.sessionQueue, ^{
            if (!self.captureSession.isRunning) {
                [self.captureSession startRunning];
            }
        });
    }];
}

- (void)stopCapture {
    if (!self.captureSession) {
        return;
    }
    dispatch_async(self.sessionQueue, ^{
        if (self.captureSession.isRunning) {
            [self.captureSession stopRunning];
        }
    });
}


#pragma mark - AVCaptureVideoDataOutputSampleBufferDelegate
- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
       fromConnection:(AVCaptureConnection *)connection {
    if (output == self.videoOutput) {
        if (self.delegate && [self.delegate respondsToSelector:@selector(capture:didOutputVideoSampleBuffer:)]) {
            [self.delegate capture:self didOutputVideoSampleBuffer:sampleBuffer];
        }
    } else if (output == self.audioOutput){
        if (self.delegate && [self.delegate respondsToSelector:@selector(capture:didOutputAudioSampleBuffer:)]) {
            [self.delegate capture:self didOutputAudioSampleBuffer:sampleBuffer];
        }
    }
}

+ (void)requestCameraAndMicroAuthorization:(void (^)(BOOL cameraGranted, BOOL microGranted))handler {
    dispatch_group_t group = dispatch_group_create();
    __block BOOL cg = NO;
    __block BOOL mg = NO;
    dispatch_group_enter(group);
    [self requestCameraAuthorization:^(BOOL granted) {
        cg = granted;
        dispatch_group_leave(group);
    }];
    dispatch_group_enter(group);
    [self requestMicrophoneAuthorization:^(BOOL granted) {
        mg = granted;
        dispatch_group_leave(group);
    }];
    dispatch_group_notify(group, dispatch_get_main_queue(), ^{
        if (handler) {
            handler(cg, mg);
        }
    });
}

+ (void)requestCameraAuthorization:(void (^)(BOOL granted))handler {
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (authStatus == AVAuthorizationStatusNotDetermined) {
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
            handler(granted);
        }];
    } else if (authStatus == AVAuthorizationStatusAuthorized) {
        handler(YES);
    } else {
        handler(NO);
    }
}

+ (void)requestMicrophoneAuthorization:(void (^)(BOOL granted))handler {
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeAudio];
    if (authStatus == AVAuthorizationStatusNotDetermined) {
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeAudio completionHandler:^(BOOL granted) {
            handler(granted);
        }];
    } else if (authStatus == AVAuthorizationStatusAuthorized) {
        handler(YES);
    } else {
        handler(NO);
    }
}
@end
