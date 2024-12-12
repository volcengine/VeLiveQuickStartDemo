/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.link;

import static com.ss.bytertc.engine.type.MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO;

import android.support.v7.app.AppCompatActivity;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ss.bytertc.engine.video.IVideoEffect;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveEffectHelper;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.features.interact.manager.VeLiveAudienceManager;
import com.pandora.common.env.Env;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.StreamRemoveReason;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePullURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class LinkAudienceActivity extends AppCompatActivity {
    private final String TAG = "LinkAudienceActivity";
    public static final String ROOM_ID = "LinkAudienceActivity_ROOM_ID";
    public static final String USER_ID = "LinkAudienceActivity_USER_ID";
    public static final String TOKEN = "LinkAudienceActivity_TOKEN";
    private LinearLayout mRemoteLinearLayout;
    private EditText mUrlText;
    private TextView mInfoView;
    private String mRoomID;
    private String mUserID;
    private String mToken;

    //  主播推流预览View  
    private TextureView mLocalView;
    //  拉流视图  
    private SurfaceView mPreviewView;
    //  参与连麦的用户列表  
    private ArrayList<String> mUsersInRoom;
    //  连麦过程中远端用户视图列表  
    private HashMap<String, TextureView> mRemoteUserViews;

    private VeLiveAudienceManager mAudienceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_link_audience);
        mUrlText = findViewById(R.id.url_input_view);
        mInfoView = findViewById(R.id.pull_info_text_view);
        mLocalView = findViewById(R.id.render_view);
        mPreviewView = findViewById(R.id.player_view);
        mRemoteLinearLayout = findViewById(R.id.guest_linear_layout);
        mRoomID = getIntent().getStringExtra(ROOM_ID);
        mUserID = getIntent().getStringExtra(USER_ID);
        mToken = getIntent().getStringExtra(TOKEN);
        setupAudienceManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudienceManager != null) {
            VeLiveAudienceManager.destroy();
            mAudienceManager = null;
        }
    }

    private void setupAudienceManager() {
        mUsersInRoom = new ArrayList<>();
        mRemoteUserViews = new HashMap<>();
        mAudienceManager = VeLiveAudienceManager.create(VeLiveSDKHelper.RTC_APPID, mUserID);
    }

    private void startPlay(String url) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "Please config pull url");
            return;
        }
        mPreviewView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mAudienceManager.setPlayerVideoView(mPreviewView.getHolder());
        mAudienceManager.startPlay(url);
    }

    private void stopPlay() {
        mAudienceManager.stopPlay();
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
        //  设置推流配置  
        mAudienceManager.setConfig(new VeLiveAudienceManager.Config());
        mAudienceManager.setLocalVideoView(mLocalView);
        mPreviewView.setVisibility(View.INVISIBLE);
        mLocalView.setVisibility(View.VISIBLE);
        mAudienceManager.startInteract(mRoomID, mToken, mAudienceListener);
        setupEffectSDK();
    }

    private void stopInteractive() {
        clearInteractUsers();
        mLocalView.setVisibility(View.INVISIBLE);
        mPreviewView.setVisibility(View.VISIBLE);
        mAudienceManager.stopInteract();
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

    private VeLiveAudienceManager.IListener mAudienceListener = new VeLiveAudienceManager.IListener() {
        @Override
        public void onUserJoined(String uid) {
        }

        @Override
        public void onUserLeave(String uid) {

        }

        @Override
        public void onJoinRoom(int state) {
            if (state != 0) { //  加入房间失败  
                runOnUiThread(() -> stopInteractive());
            }
        }

        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            if (type != RTC_MEDIA_STREAM_TYPE_AUDIO) {
                runOnUiThread(() -> {
                    //  添加远端用户view， 仅供参考  
                    TextureView textureView = getTextureView(uid);
                    mRemoteLinearLayout.addView(textureView, 0);
                    mAudienceManager.setRemoteVideoView(uid, textureView);
                });
            }
        }

        @Override
        public void onUserUnPublishStream(String uid, MediaStreamType type, StreamRemoveReason reason) {
            if (type != RTC_MEDIA_STREAM_TYPE_AUDIO) {
                runOnUiThread(() -> {
                    mUsersInRoom.remove(uid);
                    TextureView textureView = mRemoteUserViews.get(uid);
                    mRemoteLinearLayout.removeView(textureView);
                    mRemoteUserViews.remove(uid);
                    //  移除远端视图  
                    mAudienceManager.setRemoteVideoView(uid, null);
                });
            }
        }
    };

    public void playControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (mUrlText.getText().toString().isEmpty()) {
            toggleButton.setChecked(false);
            mInfoView.setText(R.string.config_stream_name_tip);
            return;
        }
        if (toggleButton.isChecked()) {
            view.setEnabled(false);
            mInfoView.setText(R.string.Generate_Pull_Url_Tip);
            VeLiveURLGenerator.genPullUrl(VeLiveSDKHelper.LIVE_APP_NAME, mUrlText.getText().toString(), new VeLiveURLGenerator.VeLiveURLCallback<VeLivePullURLModel>() {
                @Override
                public void onSuccess(VeLiveURLRootModel<VeLivePullURLModel> model) {
                    view.setEnabled(true);
                    mInfoView.setText("");
                    startPlay(model.result.getUrl("flv"));
                }

                @Override
                public void onFailed(VeLiveURLError error) {
                    view.setEnabled(true);
                    mInfoView.setText(error.message);
                    toggleButton.setChecked(false);
                }
            });
        } else {
            stopPlay();
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
        mAudienceManager.sendSeiMessage("anchor_test_sei", 20);
    }

    private void setupEffectSDK() {
        if (mAudienceManager.getRTCVideo() == null) {
            return;
        }
        //  特效鉴权License路径，请根据工程配置查找正确的路径  
        String licPath = VeLiveEffectHelper.getLicensePath("xxx.licbag");
        //  特效模型资源包路径  
        String algoModePath = VeLiveEffectHelper.getModelPath();
        if (!VeLiveSDKHelper.isFileExists(licPath) || !VeLiveSDKHelper.isFileExists(algoModePath)) {
            return;
        }
        IVideoEffect effect = mAudienceManager.getRTCVideo().getVideoEffectInterface();
        //  检查License  
        //  设置特效算法包  
        effect.initCVResource(licPath, algoModePath);

        if (effect.enableVideoEffect() != 0) {
            Log.e(TAG, "enable effect error");
        }
    }

    public void beautyControl(View view) {
        if (mAudienceManager.getRTCVideo() == null) {
            return;
        }
        //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
        String beautyPath = VeLiveEffectHelper.getBeautyPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(beautyPath)) {
            return;
        }
        IVideoEffect effect = mAudienceManager.getRTCVideo().getVideoEffectInterface();
        //  设置美颜美型特效资源包  
        effect.setEffectNodes(Collections.singletonList(beautyPath));
        //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
        effect.updateEffectNode(beautyPath, "whiten", 0.5F);
    }

    public void filterControl(View view) {
        if (mAudienceManager.getRTCVideo() == null) {
            return;
        }
        //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
        String filterPath = VeLiveEffectHelper.getFilterPathByName("xxx");;
        if (!VeLiveSDKHelper.isFileExists(filterPath)) {
            return;
        }
        IVideoEffect effect = mAudienceManager.getRTCVideo().getVideoEffectInterface();
        //  设置滤镜资源包路径  
        effect.setColorFilter(filterPath);
        //  设置滤镜特效强度  
        effect.setColorFilterIntensity(0.5F);
    }

    public void stickerControl(View view) {
        if (mAudienceManager.getRTCVideo() == null) {
            return;
        }
        //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
        String stickerPath = VeLiveEffectHelper.getStickerPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(stickerPath)) {
            return;
        }
        IVideoEffect effect = mAudienceManager.getRTCVideo().getVideoEffectInterface();
        //  设置贴纸资源包路径  
        effect.appendEffectNodes(Collections.singletonList(stickerPath));
    }
}