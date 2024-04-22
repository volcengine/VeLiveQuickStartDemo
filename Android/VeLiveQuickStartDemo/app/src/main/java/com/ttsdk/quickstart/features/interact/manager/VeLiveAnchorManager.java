/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.manager;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureExternal;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioChannel.VeLiveAudioChannelMono;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioChannel.VeLiveAudioChannelStereo;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioSampleRate.VeLiveAudioSampleRate32000;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioSampleRate.VeLiveAudioSampleRate44100;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioSampleRate.VeLiveAudioSampleRate48000;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureExternal;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution720P;
import static com.ss.bytertc.engine.VideoCanvas.RENDER_MODE_HIDDEN;
import static com.ss.bytertc.engine.data.AudioFrameType.FRAME_TYPE_PCM16;
import static com.ss.bytertc.engine.live.ByteRTCStreamMixingEvent.STREAM_MIXING_START_FAILED;
import static com.ss.bytertc.engine.live.ByteRTCStreamMixingType.STREAM_MIXING_BY_SERVER;

import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import com.pandora.common.env.Env;
import com.ss.avframework.live.VeLiveAudioFrame;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
import com.ss.avframework.live.VeLiveVideoFrame;
import com.ss.avframework.opengl.YuvHelper;
import com.ss.avframework.utils.TimeUtils;
import com.ss.bytertc.engine.IAudioFrameObserver;
import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.AudioChannel;
import com.ss.bytertc.engine.data.AudioFormat;
import com.ss.bytertc.engine.data.AudioFrameCallbackMethod;
import com.ss.bytertc.engine.data.AudioSampleRate;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.ForwardStreamInfo;
import com.ss.bytertc.engine.data.MirrorType;
import com.ss.bytertc.engine.data.RemoteStreamKey;
import com.ss.bytertc.engine.data.SEICountPerFrame;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.data.VideoOrientation;
import com.ss.bytertc.engine.handler.IRTCVideoEventHandler;
import com.ss.bytertc.engine.live.ByteRTCStreamMixingEvent;
import com.ss.bytertc.engine.live.ByteRTCTranscoderErrorCode;
import com.ss.bytertc.engine.live.IMixedStreamObserver;
import com.ss.bytertc.engine.live.MixedStreamConfig;
import com.ss.bytertc.engine.live.MixedStreamType;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.RTCRoomStats;
import com.ss.bytertc.engine.type.StreamRemoveReason;
import com.ss.bytertc.engine.utils.IAudioFrame;
import com.ss.bytertc.engine.video.IVideoSink;
import com.ss.bytertc.engine.video.VideoCaptureConfig;

import java.nio.ByteBuffer;
import java.util.List;

public class VeLiveAnchorManager {
    private static final String TAG = "VeLiveAnchorManager";
    private IListener mRoomListener;
    private static VeLiveAnchorManager mInstance;
    private VeLiveAnchorManager.Config mConfig;
    //  推流器  
    private VeLivePusher mLivePusher;
    //  RTC 音视频采集对象  
    private RTCVideo mRTCVideo;
    //  RTC 房间对象  
    private RTCRoom mRTCRoom;
    //  音频回调监听，需要在这里定义，防止gc释放listener对象  
    private IAudioFrameObserver mAudioFrameListener;
    //  视频回调监听，需要在这里定义，防止gc释放listener对象  
    private IVideoSink mVideoFrameListener;
    //  推流直播地址  
    private String mPushUrl;
    private String mUserId;
    private boolean mIsPushing = false;
    private String mAppId;
    private String mRoomId;
    private boolean mIsTranscoding = false;
    private MixedStreamConfig mMixedStreamConfig;

    public static class Config {
        //  采集宽度  
        public int mVideoCaptureWidth = 720;
        //  采集高度  
        public int mVideoCaptureHeight = 1280;
        //  采集帧率  
        public int mVideoCaptureFps = 15;
        //  音频采样率  
        public int mAudioCaptureSampleRate = 44100;
        //  音频通道数  
        public int mAudioCaptureChannel = 2;
        //  视频编码宽  
        public int mVideoEncoderWidth = 720;
        //  视频编码高  
        public int mVideoEncoderHeight = 1280;
        //  视频编码帧率  
        public int mVideoEncoderFps = 15;
        //  视频编码比特率  
        public int mVideoEncoderKBitrate = 1600;
        //  是否开启硬件编码  
        public boolean mIsVideoHardwareEncoder = true;
        //  音频编码采样率  
        public int mAudioEncoderSampleRate = 44100;
        //  音频编码通道数  
        public int mAudioEncoderChannel = 2;
        //  音频编码比特率  
        public int mAudioEncoderKBitrate = 64;
    }

    public interface IListener {
        void onUserJoined(String uid);
        void onUserLeave(String uid);
        void onJoinRoom(String uid, int state);
        void onUserPublishStream(String uid, MediaStreamType type);
        void onUserUnPublishStream(String uid, MediaStreamType type, StreamRemoveReason reason);
    }

    private VeLiveAnchorManager(String appId, String userId) {
        mAppId = appId;
        mUserId = userId;
        initLivePusher();
        initRtcEngine();
    }

    public static VeLiveAnchorManager create(String appId, String userId) {
        if (mInstance == null) {
            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(userId)) {
                mInstance = new VeLiveAnchorManager(appId, userId);
            }
        }
        return mInstance;
    }

    public static void destroy() {
        if (mInstance != null) {
            mInstance.stopInteract();
            mInstance.stopPush();

            mInstance.releaseLivePusher();
            mInstance.releaseRtcEngine();
        }
        mInstance = null;
    }

    public RTCVideo getRTCVideo() {
        return mRTCVideo;
    }
    public void setConfig(VeLiveAnchorManager.Config config) {
        mConfig = config;
    }

    public void setLocalVideoView(TextureView renderView) {
        if (mRTCVideo == null) {
            return;
        }
        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = renderView;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        //  设置本地视频渲染视图  
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
        Log.d(TAG, "setLocalVideoView");
    }

    public void setRemoteVideoView(String uid, TextureView renderView) {
        if (mRTCVideo != null) {
            VideoCanvas canvas = new VideoCanvas();
            canvas.renderView = renderView;
            canvas.renderMode = RENDER_MODE_HIDDEN;
            RemoteStreamKey key = new RemoteStreamKey(mRoomId, uid, StreamIndex.STREAM_INDEX_MAIN);
            mRTCVideo.setRemoteVideoCanvas(key, canvas);
        }
    }

    public void startAudioCapture() {
        if (mRTCVideo != null) {
            mRTCVideo.startAudioCapture();
        }
    }

    public void startVideoCapture() {
        if (mRTCVideo != null) {

            VideoCaptureConfig captureConfig = new VideoCaptureConfig();
            captureConfig.width = mConfig.mVideoCaptureWidth;
            captureConfig.height = mConfig.mVideoCaptureHeight;
            captureConfig.frameRate = mConfig.mVideoCaptureFps;
            mRTCVideo.setVideoCaptureConfig(captureConfig);

            VideoEncoderConfig config = new VideoEncoderConfig();
            config.frameRate = mConfig.mVideoEncoderFps;
            config.width = mConfig.mVideoEncoderWidth;
            config.height = mConfig.mVideoEncoderHeight;
            config.maxBitrate = mConfig.mVideoEncoderKBitrate * 1000;
            mRTCVideo.setVideoEncoderConfig(config);

            //  使用前置摄像头，本地预览和推流镜像  
            mRTCVideo.switchCamera(CameraId.CAMERA_ID_FRONT);
            //  设置镜像  
            mRTCVideo.setLocalVideoMirrorType(MirrorType.MIRROR_TYPE_RENDER_AND_ENCODER);
            //  设置视频方向  
            mRTCVideo.setVideoOrientation(VideoOrientation.Portrait);

            //  使用后置摄像头参考代码  
            // mRTCVideo.switchCamera(CameraId.CAMERA_ID_BACK);
            // mRTCVideo.setLocalVideoMirrorType(MirrorType.MIRROR_TYPE_NONE);
            mRTCVideo.startVideoCapture();
        }
    }

    public void stopAudioCapture() {
        if (mRTCVideo != null) {
            mRTCVideo.stopAudioCapture();
        }
    }

    public void stopVideoCapture() {
        if (mRTCVideo != null) {
            mRTCVideo.stopVideoCapture();
        }
    }

    public void startPush(String url) {
        if (mIsPushing) {
            return;
        }
        setupLivePusher();
        //  打通从 RTC 到 TTSDK 的数据通路  
        registerVideoListener();
        registerAudioListener();
        //  开始直播推流  
        mPushUrl = url;
        mLivePusher.startPush(url);
        mIsPushing = true;
    }

    public void stopPush() {
        if (!mIsPushing) {
            return;
        }
        mLivePusher.stopPush();
        mIsPushing = false;
        unregisterAudioListener();
        unregisterVideoListener();
    }

    public void startInteract(String roomId, String token, VeLiveAnchorManager.IListener roomListener) {
        mRoomListener = roomListener;
        //  设置用户信息，加入房间，开始连麦  
        joinRoom(roomId, mUserId, token, mRoomListener);
    }

    public void stopInteract() {
        leaveRoom();
        startPush(mPushUrl);
    }

    public int startForwardStream(List<ForwardStreamInfo> forwardStreamInfos) {
        if (mRTCRoom != null) {
            return mRTCRoom.startForwardStreamToRooms(forwardStreamInfos);
        }
        return -1;
    }

    public void stopForwardStream() {
        if (mRTCRoom != null) {
            mRTCRoom.stopForwardStreamToRooms();
        }
    }

    public void updatePushMixedStreamToCDN(MixedStreamConfig.MixedStreamLayoutConfig layout) {
        if (!mIsTranscoding) {
            stopPush();
            startPushMixedStreamToCDN("", layout);
        } else {
            mMixedStreamConfig.setLayout(layout);
            mRTCVideo.updatePushMixedStreamToCDN("", mMixedStreamConfig);
        }
    }

    public void sendSeiMessage(String msg, int repeat) {
        if (mIsTranscoding) {
            //  合流时发送的SEI，跟随每一帧  
//           MixedStreamConfig.MixedStreamLayoutConfig layout = mMixedStreamConfig.getLayout();
//           layout.setUserConfigExtraInfo(msg);
//           updatePushMixedStreamToCDN(layout);
            //  直播间普通业务消息  
            mRTCVideo.sendSEIMessage(StreamIndex.STREAM_INDEX_MAIN,
                    TextUtils.isEmpty(msg) ? new byte[0] : msg.getBytes(), repeat, SEICountPerFrame.SEI_COUNT_PER_FRAME_SINGLE);
        } else {
            mLivePusher.sendSeiMessage("live_engine", msg, repeat, true, true);
        }
    }

    private void initLivePusher() {
        //  推流配置  
        VeLivePusherConfiguration config = new VeLivePusherConfiguration();
        //  配置上下文  
        config.setContext(Env.getApplicationContext());
        //  失败重连次数  
        config.setReconnectCount(10);
        //  创建推流器  
        mLivePusher = config.build();
        //  开始视频采集  
        mLivePusher.startVideoCapture(VeLiveVideoCaptureExternal);
        //  开始音频采集  
        mLivePusher.startAudioCapture(VeLiveAudioCaptureExternal);
        mLivePusher.setObserver(new VeLivePusherObserver() {
            @Override
            public void onError(int code, int subCode, String msg) {
                Log.e(TAG, "error" + msg);
            }
        });
        mLivePusher.setStatisticsObserver(new VeLivePusherDef.VeLivePusherStatisticsObserver() {
            @Override
            public void onStatistics(VeLivePusherDef.VeLivePusherStatistics statistics) {
                Log.i(TAG, "SendFPS:" + statistics.transportFps + " SendVideoBitrate:" + statistics.transportVideoBitrate);
            }
        }, 3);
    }

    private void setupLivePusher() {
        if (mLivePusher == null || mConfig == null) {
            return;
        }
        //  视频编码配置  
        VeLivePusherDef.VeLiveVideoEncoderConfiguration videoEncoderCfg = new VeLivePusherDef.VeLiveVideoEncoderConfiguration();
        //  设置视频分辨率，内部会根据分辨率设置最佳码率参数  
        videoEncoderCfg.setResolution(getEncodeVideoResolution());
        //  视频编码初始化码率（仅供参考）  
        videoEncoderCfg.setBitrate(mConfig.mVideoEncoderKBitrate);
        //  视频编码最大码率（仅供参考）  
        videoEncoderCfg.setMaxBitrate(mConfig.mVideoEncoderKBitrate);
        //  视频编码最小码率（仅供参考）  
        videoEncoderCfg.setMinBitrate(mConfig.mVideoEncoderKBitrate);
        //  硬编码  
        videoEncoderCfg.setEnableAccelerate(mConfig.mIsVideoHardwareEncoder);
        //  编码帧率  
        videoEncoderCfg.setFps(mConfig.mVideoEncoderFps);

        VeLivePusherDef.VeLiveAudioEncoderConfiguration audioEncoderCfg = new VeLivePusherDef.VeLiveAudioEncoderConfiguration();
        //  音频编码采样率  
        if (mConfig.mAudioEncoderSampleRate == 32000) {
            audioEncoderCfg.setSampleRate(VeLiveAudioSampleRate32000);
        } else if (mConfig.mAudioEncoderSampleRate == 48000) {
            audioEncoderCfg.setSampleRate(VeLiveAudioSampleRate48000);
        } else {
            audioEncoderCfg.setSampleRate(VeLiveAudioSampleRate44100);
        }

        //  音频编码通道数  
        if (mConfig.mAudioEncoderChannel == 1) {
            audioEncoderCfg.setChannel(VeLiveAudioChannelMono);
        } else {
            audioEncoderCfg.setChannel(VeLiveAudioChannelStereo);
        }
        
        try {
            //  音频编码码率  
            audioEncoderCfg.setBitrate(mConfig.mAudioEncoderKBitrate);
            //  配置视频编码  
            mLivePusher.setVideoEncoderConfiguration(videoEncoderCfg);
            //  配置音频编码  
            mLivePusher.setAudioEncoderConfiguration(audioEncoderCfg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private VeLivePusherDef.VeLiveVideoResolution getEncodeVideoResolution() {
        if (Integer.max(mConfig.mVideoEncoderWidth, mConfig.mVideoEncoderHeight) >= 1920) {
            return VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution1080P;
        } else if (Integer.max(mConfig.mVideoEncoderWidth, mConfig.mVideoEncoderHeight) >= 1280) {
            return VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution720P;
        } else if (Integer.max(mConfig.mVideoEncoderWidth, mConfig.mVideoEncoderHeight) >= 960) {
            return VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution540P;
        } else if (Integer.max(mConfig.mVideoEncoderWidth, mConfig.mVideoEncoderHeight) >= 640) {
            return VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution360P;
        }
        return VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution720P;
    }

    private void releaseLivePusher() {
        if (mLivePusher == null) {
            return;
        }
        mLivePusher.stopAudioCapture();
        mLivePusher.stopVideoCapture();
        mLivePusher.stopPush();
        mLivePusher.release();
        mLivePusher = null;
    }

    private void pushVideoFrameToLivePusher(com.ss.bytertc.engine.video.VideoFrame videoFrame) {
        if (mIsPushing) {
            final int width = videoFrame.getWidth();
            final int height = videoFrame.getHeight();
            final int chromaHeight = (height + 1) / 2;
            final int chromaWidth = (width + 1) / 2;
            int bufferSize = width * height + chromaWidth * chromaHeight * 2;
            final ByteBuffer dstBuffer = ByteBuffer.allocateDirect(bufferSize);

            YuvHelper.I420Rotate(videoFrame.getPlaneData(0), videoFrame.getPlaneStride(0),
                    videoFrame.getPlaneData(1), videoFrame.getPlaneStride(1),
                    videoFrame.getPlaneData(2), videoFrame.getPlaneStride(2),
                    dstBuffer,width, height,videoFrame.getRotation().value());

            dstBuffer.position(0);
            VeLiveVideoFrame videoFrame1 = new VeLiveVideoFrame(width, height, System.currentTimeMillis() * 1000, dstBuffer);
            mLivePusher.pushExternalVideoFrame(videoFrame1);
            videoFrame1.release();
        }
    }

    private void pushAudioFrameToLivePusher(ByteBuffer byteBuffer, int sampleRate, int channels, int bitWidth, int samples, long timestamp) {
        if (mIsPushing) {
            VeLiveAudioFrame audioFrame = new VeLiveAudioFrame(VeLivePusherDef.VeLiveAudioSampleRate.fromValue(sampleRate, VeLiveAudioSampleRate44100),
                    VeLivePusherDef.VeLiveAudioChannel.fromValue(channels, VeLiveAudioChannelStereo),
                    timestamp,
                    byteBuffer);
            mLivePusher.pushExternalAudioFrame(audioFrame);
        }
    }

    private final IRTCVideoEventHandler mRTCVideoEventHandler = new IRTCVideoEventHandler() {
        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.i(TAG, warn + "");
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.i(TAG, err + "");
        }

        @Override
        public void onNetworkTypeChanged(int type) {
            super.onNetworkTypeChanged(type);
        }
    };

    private void initRtcEngine() {
        mRTCVideo = RTCVideo.createRTCVideo(Env.getApplicationContext(), mAppId, mRTCVideoEventHandler, null, null);
        mRTCVideo.setLocalVideoMirrorType(MirrorType.MIRROR_TYPE_RENDER_AND_ENCODER);
    }

    private void releaseRtcEngine() {
        if (mRTCRoom != null) {
            mRTCRoom.destroy();
            mRTCRoom = null;
        }
        if (mRTCVideo != null) {
            RTCVideo.destroyRTCVideo();
            mRTCVideo = null;
        }
    }

    private void registerVideoListener() {
        mVideoFrameListener = new IVideoSink() {
            @Override
            public void onFrame(com.ss.bytertc.engine.video.VideoFrame frame) {
                if (!mIsTranscoding) {
                    pushVideoFrameToLivePusher(frame);
                }
                frame.release();
            }

            @Override
            public int getRenderElapse() { return 0; }
        };
        mRTCVideo.setLocalVideoSink(StreamIndex.STREAM_INDEX_MAIN, mVideoFrameListener, IVideoSink.PixelFormat.I420);
    }

    private void unregisterVideoListener() {
        mRTCVideo.setLocalVideoSink(StreamIndex.STREAM_INDEX_MAIN, null, IVideoSink.PixelFormat.I420);
    }

    private long timestamp = 0;
    private void registerAudioListener() {
        mAudioFrameListener = new IAudioFrameObserver() {
            @Override
            public void onRecordAudioFrame(IAudioFrame audioFrame) {
                if (!mIsTranscoding) {
                    if (timestamp == 0) {
                        timestamp = System.currentTimeMillis() * 1000;
                    } else {
                        int bitWidth = (audioFrame.frame_type() == FRAME_TYPE_PCM16 ? 16 : 0);
                        int time = audioFrame.getDataBuffer().limit() * 1000 / audioFrame.channel().value() / (bitWidth / 8) / audioFrame.sample_rate().value();
                        timestamp = timestamp + time * 1000;
                    }
                    pushAudioFrameToLivePusher(
                            audioFrame.getDataBuffer(), audioFrame.sample_rate().value(), audioFrame.channel().value(),
                            16, audioFrame.data_size() / 2, timestamp);
                }
                audioFrame.release();
            }

            @Override
            public void onPlaybackAudioFrame(IAudioFrame audioFrame) {}

            @Override
            public void onRemoteUserAudioFrame(RemoteStreamKey stream_info, IAudioFrame audioFrame) {}

            @Override
            public void onMixedAudioFrame(IAudioFrame audioFrame) {}
        };
        mRTCVideo.enableAudioFrameCallback(AudioFrameCallbackMethod.AUDIO_FRAME_CALLBACK_RECORD,
                new AudioFormat(AudioSampleRate.fromId(mConfig.mAudioCaptureSampleRate),
                        AudioChannel.fromId(mConfig.mAudioCaptureChannel)));
        mRTCVideo.registerAudioFrameObserver(mAudioFrameListener);
    }

    private void unregisterAudioListener() {
        mRTCVideo.registerAudioFrameObserver(null);
    }

    private RtcRoomEventHandlerAdapter mIRtcRoomEventHandler = new RtcRoomEventHandlerAdapter() {
        @Override
        public void onLeaveRoom(RTCRoomStats stats) {
            mIsTranscoding = false;
        }

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            if (TextUtils.equals(uid, mUserId) && TextUtils.equals(roomId, mRoomId)) {
                mRoomListener.onJoinRoom(uid, state);
            }
        }

        @Override
        public void onUserJoined(UserInfo userInfo, int elapsed) {
            mRoomListener.onUserJoined(userInfo.getUid());
        }

        @Override
        public void onUserLeave(String uid, int reason) {
            mRoomListener.onUserLeave(uid);
        }

        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            mRoomListener.onUserPublishStream(uid, type);
        }

        @Override
        public void onUserUnpublishStream(String uid, MediaStreamType type, StreamRemoveReason reason) {
            mRoomListener.onUserUnPublishStream(uid, type, reason);
        }
    };

    private void joinRoom(String roomId, String userId, String token, VeLiveAnchorManager.IListener roomListener) {
        Log.d(TAG, String.format("joinRoom: %s %s %s", roomId, userId, token));
        mRoomListener = roomListener;
        if (mRTCVideo == null) {
            return;
        }
        mRTCRoom = mRTCVideo.createRTCRoom(roomId);
        mRTCRoom.setRTCRoomEventHandler(mIRtcRoomEventHandler);
        mUserId = userId;
        mRoomId = roomId;
        UserInfo userInfo = new UserInfo(userId, null);
        RTCRoomConfig roomConfig = new RTCRoomConfig(ChannelProfile.CHANNEL_PROFILE_COMMUNICATION,
                true, true, true);
        mRTCRoom.joinRoom(token, userInfo, roomConfig);
    }

    private void leaveRoom() {
        Log.d(TAG, "leaveRoom");
        stopPushStreamToCDN("");
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom = null;
        }
    }

    private final IMixedStreamObserver mIMixedStreamObserver = new IMixedStreamObserver() {
        @Override
        public boolean isSupportClientPushStream() {
            return false;
        }

        @Override
        public void onMixingEvent(ByteRTCStreamMixingEvent eventType, String taskId, ByteRTCTranscoderErrorCode error, MixedStreamType mixType) {
            Log.i(TAG, "onStreamMixingEvent" + ", eventType:" + eventType + ", taskId:" + taskId + ", error:" + error + ", mixType:" + mixType);
            if (eventType == STREAM_MIXING_START_FAILED) {
                startPush(mPushUrl);
            }
        }

        @Override
        public void onMixingAudioFrame(String taskId, byte[] audioFrame, int frameNum, long timeStampMs) {

        }

        @Override
        public void onMixingVideoFrame(String taskId, com.ss.bytertc.engine.video.VideoFrame videoFrame) {

        }

        @Override
        public void onMixingDataFrame(String taskId, byte[] dataFrame, long time) {

        }

        @Override
        public void onCacheSyncVideoFrames(String taskId, String[] userIds, com.ss.bytertc.engine.video.VideoFrame[] videoFrame, byte[][] dataFrame, int count) {

        }
    };

    private void startPushMixedStreamToCDN(String taskId, MixedStreamConfig.MixedStreamLayoutConfig layout) {
        //  合流转推配置  
        mMixedStreamConfig = MixedStreamConfig.defaultMixedStreamConfig();
        mMixedStreamConfig.setRoomID(mRoomId);
        mMixedStreamConfig.setUserID(mUserId);
        mMixedStreamConfig.setPushURL(mPushUrl);
        mMixedStreamConfig.setExpectedMixingType(STREAM_MIXING_BY_SERVER);

        //  设置视频编码参数, 必须和直播推流设置保持一致  
        MixedStreamConfig.MixedStreamVideoConfig videoConfig = mMixedStreamConfig.getVideoConfig();
        //  分辨率宽  
        videoConfig.setWidth(mConfig.mVideoEncoderWidth);
        //  分辨率高  
        videoConfig.setHeight(mConfig.mVideoEncoderHeight);
        // fps
        videoConfig.setFps(mConfig.mVideoEncoderFps);
        //  比特率  
        videoConfig.setBitrate(mConfig.mVideoEncoderKBitrate);
        mMixedStreamConfig.setVideoConfig(videoConfig);

        //  设置音频编码参数, 必须和on直播推流设置保持一致  
        MixedStreamConfig.MixedStreamAudioConfig audioConfig = mMixedStreamConfig.getAudioConfig();
        //  音频采样率  
        audioConfig.setSampleRate(mConfig.mAudioEncoderSampleRate);
        //  通道数  
        audioConfig.setChannels(mConfig.mAudioEncoderChannel);
        //  比特率 k  
        audioConfig.setBitrate(mConfig.mAudioEncoderKBitrate);
        //  配置音频参数  
        mMixedStreamConfig.setAudioConfig(audioConfig);

        //  配置信息  
        mMixedStreamConfig.setLayout(layout);
        mIsTranscoding = true;
        //  开始合流转推  
        mRTCVideo.startPushMixedStreamToCDN(taskId, mMixedStreamConfig, mIMixedStreamObserver);
    }

    private void stopPushStreamToCDN(String taskId) {
        Log.d(TAG, "stopPushStreamToCDN");
        mIsTranscoding = false;
        mMixedStreamConfig = null;
        if (mRTCVideo != null) {
            mRTCVideo.stopPushStreamToCDN(taskId);
        }
    }

}
