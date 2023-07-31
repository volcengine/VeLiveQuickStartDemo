/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.link;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveEffectHelper;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.features.interact.manager.VeLiveAnchorManager;
import com.pandora.common.env.Env;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.live.LiveTranscoding;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.StreamRemoveReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class LinkAnchorActivity extends AppCompatActivity {
    public static final String ROOM_ID = "LinkAnchorActivity_ROOM_ID";
    public static final String USER_ID = "LinkAnchorActivity_USER_ID";
    public static final String TOKEN = "LinkAnchorActivity_TOKEN";

    private LinearLayout mRemoteLinearLayout;
    private EditText mUrlText;
    private String mRoomID;
    private String mUserID;
    private String mToken;
    //  主播推流预览View  
    private TextureView mLocalView;
    //  参与连麦的用户列表  
    private ArrayList <String> mUsersInRoom;
    //  连麦过程中远端用户视图列表  
    private HashMap<String, TextureView> mRemoteUserViews;
    //  主播+连麦管理器  
    private VeLiveAnchorManager mAnchorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_anchor);
        mRemoteLinearLayout = findViewById(R.id.guest_linear_layout);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
        mRoomID = getIntent().getStringExtra(ROOM_ID);
        mUserID = getIntent().getStringExtra(USER_ID);
        mToken = getIntent().getStringExtra(TOKEN);
        mLocalView = findViewById(R.id.render_view);
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
        mRemoteUserViews = new HashMap<>();
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
        Object[] remoteViews = mRemoteUserViews.values().toArray();
        for (Object remoteView : remoteViews) {
            if (remoteView instanceof View) {
                mRemoteLinearLayout.removeView((View)remoteView);
            }
        }
        mRemoteUserViews.clear();
    }

    private void startInteractive() {
        clearInteractUsers();
        mAnchorManager.startInteract(mRoomID, mToken, anchorListener);
    }

    private void stopInteractive() {
        clearInteractUsers();
        mAnchorManager.stopInteract();
    }


    public void pushControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            startPush();
        } else {
            stopPush();
        }
    }

    public void interactControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            startInteractive();
        } else {
            stopInteractive();
        }
    }

    public void seiControl(View view) {
        mAnchorManager.sendSeiMessage("anchor_test_sei", 20);
    }

    private void setupEffectSDK() {
        RTCVideo rtcVideo = mAnchorManager.getRTCVideo();
        //  特效鉴权License路径，请根据工程配置查找正确的路径  
        String licPath = VeLiveEffectHelper.getLicensePath("xxx.licbag");
        //  特效模型资源包路径  
        String algoModePath = VeLiveEffectHelper.getModelPath();
        //  检查License  
        rtcVideo.checkVideoEffectLicense(Env.getApplicationContext(), licPath);
        //  设置特效算法包  
        rtcVideo.setVideoEffectAlgoModelPath(algoModePath);
        if (rtcVideo.enableEffectBeauty(true) != 0) {
            Log.e("VeLiveQuickStartDemo", "enable effect error");
        }
    }

    public void beautyControl(View view) {
        //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
        String beautyPath = VeLiveEffectHelper.getBeautyPathByName("xxx");
        //  设置美颜美型特效资源包  

        mAnchorManager.getRTCVideo().setVideoEffectNodes(Collections.singletonList(beautyPath));
        //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
        mAnchorManager.getRTCVideo().updateVideoEffectNode(beautyPath, "whiten", 0.5F);
    }

    public void filterControl(View view) {
        //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
        String filterPath = VeLiveEffectHelper.getFilterPathByName("xxx");;
        //  设置滤镜资源包路径  
        mAnchorManager.getRTCVideo().setVideoEffectColorFilter(filterPath);
        //  设置滤镜特效强度  
        mAnchorManager.getRTCVideo().setVideoEffectColorFilterIntensity(0.5F);
    }

    public void stickerControl(View view) {
        //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
        String stickerPath = VeLiveEffectHelper.getStickerPathByName("xxx");
        //  设置贴纸资源包路径  
        mAnchorManager.getRTCVideo().appendVideoEffectNodes(Collections.singletonList(stickerPath));
    }

    private LiveTranscoding.Layout getTranscodingLayout() {
        LiveTranscoding.Layout.Builder builder = new LiveTranscoding.Layout.Builder();
        //  设置背景色  
        builder.backgroundColor("#000000");
        int guestIndex = 0;
        float density = getResources().getDisplayMetrics().density;
        float viewWidth = getResources().getDisplayMetrics().widthPixels / density;
        float viewHeight = getResources().getDisplayMetrics().heightPixels / density;

        double guestX = (viewWidth - 130.0) / viewWidth;
        double guestStartY = (viewHeight - 42.0) / viewHeight;
        for (String uid : mUsersInRoom) {
            LiveTranscoding.Region region = new LiveTranscoding.Region();
            region.uid(uid);
            region.roomId(mRoomID);
            region.renderMode(LiveTranscoding.TranscoderRenderMode.RENDER_HIDDEN);
            region.setLocalUser(Objects.equals(uid, mUserID));
            if (region.isLocalUser()) { // 当前主播位置，仅供参考 
                region.position(0.0, 0.0);
                region.size(1, 1);
                region.zorder(0);
                region.alpha(1);
            } else { //  远端用户位置，仅供参考  
                //  130 是小窗的宽高， 8 是小窗的间距  
                region.position(guestX, guestStartY - (130.0 * (guestIndex + 1) + guestIndex * 8) / viewHeight);
                region.size((130.0 / viewWidth), (130.0 / viewHeight));
                region.zorder(1);
                region.alpha(1);
                guestIndex ++;
            }
            builder.addRegion(region);
        }
        return builder.builder();
    }

    private TextureView getTextureView(String uid) {
        TextureView textureView = mRemoteUserViews.get(uid);
        if (textureView == null) {
            textureView = new TextureView(this);
            int width = (int)(130 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width);
            layoutParams.weight = 1;
            layoutParams.topMargin = 8;
            textureView.setLayoutParams(layoutParams);
            mRemoteUserViews.put(uid, textureView);
        }
        return textureView;
    }

    private VeLiveAnchorManager.IListener anchorListener = new VeLiveAnchorManager.IListener() {
        @Override
        public void onUserJoined(String uid) {
            mUsersInRoom.add(uid);
            mAnchorManager.updateLiveTranscoding(getTranscodingLayout());
        }

        @Override
        public void onUserLeave(String uid) {
            mUsersInRoom.remove(uid);
            mAnchorManager.updateLiveTranscoding(getTranscodingLayout());
        }

        @Override
        public void onJoinRoom(String uid, int state) {
            if (state != 0) { //  加入房间失败  
                runOnUiThread(() -> stopInteractive());
                return;
            }
            mUsersInRoom.add(uid);
            mAnchorManager.updateLiveTranscoding(getTranscodingLayout());
        }

        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            if (type == MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO) {
                return;
            }
            try {
                TextureView textureView = getTextureView(uid);
                runOnUiThread(() -> {
                    mRemoteLinearLayout.addView(textureView, 0);
                });
                //  配置远端视图  
                mAnchorManager.setRemoteVideoView(uid, textureView);
                //  更新混流布局  
                mAnchorManager.updateLiveTranscoding(getTranscodingLayout());
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
            TextureView textureView = mRemoteUserViews.get(uid);
            mRemoteLinearLayout.removeView(textureView);
            mRemoteUserViews.remove(uid);
            //  移除远端视图  
            mAnchorManager.setRemoteVideoView(uid, null);
            //  更新混流布局  
            mAnchorManager.updateLiveTranscoding(getTranscodingLayout());
        }
    };
}