/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  VeLiveAudienceManager.m
//  VeLiveQuickStartDemo
//
//  Created by Volcano Engine Team on 2023/2/22.
//

#import "VeLiveAudienceManager.h"
@interface VeLiveAudienceManager () <ByteRTCRoomDelegate, ByteRTCVideoDelegate, VeLivePlayerObserver>
@property (nonatomic, strong, readwrite) ByteRTCVideo *rtcVideo;
@property (nonatomic, strong, readwrite) ByteRTCRoom *rtcRoom;
@property (nonatomic, copy, readwrite) NSString *appId;
@property (nonatomic, copy, readwrite) NSString *userId;
@property (nonatomic, copy, readwrite) NSString *roomId;
@property (nonatomic, copy, readwrite) NSString *token;
@property (nonatomic, assign, getter=isInteractive) BOOL interactive;
@property (nonatomic, copy) NSString *playUrl;
@property (nonatomic, strong, readwrite) TVLManager *livePlayer;
@property (nonatomic, assign) NSInteger streamRetryCount;
@end
@implementation VeLiveAudienceManager
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
            [self.rtcVideo startVideoCapture];
        } else {
            NSLog(@"VeLiveQuickStartDemo: Please turn on the camera permission");
        }
    }];
    
}

- (void)stopVideoCapture {
    [self.rtcVideo stopVideoCapture];
    [self setupLocalVideoView:nil];
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

- (void)stopAudioCapture {
    [self.rtcVideo stopAudioCapture];
}

- (void)startInteract:(NSString *)roomId token:(NSString *)token delegate:(id <VeLiveAudienceDelegate>)delegate {
    [self stopPlay];
    [self startAudioCapture];
    [self startVideoCapture];
    
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
    [self.rtcRoom leaveRoom];
    self.interactive = NO;
    [self stopAudioCapture];
    [self stopVideoCapture];
    [self startPlayIfNeed];
}

- (void)sendSeiMessage:(NSString *)message repeat:(int)repeat {
    [self.rtcVideo sendSEIMessage:(ByteRTCStreamIndexMain)
                       andMessage:[message dataUsingEncoding:NSUTF8StringEncoding]
                   andRepeatCount:repeat
                 andCountPerFrame:(kSingleSEIPerFrame)];
}

- (void)destory {
    [self stopPlay];
    [self.livePlayer destroy];
    self.livePlayer = nil;
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
            if (self.delegate && [self.delegate respondsToSelector:@selector(manager:onJoinRoom:)]) {
                [self.delegate manager:self onJoinRoom:uid];
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
        ByteRTCVideoEncoderConfig *solution = [[ByteRTCVideoEncoderConfig alloc] init];
        solution.width = self.config.captureWidth;
        solution.height = self.config.captureHeight;
        solution.frameRate = self.config.captureFps;
        solution.maxBitrate = self.config.videoEncoderKBitrate;
        [self.rtcVideo setVideoOrientation:(ByteRTCVideoOrientationPortrait)];
        [self.rtcVideo setMaxVideoEncoderConfig:solution];
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
    
    if (_livePlayer) {
        [view addSubview:_livePlayer.playerView];
        _livePlayer.playerView.frame = view.bounds;
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

- (void)startPlay:(NSString *)url {
    [self setupPlayerIfNeed];
    self.playUrl = url;
    [self.livePlayer setPlayUrl:url];
    [self.livePlayer play];
    self.livePlayer.playerView.hidden = NO;
    [self setupLocalVideoView:self.localVideoView];
}

- (void)stopPlay {
    [NSObject cancelPreviousPerformRequestsWithTarget:self];
    self.streamRetryCount = 0;
    [self.livePlayer stop];
    self.livePlayer.playerView.hidden = YES;
}

// MARK: - Private
- (void)startPlayIfNeed {
    if (self.livePlayer) {
        [self startPlay:self.playUrl];
    }
}
- (void)setupPlayerIfNeed {
    if (self.livePlayer == nil) {
        //  创建播放器  
        TVLManager *livePlayer =  [[TVLManager alloc] initWithOwnPlayer:YES];
        self.livePlayer = livePlayer;
        //  设置播放器回调  
        [self.livePlayer setObserver:self];
        //  播放器基础设置  
        VeLivePlayerConfiguration *config = [[VeLivePlayerConfiguration alloc]init];
        config.enableStatisticsCallback = YES;
        config.enableLiveDNS = YES;
        [self.livePlayer setConfig:config];
    }
    
    if (self.livePlayer.playerView.superview != self.localVideoView) {
        [self.livePlayer.playerView removeFromSuperview];
        [self.localVideoView addSubview:self.livePlayer.playerView];
        self.livePlayer.playerView.frame = self.localVideoView.bounds;
    }
    
    //  设置渲染模式  
    [self.livePlayer setRenderFillMode:VeLivePlayerFillModeAspectFill];
}

#pragma mark - VeLivePlayerObserver

- (void)onError:(TVLManager *)player error:(VeLivePlayerError *)error {
    NSLog(@"VeLiveQuickStartDemo: Play Error: %ld", error.errorCode);
}

- (void)onFirstVideoFrameRender:(TVLManager *)player isFirstFrame:(BOOL)isFirstFrame {
    NSLog(@"VeLiveQuickStartDemo: First Video Frame: %d", isFirstFrame);
}

- (void)onFirstAudioFrameRender:(TVLManager *)player isFirstFrame:(BOOL)isFirstFrame {
    NSLog(@"VeLiveQuickStartDemo: First Audio Frame: %d", isFirstFrame);
}

- (void)onStallStart:(TVLManager *)player {
    NSLog(@"VeLiveQuickStartDemo: Stall Start");
}

- (void)onStallEnd:(TVLManager *)player {
    NSLog(@"VeLiveQuickStartDemo: Stall End");
}

- (void)onVideoRenderStall:(TVLManager *)player stallTime:(int64_t)stallTime  {
    NSLog(@"VeLiveQuickStartDemo: Video Render Stall：%lld", stallTime);
}

- (void)onAudioRenderStall:(TVLManager *)player stallTime:(int64_t)stallTime  {
    NSLog(@"VeLiveQuickStartDemo: Video Render Stall：%lld", stallTime);
}

- (void)onResolutionSwitch:(TVLManager *)player resolution:(VeLivePlayerResolution)resolution error:(VeLivePlayerError *)error reason:(VeLivePlayerResolutionSwitchReason)reason  {
    NSLog(@"VeLiveQuickStartDemo: Resolution Switch resolution=%ld", resolution);
}

- (void)onVideoSizeChanged:(TVLManager *)player width:(int)width height:(int)height  {
    NSLog(@"VeLiveQuickStartDemo: Video Size Change width=%d height=%d", width, height);
}

- (void)onReceiveSeiMessage:(TVLManager *)player message:(NSString*)message  {
    NSLog(@"VeLiveQuickStartDemo: SEI :%@",message);
}

- (void)onMainBackupSwitch:(TVLManager *)player streamType:(VeLivePlayerStreamType)streamType error:(VeLivePlayerError *)error  {
    NSLog(@"VeLiveQuickStartDemo: Main Backup Switch");
}

- (void)onPlayerStatusUpdate:(TVLManager *)player status:(VeLivePlayerStatus)status  {
    if(status == VeLivePlayerStatusPrepared) {
        NSLog(@"VeLiveQuickStartDemo: State: Prepared");
    }else if(status == VeLivePlayerStatusPlaying) {
        NSLog(@"VeLiveQuickStartDemo: State: Playing");
    }else if(status == VeLivePlayerStatusPaused) {
        NSLog(@"VeLiveQuickStartDemo: State: Paused");
    }else if(status == VeLivePlayerStatusStopped) {
        NSLog(@"VeLiveQuickStartDemo: State: Stopped");
    }else if(status == VeLivePlayerStatusError) {
        NSLog(@"VeLiveQuickStartDemo: State: Error");
    }
}

- (void)onStatistics:(TVLManager *)player statistics:(VeLivePlayerStatistics *)statistics {
    NSLog(@"VeLiveQuickStartDemo: statistics.url=%@", statistics.url);
    NSLog(@"VeLiveQuickStartDemo: statistics.width=%d", statistics.width);
    NSLog(@"VeLiveQuickStartDemo: statistics.height=%d", statistics.height);
    NSLog(@"VeLiveQuickStartDemo: statistics.isHardWareDecode=%d", statistics.isHardWareDecode);
    NSLog(@"VeLiveQuickStartDemo: statistics.videoCodec=%@", statistics.videoCodec);
    NSLog(@"VeLiveQuickStartDemo: statistics.bitrate=%ld", statistics.bitrate);
    NSLog(@"VeLiveQuickStartDemo: statistics.fps=%f", statistics.fps);
}

- (void)onSnapshotComplete:(TVLManager *)player image:(UIImage *)image  {
    NSLog(@"VeLiveQuickStartDemo: snapshot");
}

- (void)onRenderVideoFrame:(TVLManager *)player videoFrame:(VeLivePlayerVideoFrame *)videoFrame  {
    NSLog(@"VeLiveQuickStartDemo: on render video frame");
}

- (void)onRenderAudioFrame:(TVLManager *)player audioFrame:(VeLivePlayerAudioFrame *)audioFrame  {
    NSLog(@"VeLiveQuickStartDemo: on render audio frame");
}


@end
