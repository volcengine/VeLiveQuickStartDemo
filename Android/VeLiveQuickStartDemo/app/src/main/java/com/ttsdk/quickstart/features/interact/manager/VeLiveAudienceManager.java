/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.manager;


import static com.ss.bytertc.engine.VideoCanvas.RENDER_MODE_HIDDEN;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerStatus.VeLivePlayerStatusError;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerStatus.VeLivePlayerStatusPaused;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerStatus.VeLivePlayerStatusPlaying;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerStatus.VeLivePlayerStatusPrepared;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerStatus.VeLivePlayerStatusStopped;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.pandora.common.env.Env;
import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.MirrorType;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.handler.IRTCVideoEventHandler;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.RTCRoomStats;
import com.ss.bytertc.engine.type.StreamRemoveReason;
import com.ss.bytertc.engine.video.VideoCaptureConfig;
import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ss.videoarch.liveplayer.VeLivePlayerAudioFrame;
import com.ss.videoarch.liveplayer.VeLivePlayerConfiguration;
import com.ss.videoarch.liveplayer.VeLivePlayerDef;
import com.ss.videoarch.liveplayer.VeLivePlayerError;
import com.ss.videoarch.liveplayer.VeLivePlayerObserver;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;
import com.ss.videoarch.liveplayer.VeLivePlayerVideoFrame;
import com.ss.videoarch.liveplayer.VideoLiveManager;

public class VeLiveAudienceManager {
    private static final String TAG = "VeLiveAudienceManager";

    private IListener mRoomListener;
    private static VeLiveAudienceManager mInstance;
    private final String mAppId;
    private String mUserId;
    private String mPullUrl;
    private VeLiveAudienceManager.Config mConfig;

    private VeLivePlayer mLivePlayer;                             //  播放器引擎  

    private RTCVideo mRTCVideo;
    private RTCRoom mRTCRoom; //  RTC 房间对象  
    private String mRoomId;
    private TextureView mLocalView;
    private int streamRetryCount;

    public static class Config {
        public int mVideoCaptureWidth = 720;
        public int mVideoCaptureHeight = 1280;
        public int mVideoCaptureFps = 30;

        public int mAudioCaptureSampleRate = 44100;
        public int mAudioCaptureChannel = 2;

        public int mVideoEncoderWidth = 720;
        public int mVideoEncoderHeight = 1280;
        public int mVideoEncoderFps = 15;
        public int mVideoEncoderKBitrate = 1600;
        public boolean mIsVideoHardwareEncoder = true;

        public int mAudioEncoderSampleRate = 44100;
        public int mAudioEncoderChannel = 2;
        public int mAudioEncoderKBitrate = 32;
    }

    public interface IListener {
        void onUserJoined(String uid);
        void onUserLeave(String uid);
        void onJoinRoom(int state);
        void onUserPublishStream(String uid, MediaStreamType type);
        void onUserUnPublishStream(String uid, MediaStreamType type, StreamRemoveReason reason);
    }

    private VeLiveAudienceManager(String appId, String userId) {
        mAppId = appId;
        mUserId = userId;
        initLivePlayer();
    }

    public static VeLiveAudienceManager create(String appId, String userId) {
        if (mInstance == null) {
            if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(userId)) {
                mInstance = new VeLiveAudienceManager(appId, userId);
            }
        }
        return mInstance;
    }

    public static void destroy() {
        if (mInstance != null) {
            mInstance.stopInteract();
            mInstance.stopPlay();

            mInstance.releaseLivePlayer();
        }
        mInstance = null;
    }

    public RTCVideo getRTCVideo() {
        return mRTCVideo;
    }
    public void setConfig(VeLiveAudienceManager.Config config) {
        mConfig = config;
    }

    public void setPlayerVideoView(SurfaceHolder holder) {

        //  设置view  
        mLivePlayer.setSurfaceHolder(holder);
        //  设置渲染模式  
        mLivePlayer.setRenderFillMode(VeLivePlayerFillModeAspectFill);
    }

    public void startPlay(String url) {
        if (mLivePlayer == null) {
            return;
        }
        streamRetryCount = 0;
        mPullUrl = url;
        mLivePlayer.setPlayUrl(mPullUrl);
        mLivePlayer.play();
    }

    public void stopPlay() {
        if (mLivePlayer == null) {
            return;
        }
        mLivePlayer.stop();
    }

    public void setLocalVideoView(TextureView renderView) {
        mLocalView = renderView;
        if (mRTCVideo == null) {
            return;
        }
        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = renderView;
        videoCanvas.uid = mUserId;
        videoCanvas.isScreen = false;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        //  设置本地视频渲染视图  
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
        Log.d(TAG, "setLocalVideoView");
    }

    public void setRemoteVideoView(String uid, TextureView renderView) {
        if (mRTCVideo != null) {
            VideoCanvas canvas = new VideoCanvas(renderView, RENDER_MODE_HIDDEN, mRoomId, uid, false);
            mRTCVideo.setRemoteVideoCanvas(uid, StreamIndex.STREAM_INDEX_MAIN, canvas);
        }
    }

    public void startAudioCapture() {
        if (mRTCVideo != null) {
            mRTCVideo.startAudioCapture();
        }
    }

    public void startVideoCapture() {
        if (mRTCVideo != null) {
            VideoEncoderConfig config = new VideoEncoderConfig(
                    mConfig.mVideoEncoderWidth,
                    mConfig.mVideoEncoderHeight,
                    mConfig.mVideoEncoderFps,
                    mConfig.mVideoEncoderKBitrate * 1000);
            mRTCVideo.setVideoEncoderConfig(config);
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

    public void startInteract(String roomId, String token, VeLiveAudienceManager.IListener roomListener) {
        stopPlay();
        initRtcEngine();
        startAudioCapture();
        startVideoCapture();
        setLocalVideoView(mLocalView);
        mRoomListener = roomListener;
        //  设置用户信息，加入房间，开始连麦  
        joinRoom(roomId, mUserId, token, mRoomListener);
    }

    public void stopInteract() {
        leaveRoom();
        stopVideoCapture();
        stopAudioCapture();
        setLocalVideoView(null);
        startPlay(mPullUrl);
        releaseRtcEngine();
    }

    private void initLivePlayer() {
        //  创建播放器  
        mLivePlayer = new VideoLiveManager(Env.getApplicationContext());

        //  设置播放器回调  
        mLivePlayer.setObserver(mLivePlayerObserver);

        //  播放器基础设置  
        VeLivePlayerConfiguration config = new VeLivePlayerConfiguration();
        config.enableStatisticsCallback = true;
        config.enableLiveDNS = true;
        mLivePlayer.setConfig(config);
    }

    public void sendSeiMessage(String msg, int repeat) {
        mRTCVideo.sendSEIMessage(StreamIndex.STREAM_INDEX_MAIN,
                TextUtils.isEmpty(msg) ? new byte[0] : msg.getBytes(), repeat);
    }

    private void releaseLivePlayer() {
        if (mLivePlayer != null) {
            mLivePlayer.destroy();
            mLivePlayer = null;
        }
        if (mLocalView != null) {
            mLocalView = null;
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
        VideoCaptureConfig captureConfig = new VideoCaptureConfig();
        captureConfig.width = mConfig.mVideoCaptureWidth;
        captureConfig.height = mConfig.mVideoCaptureHeight;
        captureConfig.frameRate = mConfig.mVideoCaptureFps;
        mRTCVideo.setVideoCaptureConfig(captureConfig);
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

    private RtcRoomEventHandlerAdapter mIRtcRoomEventHandler = new RtcRoomEventHandlerAdapter() {
        @Override
        public void onLeaveRoom(RTCRoomStats stats) {
        }

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            if (TextUtils.equals(uid, mUserId) && TextUtils.equals(roomId, mRoomId)) {
                mRoomListener.onJoinRoom(state);
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

    private void joinRoom(String roomId, String userId, String token, VeLiveAudienceManager.IListener roomListener) {
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
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom = null;
        }
    }

    private VeLivePlayerObserver mLivePlayerObserver = new VeLivePlayerObserver() {
        @Override
        public void onError(VeLivePlayer player, VeLivePlayerError error) {
            Log.i("VeLivePlayer-", "Player Error: " + error.mErrorCode);
        }

        @Override
        public void onFirstVideoFrameRender(VeLivePlayer player, boolean isFirstFrame) {
            Log.i("VeLivePlayer-", "First Video Frame: " + isFirstFrame);
        }

        @Override
        public void onFirstAudioFrameRender(VeLivePlayer player, boolean isFirstFrame) {
            Log.i("VeLivePlayer-", "First Audio Frame: " + isFirstFrame);
        }

        @Override
        public void onStallStart(VeLivePlayer player) {
            Log.i("VeLivePlayer-", "Stall Start");
        }

        @Override
        public void onStallEnd(VeLivePlayer player) {
            Log.i("VeLivePlayer-", "Stall End");
        }

        @Override
        public void onVideoRenderStall(VeLivePlayer player, long stallTime) {
            Log.i("VeLivePlayer-", "Video Render Stall：" + stallTime);
        }

        @Override
        public void onAudioRenderStall(VeLivePlayer player, long stallTime) {
            Log.i("VeLivePlayer-", "Audio Render Stall：" + stallTime);
        }

        @Override
        public void onResolutionSwitch(VeLivePlayer player, VeLivePlayerDef.VeLivePlayerResolution resolution, VeLivePlayerError error, VeLivePlayerDef.VeLivePlayerResolutionSwitchReason reason) {
            Log.i("VeLivePlayer-", "Resolution Switch：" + resolution);
        }

        @Override
        public void onVideoSizeChanged(VeLivePlayer player, int width, int height) {
            Log.i("VeLivePlayer-", "Video Size Changed width:" + width + " height:" + height);
        }

        @Override
        public void onReceiveSeiMessage(VeLivePlayer player, String message) {
            Log.i("VeLivePlayer-", "SEI:" + message);
        }

        @Override
        public void onMainBackupSwitch(VeLivePlayer player, VeLivePlayerDef.VeLivePlayerStreamType streamType, VeLivePlayerError error) {
            Log.i("VeLivePlayer-", "Main Backup Switch");
        }

        @Override
        public void onPlayerStatusUpdate(VeLivePlayer player, VeLivePlayerDef.VeLivePlayerStatus status) {
            if(status == VeLivePlayerStatusPrepared) {
                Log.i("VeLivePlayer-", "State:" + "Prepared");
            }else if(status == VeLivePlayerStatusPlaying) {
                Log.i("VeLivePlayer-", "State:" + "Playing");
            }else if(status == VeLivePlayerStatusPaused) {
                Log.i("VeLivePlayer-", "State:" + "Paused");
            }else if(status == VeLivePlayerStatusStopped) {
                Log.i("VeLivePlayer-", "State:" + "Stopped");
            }else if(status == VeLivePlayerStatusError) {
                Log.i("VeLivePlayer-", "State:" + "Error");
            }
        }

        @Override
        public void onStatistics(VeLivePlayer player, VeLivePlayerStatistics statistics) {
            Log.i("VeLivePlayer-", "statistics url:" + statistics.url);
            Log.i("VeLivePlayer-", "statistics width:" + statistics.width);
            Log.i("VeLivePlayer-", "statistics height:" + statistics.height);
            Log.i("VeLivePlayer-", "statistics isHardWareDecode:" + statistics.isHardwareDecode);
            Log.i("VeLivePlayer-", "statistics videoCodec:" + statistics.videoCodec);
            Log.i("VeLivePlayer-", "statistics bitrate:" + statistics.bitrate);
            Log.i("VeLivePlayer-", "statistics fps:" + statistics.fps);
        }

        @Override
        public void onSnapshotComplete(VeLivePlayer player, Bitmap bitmap) {
            Log.i("VeLivePlayer-", "Snapshot");
        }

        @Override
        public void onRenderVideoFrame(VeLivePlayer player, VeLivePlayerVideoFrame videoFrame) {
            Log.i("VeLivePlayer-", "Render Video Frame");
        }

        @Override
        public void onRenderAudioFrame(VeLivePlayer player, VeLivePlayerAudioFrame audioFrame) {
            Log.i("VeLivePlayer-", "Render Audio Frame");
        }

        @Override
        public void onStreamFailedOpenSuperResolution(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {

        }
    };
}
