/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.pk;

import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.ss.bytertc.engine.live.MixedStreamConfig;
import com.ss.bytertc.engine.video.IVideoEffect;
import com.ss.bytertc.engine.video.RTCVideoEffect;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveEffectHelper;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.features.interact.manager.VeLiveAnchorManager;
import com.pandora.common.env.Env;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.data.ForwardStreamInfo;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.StreamRemoveReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class PKAnchorActivity extends AppCompatActivity {
    public static final String ROOM_ID = "PKAnchorActivity_ROOM_ID";
    public static final String USER_ID = "PKAnchorActivity_USER_ID";
    public static final String TOKEN = "PKAnchorActivity_TOKEN";
    public static final String OTHER_ROOM_ID = "PKAnchorActivity_OTHER_ROOM_ID";
    public static final String OTHER_TOKEN = "PKAnchorActivity_OTHER_TOKEN";

    private EditText mUrlText;
    private String mRoomID;
    private String mUserID;
    private String mToken;
    private String mOtherRoomID;
    private String mOtherToken;

    //  主播推流预览View  
    private TextureView mLocalView;
    private LinearLayout mPKContainer;
    private TextureView mPKLocalView;
    private TextureView mPKOtherView;
    //  参与连麦的用户列表  
    private ArrayList<String> mUsersInRoom;
    //  主播+连麦管理器  
    private VeLiveAnchorManager mAnchorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pkanchor);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
        mRoomID = getIntent().getStringExtra(ROOM_ID);
        mUserID = getIntent().getStringExtra(USER_ID);
        mToken = getIntent().getStringExtra(TOKEN);
        mOtherRoomID = getIntent().getStringExtra(OTHER_ROOM_ID);
        mOtherToken = getIntent().getStringExtra(OTHER_TOKEN);
        mLocalView = findViewById(R.id.render_view);
        mPKContainer = findViewById(R.id.pk_container);
        mPKLocalView = findViewById(R.id.render_local_view);
        mPKOtherView = findViewById(R.id.render_other_view);
        setupAnchorManager();
        setupEffectSDK();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearInteractUsers();
        VeLiveAnchorManager.destroy();
        mAnchorManager = null;
    }

    private void setupAnchorManager() {
        mUsersInRoom = new ArrayList<>();
        mAnchorManager = VeLiveAnchorManager.create(VeLiveSDKHelper.RTC_APPID, mUserID);
        //  设置推流配置  
        mAnchorManager.setConfig(new VeLiveAnchorManager.Config());
        //  配置本地预览视图  
        mAnchorManager.setLocalVideoView(mLocalView);
        //  开启视频采集  
        mAnchorManager.startVideoCapture();
        //  开启音频采集  
        mAnchorManager.startAudioCapture();
        //  开始推流  
        startPush();
    }

    private void startPush() {
        if (mUrlText.getText().toString().isEmpty()) {
            Log.e("VeLiveQuickStartDemo", "Please config push url");
            return;
        }
        mPKContainer.setVisibility(View.INVISIBLE);
        mLocalView.setVisibility(View.VISIBLE);
        //  开始推流  
        mAnchorManager.startPush(mUrlText.getText().toString());
    }
    private void stopPush() {
        mAnchorManager.stopPush();
    }

    private void clearInteractUsers() {
        //  开始连麦  
        //  清空历史用户，业务逻辑处理  
        mUsersInRoom.clear();
    }

    private void startForward() {
        clearInteractUsers();
        ForwardStreamInfo streamInfo = new ForwardStreamInfo(mOtherRoomID ,mOtherToken);
        mAnchorManager.startForwardStream(Collections.singletonList(streamInfo));
    }

    private void stopForward() {
        clearInteractUsers();
        mAnchorManager.stopForwardStream();
    }

    private void startPK() {
        mLocalView.setVisibility(View.INVISIBLE);
        mPKContainer.setVisibility(View.VISIBLE);
        clearInteractUsers();
        mAnchorManager.setLocalVideoView(mPKLocalView);
        //  加入房间后，开始跨房转推  
        mAnchorManager.startInteract(mRoomID, mToken, anchorListener);
    }
    private void stopPK() {
        mLocalView.setVisibility(View.VISIBLE);
        mPKContainer.setVisibility(View.INVISIBLE);
        clearInteractUsers();
        stopForward();
        mAnchorManager.stopInteract();
        mAnchorManager.setLocalVideoView(mLocalView);
    }

    public void pushControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            startPush();
        } else {
            stopPush();
        }
    }

    public void pkControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            startPK();
        } else {
            stopPK();
        }
    }

    public void seiControl(View view) {
        mAnchorManager.sendSeiMessage("anchor_test_sei", 20);
    }

    private void setupEffectSDK() {
        RTCVideo rtcVideo = mAnchorManager.getRTCVideo();
        IVideoEffect rtcVideoEffect = rtcVideo.getVideoEffectInterface();
        //  特效鉴权License路径，请根据工程配置查找正确的路径  
        String licPath = VeLiveEffectHelper.getLicensePath("xxx.licbag");
        //  特效模型资源包路径  
        String algoModePath = VeLiveEffectHelper.getModelPath();
        if (!VeLiveSDKHelper.isFileExists(licPath)) {
            return;
        }
        //  检查License  
        //  设置特效算法包  
        rtcVideoEffect.initCVResource(licPath, algoModePath);
        if (rtcVideoEffect.enableVideoEffect() != 0) {
            Log.e("VeLiveQuickStartDemo", "enable effect error");
        }
    }

    public void beautyControl(View view) {
        //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
        String beautyPath = VeLiveEffectHelper.getBeautyPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(beautyPath)) {
            return;
        }
        IVideoEffect rtcVideoEffect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置美颜美型特效资源包  
        rtcVideoEffect.setEffectNodes(Collections.singletonList(beautyPath));
        //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
        rtcVideoEffect.updateEffectNode(beautyPath, "whiten", 0.5F);
    }

    public void filterControl(View view) {
        //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
        String filterPath = VeLiveEffectHelper.getFilterPathByName("xxx");;
        if (!VeLiveSDKHelper.isFileExists(filterPath)) {
            return;
        }
        IVideoEffect rtcVideoEffect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置滤镜资源包路径  
        rtcVideoEffect.setColorFilter(filterPath);
        //  设置滤镜特效强度  
        rtcVideoEffect.setColorFilterIntensity(0.5F);
    }

    public void stickerControl(View view) {
        //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
        String stickerPath = VeLiveEffectHelper.getStickerPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(stickerPath)) {
            return;
        }
        IVideoEffect rtcVideoEffect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置贴纸资源包路径  
        rtcVideoEffect.appendEffectNodes(Collections.singletonList(stickerPath));
    }

    private MixedStreamConfig.MixedStreamLayoutConfig getTranscodingLayout() {
        MixedStreamConfig.MixedStreamLayoutConfig layout = new MixedStreamConfig.MixedStreamLayoutConfig();
        //  设置背景色  
        layout.setBackgroundColor("#000000");
        int guestIndex = 0;
        float density = getResources().getDisplayMetrics().density;
        float viewWidth = getResources().getDisplayMetrics().widthPixels / density;
        float viewHeight = getResources().getDisplayMetrics().heightPixels / density;
        float pkViewWidth = (float) ((viewWidth - 8) * 0.5 / viewWidth);;
        float pkViewHeight =  260 / viewHeight;
        float pkViewY =  209 / viewHeight;

        MixedStreamConfig.MixedStreamLayoutRegionConfig[] regions = new MixedStreamConfig.MixedStreamLayoutRegionConfig[mUsersInRoom.size()];
        int pos = 0;
        for (String uid : mUsersInRoom) {
            MixedStreamConfig.MixedStreamLayoutRegionConfig region = new MixedStreamConfig.MixedStreamLayoutRegionConfig();
            region.setUserID(uid);
            region.setRoomID(mRoomID);
            region.setRenderMode(MixedStreamConfig.MixedStreamRenderMode.MIXED_STREAM_RENDER_MODE_HIDDEN);
            region.setIsLocalUser(Objects.equals(uid, mUserID));
            region.setLocationY(pkViewY);
            region.setWidthProportion(pkViewWidth);
            region.setHeightProportion(pkViewHeight);
            region.setAlpha(1);
            if (region.getIsLocalUser()) { // 当前主播位置，仅供参考 
                region.setLocationX(0.0);
                region.setZOrder(0);
            } else { //  远端用户位置，仅供参考  
                //  130 是小窗的宽高， 8 是小窗的间距  
                region.setLocationX((viewWidth * 0.5 + 8) / viewWidth);
                region.setZOrder(1);
                guestIndex ++;
            }
            regions[pos++] = region;
        }
        layout.setRegions(regions);
        return layout;
    }

    private VeLiveAnchorManager.IListener anchorListener = new VeLiveAnchorManager.IListener() {
        @Override
        public void onUserJoined(String uid) {
            mUsersInRoom.add(uid);
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
        }

        @Override
        public void onUserLeave(String uid) {
            mUsersInRoom.remove(uid);
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
        }

        @Override
        public void onJoinRoom(String uid, int state) {
            if (state != 0) { //  加入房间失败  
                runOnUiThread(() -> stopPK());
                return;
            }
            mUsersInRoom.add(uid);
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
            if (Objects.equals(uid, mUserID)) {
                startForward();
            }
        }

        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            if (type == MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO) {
                return;
            }
            try {
                //  配置远端视图  
                mAnchorManager.setRemoteVideoView(uid, mPKOtherView);
                //  更新混流布局  
                mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
            } catch (Exception e) {
                Log.e("VeLiveQuickStartDemo", e.toString());
            }
        }

        @Override
        public void onUserUnPublishStream(String uid, MediaStreamType type, StreamRemoveReason reason) {
            if (type == MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO) {
                return;
            }
            mUsersInRoom.remove(uid);
            //  移除远端视图  
            mAnchorManager.setRemoteVideoView(uid, null);
            //  更新混流布局  
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
        }
    };
}