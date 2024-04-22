/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveAnchorManager.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/2/22.
//

#import "VeLiveAnchorManager.h"
@interface VeLiveAnchorManager () <ByteRTCRoomDelegate, ByteRTCVideoDelegate, ByteRTCVideoSinkDelegate, ByteRTCAudioFrameObserver, ByteRTCMixedStreamObserver, VeLivePusherObserver>
@property (nonatomic, strong, readwrite) ByteRTCVideo *rtcVideo;
@property (nonatomic, strong, readwrite) ByteRTCRoom *rtcRoom;
@property (nonatomic, copy, readwrite) NSString *appId;
@property (nonatomic, copy, readwrite) NSString *userId;
@property (nonatomic, copy, readwrite) NSString *roomId;
@property (nonatomic, copy, readwrite) NSString *token;
@property (nonatomic, assign, getter=isInteractive) BOOL interactive;
@property (nonatomic, strong, readwrite) VeLivePusher *livePusher;
@property (nonatomic, strong) ByteRTCMixedStreamConfig *mixStreamConfig;
@property (nonatomic, copy) NSString *streamUrl;
@property (nonatomic, copy) NSString *rtcTaskId;
@property (nonatomic, assign) BOOL isStartPush;
@end
@implementation VeLiveAnchorManager
- (instancetype)initWithAppId:(NSString *)appId userId:(NSString *)userId {
    if (self = [super init]) {
        self.appId = appId;
        self.userId = userId;
        self.config = [[VeLiveConfig alloc] init];
    }
    return self;
}

- (instancetype)init {
    return [self initWithAppId:@"" userId:@""];
}

- (void)setLocalVideoView:(UIView *)localVideoView {
    _localVideoView = localVideoView;
    [self setupLocalVideoView:localVideoView];
}

- (void)setRemoteVideoView:(UIView *)view forUid:(NSString *)uid {
    if (self.rtcVideo) {
        ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
        canvas.renderMode = ByteRTCRenderModeHidden;
        canvas.view.backgroundColor = [UIColor clearColor];
        canvas.view = view;
        
        ByteRTCRemoteStreamKey *streamKey = [[ByteRTCRemoteStreamKey alloc] init];
        streamKey.userId = uid;
        streamKey.streamIndex = ByteRTCStreamIndexMain;
        streamKey.roomId = self.roomId;
        [self.rtcVideo setRemoteVideoCanvas:streamKey withCanvas:canvas];
    }
}

- (void)startVideoCapture {
    [self createRTCVideoIfNeed];
    __weak __typeof__(self)weakSelf = self;
    [self authorPermissionFor:(AVMediaTypeVideo) completion:^(BOOL granted) {
        __strong __typeof__(weakSelf)self = weakSelf;
        if (granted) {
            
            //  使用前置摄像头，本地预览和推流镜像  
            [self.rtcVideo switchCamera:ByteRTCCameraIDFront];
            [self.rtcVideo setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];

            //  使用后置摄像头参考代码  
            //[self.rtcVideo switchCamera:ByteRTCCameraIDBack];
            //[self.rtcVideo setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    
            [self.rtcVideo startVideoCapture];
        } else {
            NSLog(@"VeLiveQuickStartDemo: Please turn on the camera permission");
        }
    }];
}

- (void)registerVideoListener {
    //  接管视频采集回调  
    [self.rtcVideo setLocalVideoSink:ByteRTCStreamIndexMain
                            withSink:self
                     withPixelFormat:(ByteRTCVideoSinkPixelFormatBGRA)];
}

- (void)stopVideoCapture {
    [self.rtcVideo stopVideoCapture];
    [self setupLocalVideoView:nil];
}

- (void)unregisterVideoListener {
    [self.rtcVideo setLocalVideoSink:(ByteRTCStreamIndexMain)
                            withSink:nil
                     withPixelFormat:(ByteRTCVideoSinkPixelFormatBGRA)];
}

- (void)startAudioCapture {
    [self createRTCVideoIfNeed];
    __weak __typeof__(self)weakSelf = self;
    [self authorPermissionFor:(AVMediaTypeAudio) completion:^(BOOL granted) {
        __strong __typeof__(weakSelf)self = weakSelf;
        if (granted) {
            [self.rtcVideo startAudioCapture];
        } else {
            NSLog(@"VeLiveQuickStartDemo: Please turn on the microphone permission");
        }
    }];
}

- (void)registerAudioListener {
    //  接管音频采集回调  
    ByteRTCAudioFormat *audioFormat = [[ByteRTCAudioFormat alloc] init];
    audioFormat.channel = ByteRTCAudioChannelStereo;
    audioFormat.sampleRate = ByteRTCAudioSampleRate44100;
    [self.rtcVideo enableAudioFrameCallback:(ByteRTCAudioFrameCallbackRecord) format:audioFormat];
    [self.rtcVideo registerAudioFrameObserver:self];
}

- (void)stopAudioCapture {
    [self.rtcVideo stopAudioCapture];
}

- (void)unregisterAudioListener {
    [self.rtcVideo disableAudioFrameCallback:(ByteRTCAudioFrameCallbackRecord)];
    [self.rtcVideo registerAudioFrameObserver:nil];
}

- (void)startInteract:(NSString *)roomId token:(NSString *)token delegate:(id <VeLiveAnchorDelegate>)delegate {
    //  停止推流  
    [self stopPush];
    
    self.roomId = roomId;
    self.token = token;
    self.delegate = delegate;
    [self setupRTCRoomIfNeed];
    
    //  设置用户信息  
    ByteRTCUserInfo *userInfo = [[ByteRTCUserInfo alloc] init];
    userInfo.userId = self.userId;
    
    //  加入房间，开始连麦  
    ByteRTCRoomConfig *config = [ByteRTCRoomConfig new];
    config.isAutoPublish = YES;
    config.isAutoSubscribeAudio = YES;
    config.isAutoSubscribeVideo = YES;
    
    //  加入RTC房间  
    NSLog(@"VeLiveQuickStartDemo: join room %@ - %@", roomId, self.userId);
    [self.rtcRoom joinRoom:token userInfo:userInfo roomConfig:config];
    self.interactive = YES;
}


- (void)stopInteract {
    self.mixStreamConfig = nil;
    [self.rtcVideo stopPushStreamToCDN:self.rtcTaskId];
    [self.rtcRoom leaveRoom];
    self.interactive = NO;
    [self startPushIfNeed];
}

- (void)sendSeiMessage:(NSString *)message repeat:(int)repeat {
    if (!self.isInteractive) {
        [self.livePusher sendSeiMessage:@"live_engine"
                                  value:message
                                 repeat:repeat
                             isKeyFrame:YES
                          allowsCovered:YES];
    } else {
        //  合流时发送的SEI，跟随每一帧  
//        ByteRTCVideoCompositingLayout *layout = self.rtcLiveTranscoding.layout;
//        layout.appData = message;
//        [self updateLiveTranscodingLayout:layout];
        [self.rtcVideo sendSEIMessage:(ByteRTCStreamIndexMain)
                           andMessage:[message dataUsingEncoding:NSUTF8StringEncoding]
                       andRepeatCount:repeat
                     andCountPerFrame:kSingleSEIPerFrame];
    }
}

- (void)destory {
    [self stopPush];
    [self.livePusher destroy];
    self.livePusher = nil;
    [self stopInteract];
    [self stopVideoCapture];
    [self stopAudioCapture];
    [self.rtcRoom destroy];
    self.rtcRoom = nil;
    [ByteRTCVideo destroyRTCVideo];
    self.rtcVideo = nil;
    
}

// MARK: - ByteRTCVideoDelegate
- (void)rtcEngine:(ByteRTCVideo *_Nonnull)engine onWarning:(ByteRTCWarningCode)Code {
    NSLog(@"VeLiveQuickStartDemo: rtc on Warning");
}

- (void)rtcEngine:(ByteRTCVideo *_Nonnull)engine onError:(ByteRTCErrorCode)errorCode {
    NSLog(@"VeLiveQuickStartDemo: rtc on Error");
}

- (void)rtcEngine:(ByteRTCVideo *_Nonnull)engine onCreateRoomStateChanged:(NSString * _Nonnull)roomId errorCode:(NSInteger)errorCode {
    if (errorCode != 0) {
        NSLog(@"VeLiveQuickStartDemo: on create room error");
    }
}

// MARK: - ByteRTCRoomDelegate
- (void)rtcRoom:(ByteRTCRoom *_Nonnull)rtcRoom
   onRoomStateChanged:(NSString *_Nonnull)roomId
            withUid:(nonnull NSString *)uid
          state:(NSInteger)state
      extraInfo:(NSString *_Nonnull)extraInfo {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (state == 0) {
            if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onJoinRoom:state:)]) {
                [self.delegate manager:self onJoinRoom:uid state:state];
            }
        } else {
            NSLog(@"VeLiveQuickStartDemo: on join room error");
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserJoined:(ByteRTCUserInfo *)userInfo elapsed:(NSInteger)elapsed {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onUserJoined:)]) {
            [self.delegate manager:self onUserJoined:userInfo.userId];
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserLeave:(NSString *)uid reason:(ByteRTCUserOfflineReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onUserLeave:)]) {
            [self.delegate manager:self onUserLeave:uid];
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserPublishStream:(NSString *)userId type:(ByteRTCMediaStreamType)type {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onUserPublishStream:type:)]) {
            [self.delegate manager:self onUserPublishStream:userId type:type];
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserUnpublishStream:(NSString * _Nonnull)userId type:(ByteRTCMediaStreamType)type reason:(ByteRTCStreamRemoveReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onUserUnPublishStream:type:reason:)]) {
            [self.delegate manager:self onUserUnPublishStream:userId type:type reason:reason];
        }
    });
}

// MARK: - Private
- (void)createRTCVideoIfNeed {
    if (self.rtcVideo == nil && self.appId != nil && self.appId.length > 0) {
        self.rtcVideo = [ByteRTCVideo createRTCVideo:self.appId delegate:self parameters:@{}];
        // 设置采集参数 
        ByteRTCVideoCaptureConfig *captureConfig = [[ByteRTCVideoCaptureConfig alloc] init];
        captureConfig.videoSize = CGSizeMake(self.config.captureWidth, self.config.captureHeight);
        captureConfig.frameRate = self.config.captureFps;
        captureConfig.preference = ByteRTCVideoCapturePreferenceAuto;
        [self.rtcVideo setVideoCaptureConfig:captureConfig];
        //  设置编码参数  
        ByteRTCVideoEncoderConfig *solution = [[ByteRTCVideoEncoderConfig alloc] init];
        solution.width = self.config.videoEncoderWith;
        solution.height = self.config.videoEncoderHeight;
        solution.frameRate = self.config.videoEncoderFps;
        solution.maxBitrate = self.config.videoEncoderKBitrate;
        [self.rtcVideo setMaxVideoEncoderConfig:solution];
        //  使用前置摄像头，本地预览和推流镜像  
        [self.rtcVideo switchCamera:(ByteRTCCameraIDFront)];
        //  设置镜像  
        [self.rtcVideo setLocalVideoMirrorType:(ByteRTCMirrorTypeRenderAndEncoder)];
        //  设置视频方向  
        [self.rtcVideo setVideoOrientation:(ByteRTCVideoOrientationPortrait)];
        //  设置预览视图  
        [self setupLocalVideoView:_localVideoView];
    }
}

- (void)setupLocalVideoView:(UIView *)view {
    if (_rtcVideo) {
        //  设置本地View  
        ByteRTCVideoCanvas *canvasView = [[ByteRTCVideoCanvas alloc] init];
        canvasView.view = view;
        canvasView.renderMode = ByteRTCRenderModeHidden;
        [self.rtcVideo setLocalVideoCanvas:ByteRTCStreamIndexMain withCanvas:canvasView];
    }
}

- (void)setupRTCRoomIfNeed {
    //  创建 RTC 房间  
    if (self.rtcRoom == nil) {
        self.rtcRoom = [self.rtcVideo createRTCRoom:self.roomId];
        self.rtcRoom.delegate = self;
    }
}

- (void)authorPermissionFor:(AVMediaType)type completion:(void (^)(BOOL granted))completion  {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:type];
    if (status == AVAuthorizationStatusNotDetermined || status == AVAuthorizationStatusRestricted) {
        [AVCaptureDevice requestAccessForMediaType:type completionHandler:^(BOOL granted) {
            dispatch_async(dispatch_get_main_queue(), ^{
                completion(granted);
            });
        }];
    } else {
        completion(status == AVAuthorizationStatusAuthorized);
    }
}

- (void)dealloc {
    [self destory];
}

- (void)startPush:(NSString *)url {
    if (self.isStartPush) {
        return;
    }
    self.isStartPush = YES;
    self.streamUrl = url;
    [self setupLivePusher];
    [self registerAudioListener];
    [self registerVideoListener];
    [self.livePusher startPush:url];
}

- (void)stopPush {
    if (!self.isStartPush) {
        return;
    }
    self.isStartPush = NO;
    [self unregisterAudioListener];
    [self unregisterVideoListener];
    [self.livePusher stopPush];
}

- (void)startForwardStream:(NSArray<ForwardStreamConfiguration *> *)forwardStreamInfos {
    [self stopPush];
    [self.rtcRoom startForwardStreamToRooms:forwardStreamInfos];
}

- (void)stopForwardStream {
    [self.rtcRoom stopForwardStreamToRooms];
    [self startPushIfNeed];
}

- (void)updateLiveTranscodingLayout:(ByteRTCMixedStreamLayoutConfig *)layout {
    if (self.mixStreamConfig == nil) {
        self.mixStreamConfig = [ByteRTCMixedStreamConfig defaultMixedStreamConfig];
        self.mixStreamConfig.roomID = self.roomId;
        self.mixStreamConfig.userID = self.userId;
        
        
        //  设置视频编码参数  
        self.mixStreamConfig.videoConfig.width = self.config.videoEncoderWith;
        self.mixStreamConfig.videoConfig.height = self.config.videoEncoderHeight;
        self.mixStreamConfig.videoConfig.fps = self.config.videoEncoderFps;
        self.mixStreamConfig.videoConfig.bitrate = self.config.videoEncoderKBitrate;
        
        //  设置音频编码参数  
        self.mixStreamConfig.audioConfig = [[ByteRTCMixedStreamAudioConfig alloc] init];
        self.mixStreamConfig.audioConfig.sampleRate = self.config.audioEncoderSampleRate;
        self.mixStreamConfig.audioConfig.channels = self.config.audioEncoderChannel;
        self.mixStreamConfig.audioConfig.bitrate = self.config.audioEncoderKBitrate;
        
        //  设置推流地址  
        self.mixStreamConfig.pushURL = self.streamUrl;
        //  服务端合流  
        self.mixStreamConfig.expectedMixingType = ByteRTCMixedStreamByServer;
        
        //  设置混流模版  
        self.mixStreamConfig.layoutConfig = layout;
        
        //  设置混流任务Id  
        self.rtcTaskId = @"unique_id";
        [self.rtcVideo startPushMixedStreamToCDN:self.rtcTaskId mixedConfig:self.mixStreamConfig observer:self];
    } else {
        //  设置混流模版  
        self.mixStreamConfig.layoutConfig = layout;
        //  开启RTC 服务端混流  
        [self.rtcVideo updatePushMixedStreamToCDN:self.rtcTaskId mixedConfig:self.mixStreamConfig];
    }
}

- (void)startPushIfNeed {
    if (self.livePusher) {
        [self startPush:self.streamUrl];
    }
}



// MARK: - Private
- (void)setupLivePusher {
    if (self.livePusher == nil) {
        //  创建推流器  
        self.livePusher = [[VeLivePusher alloc] initWithConfig:[[VeLivePusherConfiguration alloc] init]];
        
        //  设置推流器回调  
        [self.livePusher setObserver:self];
        
        //  视频编码配置  
        VeLiveVideoEncoderConfiguration *videoEncodeCfg = [[VeLiveVideoEncoderConfiguration alloc] initWithResolution:[self getEncodeVideoResolution]];
        
        //  视频编码初始化码率  
        videoEncodeCfg.bitrate = self.config.videoEncoderKBitrate;
        
        //  视频编码最小码率  
        videoEncodeCfg.minBitrate = self.config.videoEncoderKBitrate;
        
        //  视频编码最大码率  
        videoEncodeCfg.maxBitrate = self.config.videoEncoderKBitrate;
        
        //  配置帧率  
        videoEncodeCfg.fps = self.config.videoEncoderFps;
        
        //  硬编码  
        videoEncodeCfg.enableAccelerate = self.config.videoHardwareEncoder;
        
        //  配置编码  
        [self.livePusher setVideoEncoderConfiguration:videoEncodeCfg];
        
        //  开启外部视频采集  
        [self.livePusher startVideoCapture:(VeLiveVideoCaptureExternal)];
        
        //  开启外部音频采集  
        [self.livePusher startAudioCapture:(VeLiveAudioCaptureExternal)];
    }
}

- (VeLiveVideoResolution)getEncodeVideoResolution {
    if (MAX(self.config.videoEncoderWith, self.config.videoEncoderHeight) >= 1920) {
        return VeLiveVideoResolution1080P;
    } else if (MAX(self.config.videoEncoderWith, self.config.videoEncoderHeight) >= 1280) {
        return VeLiveVideoResolution720P;
    } else if (MAX(self.config.videoEncoderWith, self.config.videoEncoderHeight) >= 960) {
        return VeLiveVideoResolution540P;
    } else if (MAX(self.config.videoEncoderWith, self.config.videoEncoderHeight) >= 640) {
        return VeLiveVideoResolution360P;
    }
    return VeLiveVideoResolution720P;
}

// MARK: - ByteRTCVideoSinkDelegate
- (void)renderPixelBuffer:(CVPixelBufferRef)pixelBuffer
                 rotation:(ByteRTCVideoRotation)rotation
              contentType:(ByteRTCVideoContentType)contentType
             extendedData:(NSData *)extendedData {
    if (self.isInteractive) {
        return;
    }
    CMTime pts = CMTimeMakeWithSeconds(CACurrentMediaTime(), 1000000000);
    VeLiveVideoFrame *videoFrame = [[VeLiveVideoFrame alloc] init];
    videoFrame.pts = pts;
    videoFrame.pixelBuffer = pixelBuffer;
    VeLiveVideoRotation videoRotation = VeLiveVideoRotation0;
    switch (rotation) {
        case ByteRTCVideoRotation0:
            videoRotation = VeLiveVideoRotation0;
            break;
        case ByteRTCVideoRotation90:
            videoRotation = VeLiveVideoRotation90;
            break;
        case ByteRTCVideoRotation180:
            videoRotation = VeLiveVideoRotation180;
            break;
        case ByteRTCVideoRotation270:
            videoRotation = VeLiveVideoRotation270;
            break;
            
        default:
            break;
    }
    videoFrame.rotation = videoRotation;
    videoFrame.bufferType = VeLiveVideoBufferTypePixelBuffer;
    [self.livePusher pushExternalVideoFrame:videoFrame];
}

- (int)getRenderElapse {
    return 0;
}

// MARK: - ByteRTCAudioFrameObserver
- (void)onRecordAudioFrame:(ByteRTCAudioFrame * _Nonnull)audioFrame {
    if (self.isInteractive) {
        return;
    }
    int channel = 2;
    if (audioFrame.channel == ByteRTCAudioChannelMono) {
        channel = 1;
    } else if (audioFrame.channel == ByteRTCAudioChannelStereo) {
        channel = 2;
    }
    
    CMTime pts = CMTimeMakeWithSeconds(CACurrentMediaTime(), 1000000000);
    
    VeLiveAudioFrame *frame = [[VeLiveAudioFrame alloc] init];
    frame.bufferType = VeLiveAudioBufferTypeNSData;
    frame.data = audioFrame.buffer;
    frame.pts = pts;
    frame.channels = (VeLiveAudioChannel)channel;
    frame.sampleRate = VeLiveAudioSampleRate44100;
    
    [self.livePusher pushExternalAudioFrame:frame];
}

- (void)onPlaybackAudioFrame:(ByteRTCAudioFrame * _Nonnull)audioFrame; {
    
}

- (void)onRemoteUserAudioFrame:(ByteRTCRemoteStreamKey * _Nonnull)streamKey
                    audioFrame:(ByteRTCAudioFrame * _Nonnull)audioFrame; {
    
}

- (void)onMixedAudioFrame:(ByteRTCAudioFrame * _Nonnull)audioFrame {
    
}
// MARK: - LiveTranscodingDelegate
- (BOOL)isSupportClientPushStream {
    return NO;
}

- (void)onMixingEvent:(ByteRTCStreamMixingEvent)event
                  taskId:(NSString *_Nonnull)taskId
                      error:(ByteRTCStreamMixingErrorCode)Code
                    mixType:(ByteRTCMixedStreamType)mixType {
    if (Code == ByteRTCStreamMixingErrorCodeOK && event == ByteRTCStreamMixingEventStartSuccess) {
        //  如果合流降级到了服务端，需要把当前标识置为 NO，并停止 LiveCore 的推流  
        if (mixType == ByteRTCMixedStreamByServer) {
            NSLog(@"VeLiveQuickStartDemo: mix by server success");
        }
    }
    NSLog(@"VeLiveQuickStartDemo: mix event call back %d-%d-%d", (int)event, (int)Code, (int)mixType);
}

// MARK: - VeLivePusherObserver
- (void)onError:(int)code subcode:(int)subcode message:(nullable NSString *)msg {
    NSLog(@"VeLiveQuickStartDemo: Error %d-%d-%@", code, subcode, msg?:@"");
}

- (void)onStatusChange:(VeLivePushStatus)status {
    NSLog(@"VeLiveQuickStartDemo: Status %@", @(status));
}
@end
