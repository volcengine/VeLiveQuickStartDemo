/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.link;

import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ss.bytertc.engine.live.MixedStreamConfig;
import com.ss.bytertc.engine.video.IVideoEffect;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveEffectHelper;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.features.interact.manager.VeLiveAnchorManager;
import com.pandora.common.env.Env;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.StreamRemoveReason;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePushURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class LinkAnchorActivity extends AppCompatActivity {
    private final String TAG = "LinkAnchorActivity";
    public static final String ROOM_ID = "LinkAnchorActivity_ROOM_ID";
    public static final String USER_ID = "LinkAnchorActivity_USER_ID";
    public static final String TOKEN = "LinkAnchorActivity_TOKEN";

    private LinearLayout mRemoteLinearLayout;
    private EditText mUrlText;
    private TextView mInfoView;
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
        mInfoView = findViewById(R.id.push_info_text_view);
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
    }

    private void startPush(String url) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "Please config push url");
            return;
        }
        //  开始推流  
        mAnchorManager.startPush(url);
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
        ToggleButton toggleButton = (ToggleButton)view;
        if (mUrlText.getText().toString().isEmpty()) {
            toggleButton.setChecked(false);
            mInfoView.setText(R.string.config_stream_name_tip);
            return;
        }
        if (toggleButton.isChecked()) {
            view.setEnabled(false);
            mInfoView.setText(R.string.Generate_Push_Url_Tip);
            VeLiveURLGenerator.genPushUrl(VeLiveSDKHelper.LIVE_APP_NAME, mUrlText.getText().toString(), new VeLiveURLGenerator.VeLiveURLCallback<VeLivePushURLModel>() {
                @Override
                public void onSuccess(VeLiveURLRootModel<VeLivePushURLModel> model) {
                    view.setEnabled(true);
                    mInfoView.setText("");
                    startPush(model.result.getRtmpPushUrl());
                }

                @Override
                public void onFailed(VeLiveURLError error) {
                    view.setEnabled(true);
                    mInfoView.setText(error.message);
                    toggleButton.setChecked(false);
                }
            });
        } else {
            //  停止推流  
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

        //  特效鉴权License路径，请根据工程配置查找正确的路径  
        String licPath = VeLiveEffectHelper.getLicensePath("xxx.licbag");
        //  特效模型资源包路径  
        String algoModePath = VeLiveEffectHelper.getModelPath();
        if (!VeLiveSDKHelper.isFileExists(licPath) || !VeLiveSDKHelper.isFileExists(algoModePath)) {
            return;
        }
        IVideoEffect effect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  检查License  
        //  设置特效算法包  
        effect.initCVResource(licPath, algoModePath);

        if (effect.enableVideoEffect() != 0) {
            Log.e(TAG, "enable effect error");
        }
    }

    public void beautyControl(View view) {
        //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
        String beautyPath = VeLiveEffectHelper.getBeautyPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(beautyPath)) {
            return;
        }
        IVideoEffect effect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置美颜美型特效资源包  
        effect.setEffectNodes(Collections.singletonList(beautyPath));
        //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
        effect.updateEffectNode(beautyPath, "whiten", 0.5F);
    }

    public void filterControl(View view) {
        //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
        String filterPath = VeLiveEffectHelper.getFilterPathByName("xxx");;
        if (!VeLiveSDKHelper.isFileExists(filterPath)) {
            return;
        }
        IVideoEffect effect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置滤镜资源包路径  
        effect.setColorFilter(filterPath);
        //  设置滤镜特效强度  
        effect.setColorFilterIntensity(0.5F);
    }

    public void stickerControl(View view) {
        //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
        String stickerPath = VeLiveEffectHelper.getStickerPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(stickerPath)) {
            return;
        }
        IVideoEffect effect = mAnchorManager.getRTCVideo().getVideoEffectInterface();
        //  设置贴纸资源包路径  
        effect.appendEffectNodes(Collections.singletonList(stickerPath));
    }

    private MixedStreamConfig.MixedStreamLayoutConfig getTranscodingLayout() {
        MixedStreamConfig.MixedStreamLayoutConfig layout = new MixedStreamConfig.MixedStreamLayoutConfig();
        //  设置背景色  
        layout.setBackgroundColor("#000000");
        int guestIndex = 0;
        float density = getResources().getDisplayMetrics().density;
        float viewWidth = getResources().getDisplayMetrics().widthPixels / density;
        float viewHeight = getResources().getDisplayMetrics().heightPixels / density;

        double guestX = (viewWidth - 130.0) / viewWidth;
        double guestStartY = (viewHeight - 42.0) / viewHeight;
        MixedStreamConfig.MixedStreamLayoutRegionConfig[] regions = new MixedStreamConfig.MixedStreamLayoutRegionConfig[mUsersInRoom.size()];
        int pos = 0;
        for (String uid : mUsersInRoom) {
            MixedStreamConfig.MixedStreamLayoutRegionConfig region = new MixedStreamConfig.MixedStreamLayoutRegionConfig();
            region.setUserID(uid);
            region.setRoomID(mRoomID);
            region.setRenderMode(MixedStreamConfig.MixedStreamRenderMode.MIXED_STREAM_RENDER_MODE_HIDDEN);
            region.setIsLocalUser(Objects.equals(uid, mUserID));
            if (region.getIsLocalUser()) { // 当前主播位置，仅供参考 
                region.setLocationX(0.0);
                region.setLocationY(0.0);
                region.setWidthProportion(1);
                region.setHeightProportion(1);
                region.setZOrder(0);
                region.setAlpha(1);
            } else { //  远端用户位置，仅供参考  
                //  130 是小窗的宽高， 8 是小窗的间距  
                region.setLocationX(guestX);
                region.setLocationY(guestStartY - (130.0 * (guestIndex + 1) + guestIndex * 8) / viewHeight);
                region.setWidthProportion((130.0 / viewWidth));
                region.setHeightProportion((130.0 / viewHeight));
                region.setZOrder(1);
                region.setAlpha(1);
                guestIndex ++;
            }
            regions[pos++] = region;
        }
        layout.setRegions(regions);
        return layout;
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
                runOnUiThread(() -> stopInteractive());
                return;
            }
            mUsersInRoom.add(uid);
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
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
                mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
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
            mAnchorManager.updatePushMixedStreamToCDN(getTranscodingLayout());
        }
    };
}