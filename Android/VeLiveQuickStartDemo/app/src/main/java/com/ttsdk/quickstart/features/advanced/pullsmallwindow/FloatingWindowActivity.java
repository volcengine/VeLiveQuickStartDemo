/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced.pullsmallwindow;

import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ss.videoarch.liveplayer.VeLivePlayerConfiguration;
import com.ss.videoarch.liveplayer.VeLivePlayerDef;
import com.ss.videoarch.liveplayer.VeLivePlayerError;
import com.ss.videoarch.liveplayer.VeLivePlayerObserver;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;
import com.ss.videoarch.liveplayer.VideoLiveManager;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePullURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;
import com.ttsdk.quickstart.solution.FloatingWindow.FloatingWindowHelper;
import com.ttsdk.quickstart.solution.FloatingWindow.IFloatingWindowHelper;
import com.ttsdk.quickstart.solution.FloatingWindow.VeLiveRefMgr;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.ttsdk.quickstart.R;

public class FloatingWindowActivity extends AppCompatActivity {
    private TextView mInfoView;
    private EditText mUrlText;
    private SurfaceView mSurfaceView;
    private FrameLayout mViewContainer;
    private ToggleButton mSwitchFloatingWindow;

    private float mAspectRatio = -1f;
    private VeLivePlayer mLivePlayer;

    private String mTag;
    private boolean mIsLiving;

    public static final String EXTRA_DATA_KEY_PLAYER = "extra_data_key_player";
    public static final String EXTRA_DATA_KEY_TAG = "extra_data_key_tag";
    public static final String EXTRA_DATA_KEY_STREAM_NAME = "extra_data_key_stream_name";

    private static class LivePlayerWrapper implements VeLiveRefMgr.IObject {
        VeLivePlayer mLivePlayer;
        private LivePlayerWrapper(VeLivePlayer player) {
            mLivePlayer = player;
        }

        @Override
        public Object getId() {
            return mLivePlayer;
        }

        @Override
        public void onDecRef() {
            mLivePlayer.destroy();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_small_window);
        mInfoView = findViewById(R.id.pull_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mViewContainer = findViewById(R.id.surface_container);
        mViewContainer.addView(mSurfaceView);
        mSwitchFloatingWindow = findViewById(R.id.switchSmallWindowBtn);
        mSwitchFloatingWindow.setOnClickListener(view -> switchFloatingWindow());
        init();
    }

    private void init() {
        mTag = getIntent().getStringExtra(EXTRA_DATA_KEY_TAG);
        if (FloatingWindowHelper.getInstance().isOpen()) { //  从悬浮窗状态进入此Activity
            Map<String, Object> extraData = FloatingWindowHelper.getInstance().getExtraData();
            IFloatingWindowHelper.Config config = FloatingWindowHelper.getInstance().getConfig();
            if (extraData != null && config != null) {
                VeLivePlayer livePlayer = (VeLivePlayer)extraData.get(EXTRA_DATA_KEY_PLAYER);
                String tag = (String)extraData.get(EXTRA_DATA_KEY_TAG);
                String streamName = (String)extraData.get(EXTRA_DATA_KEY_STREAM_NAME);
                //  如果mTag为null（新创建的实例），直接使用extraData中的tag
                if (mTag == null) {
                    mTag = tag;
                }
                if (livePlayer != null) { //  恢复直播间继续播放
                    mLivePlayer = livePlayer;
                    mLivePlayer.setObserver(mplayerObserver);
                    mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());
                    ((ToggleButton)findViewById(R.id.play_control)).setChecked(true);
                    mSwitchFloatingWindow.setChecked(false);
                    mUrlText.setText(streamName);
                    mAspectRatio = config.aspectRatio;
                } else {
                    setupLivePlayer();
                }
            } else {
                setupLivePlayer();
            }
            //  关闭悬浮窗
            FloatingWindowHelper.getInstance().closeFloatingWindow(this);
        } else {
            setupLivePlayer();
        }
        VeLiveRefMgr.addRef(new LivePlayerWrapper(mLivePlayer));

        FloatingWindowHelper.getInstance().setEventListener(new IFloatingWindowHelper.Listener() {
            @Override
            public boolean onRequestOverlayPermission() {
                Toast.makeText(FloatingWindowActivity.this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                return true; // 允许申请权限
            }

            @Override
            public void onOpenFloatingWindowResult(int errCode, Map<String, Object> extraData) {
                if (errCode == IFloatingWindowHelper.Listener.ERR_NO) {
                    VeLiveRefMgr.addRef(new LivePlayerWrapper((VeLivePlayer)extraData.get(EXTRA_DATA_KEY_PLAYER)));
                    return;
                }
                mSwitchFloatingWindow.setChecked(false);
                if (errCode == IFloatingWindowHelper.Listener.ERR_NOT_SUPPORT) {
                    Toast.makeText(FloatingWindowActivity.this, "设备不支持悬浮窗", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FloatingWindowActivity.this, "开启失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCloseFloatingWindow(Map<String, Object> extraData) {
                VeLiveRefMgr.decRef(new LivePlayerWrapper((VeLivePlayer)extraData.get(EXTRA_DATA_KEY_PLAYER)));
            }

            @Override
            public void onClickFloatingWindow(Context context) {
                //  触发点击事件时，打开原创建悬浮窗的页面
                try {
                    Class<?> cls = Class.forName(FloatingWindowActivity.class.getName());
                    Intent intent = new Intent(context, cls);
                    intent.putExtra(EXTRA_DATA_KEY_TAG, mTag);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onUpdateSurfaceView(SurfaceView surfaceView) {
                mLivePlayer.setSurfaceHolder(surfaceView.getHolder());
            }
        });
    }

    private void switchFloatingWindow() {
        if (mSwitchFloatingWindow.isChecked()) {
            FloatingWindowActivity.this.finish();
        } else {
            mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());
            FloatingWindowHelper.getInstance().closeFloatingWindow(this);
        }
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        // 处理从悬浮窗返回的情况
//        setIntent(intent);
//        mTag = intent.getStringExtra(EXTRA_DATA_KEY_TAG);
//        // 确保播放器的SurfaceHolder被正确设置
//        if (mLivePlayer != null) {
//            mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());
//        }
//        // 关闭悬浮窗
//        FloatingWindowHelper.getInstance().closeFloatingWindow(this);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsLiving) { // 从后台恢复
            // 确保播放器的SurfaceHolder被正确设置
            if (mLivePlayer != null) {
                mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());
            }
            //  关闭悬浮窗
            FloatingWindowHelper.getInstance().closeFloatingWindow(this);
            return;
        }
        mIsLiving = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLivePlayer.isPlaying()) {
            Map<String, Object> extraData = new HashMap<>();
            extraData.put(EXTRA_DATA_KEY_PLAYER, mLivePlayer);
            extraData.put(EXTRA_DATA_KEY_TAG, mTag);
            extraData.put(EXTRA_DATA_KEY_STREAM_NAME, mUrlText.getText().toString());
            FloatingWindowHelper.getInstance().openFloatingWindow(this, new IFloatingWindowHelper.Config(mAspectRatio), extraData);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VeLiveRefMgr.decRef(new LivePlayerWrapper(mLivePlayer));
    }

    private void setupLivePlayer() {
        //  创建直播播放器
        mLivePlayer = new VideoLiveManager(this);

        //  设置播放器回调
        mLivePlayer.setObserver(mplayerObserver);

        //  配置播放器
        VeLivePlayerConfiguration config = new VeLivePlayerConfiguration();
        //  是否开启周期性信息回调
        config.enableStatisticsCallback = true;
        //  周期性信息回调间隔
        config.statisticsCallbackInterval = 1;
        //  是否开启内部DNS解析
        config.enableLiveDNS = true;

        //  配置拉流播放器
        mLivePlayer.setConfig(config);

        //  设置预览视图
        mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());

        //  设置渲染填充模式
        mLivePlayer.setRenderFillMode(VeLivePlayerFillModeAspectFill);
    }
//    public void playControl(View view) {
//        ToggleButton toggleButton = (ToggleButton) view;
////        if (mUrlText.getText().toString().isEmpty()) {
////            toggleButton.setChecked(false);
////            mInfoView.setText(R.string.config_stream_name_tip);
////            return;
////        }
//        if (toggleButton.isChecked()) {
//            view.setEnabled(false);
//            mLivePlayer.setPlayUrl("https://pull.ysymh.cn/live/8877.flv");
////            mLivePlayer.setPlayUrl(mUrlText.getText().toString());
//            //  开始播放
//            mLivePlayer.play();
//        } else {
//            //  停止播放
//            mLivePlayer.stop();
//        }
//    }

    public void playControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (mUrlText.getText().toString().isEmpty()) {
            toggleButton.setChecked(false);
            mInfoView.setText("请输入流名称");
            return;
        }
        if (toggleButton.isChecked()) {
            view.setEnabled(false);
            mInfoView.setText("正在生成拉流地址...");
            VeLiveURLGenerator.genPullUrl(VeLiveSDKHelper.LIVE_APP_NAME, mUrlText.getText().toString(), new VeLiveURLGenerator.VeLiveURLCallback<VeLivePullURLModel>() {
                @Override
                public void onSuccess(VeLiveURLRootModel<VeLivePullURLModel> model) {
                    view.setEnabled(true);
                    mInfoView.setText("");

                    //  设置播放地址，支持 rtmp、http、https 协议，flv、m3u8 格式的地址
                    mLivePlayer.setPlayUrl(model.result.getUrl("flv"));

                    //  开始播放
                    mLivePlayer.play();
                }

                @Override
                public void onFailed(VeLiveURLError error) {
                    view.setEnabled(true);
                    mInfoView.setText(error.message);
                    toggleButton.setChecked(false);
                }
            });
        } else {
            //  停止播放
            mLivePlayer.stop();
        }
    }

    private final VeLivePlayerObserver mplayerObserver = new VeLivePlayerObserver() {
        @Override
        public void onError(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {
            Log.e("VeLiveQuickStartDemo", "Player Error" + veLivePlayerError.mErrorMsg);
        }
        @Override
        public void onStatistics(VeLivePlayer veLivePlayer, VeLivePlayerStatistics veLivePlayerStatistics) {
            runOnUiThread(() -> mInfoView.setText(VeLiveSDKHelper.getPlaybackInfoString(veLivePlayerStatistics)));
        }

        @Override
        public void onVideoSizeChanged(VeLivePlayer veLivePlayer, int w, int h) {
            mAspectRatio = (float)w / h;
        }

        @Override
        public SwitchPermissionRequestResult shouldAutomaticallySwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution1, JSONObject jsonObject) {
            return SwitchPermissionRequestResult.APPROVED;
        }

        @Override
        public void didAutomaticallySwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution1, JSONObject jsonObject) {

        }
    };
}