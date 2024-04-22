/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ss.videoarch.liveplayer.VeLivePlayerAudioFrame;
import com.ss.videoarch.liveplayer.VeLivePlayerConfiguration;
import com.ss.videoarch.liveplayer.VeLivePlayerDef;
import com.ss.videoarch.liveplayer.VeLivePlayerError;
import com.ss.videoarch.liveplayer.VeLivePlayerObserver;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;
import com.ss.videoarch.liveplayer.VeLivePlayerVideoFrame;
import com.ss.videoarch.liveplayer.VideoLiveManager;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.features.advanced.pip.FloatingVideoService;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePullURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

public class PictureInPictureActivity extends AppCompatActivity {
    private final String TAG = "PictureInPicture";
    private VeLivePlayer mLivePlayer;
    private TextView mInfoView;

    private EditText mUrlText;
    private Button mSwitchPip;
    private boolean mIsPipOn;

    private SurfaceView mSurfaceView;
    private FrameLayout mViewContainer;
    private final int mOverlayRequestCode = 1001;
    private FloatingVideoService mFloatingVideoService = null;
    private boolean mFloatingVideoServiceIsConnected;

    private final ServiceConnection mFloatingVideoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFloatingVideoServiceIsConnected = true;
            mFloatingVideoService = ((FloatingVideoService.Binder)service).getService();
            mViewContainer.removeView(mSurfaceView);
            mFloatingVideoService.addSurfaceView(mSurfaceView, v -> startPictureInPicture(mSwitchPip));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mFloatingVideoServiceIsConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_in_picture);

        mInfoView = findViewById(R.id.pull_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mSwitchPip = findViewById(R.id.picture_in_picture_control);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mViewContainer = findViewById(R.id.surface_container);
        mViewContainer.addView(mSurfaceView);
        setupLivePlayer();
    }

    private void requestSettingCanDrawOverlays() {
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) { // 8.0以上
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, mOverlayRequestCode);
        } else if (sdkInt >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, mOverlayRequestCode);
        } else {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  销毁直播播放器  
        //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
        mLivePlayer.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == mOverlayRequestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.pip_authorization_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.pip_authorization_success, Toast.LENGTH_SHORT).show();
                }
            }
        }
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

    public void startPictureInPicture(View view) {
        if (!mIsPipOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.pip_require_authorization, Toast.LENGTH_SHORT).show();
                    requestSettingCanDrawOverlays();
                    return;
                }
            } else {
                Toast.makeText(this, R.string.pip_not_support, Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(getApplicationContext(), FloatingVideoService.class);
            bindService(intent, mFloatingVideoServiceConnection, BIND_AUTO_CREATE);
            mSwitchPip.setText(R.string.Stop_Picture_In_Picture);
        } else {
            if (mFloatingVideoServiceIsConnected && mFloatingVideoService != null) {
                mFloatingVideoService.removeSurfaceView();
                mViewContainer.addView(mSurfaceView);
                unbindService(mFloatingVideoServiceConnection);
            }
            mSwitchPip.setText(R.string.Start_Picture_In_Picture);
        }
        mIsPipOn = !mIsPipOn;
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
        public void onFirstVideoFrameRender(VeLivePlayer veLivePlayer, boolean b) {

        }

        @Override
        public void onFirstAudioFrameRender(VeLivePlayer veLivePlayer, boolean b) {

        }

        @Override
        public void onStallStart(VeLivePlayer veLivePlayer) {

        }

        @Override
        public void onStallEnd(VeLivePlayer veLivePlayer) {

        }

        @Override
        public void onVideoRenderStall(VeLivePlayer veLivePlayer, long l) {

        }

        @Override
        public void onAudioRenderStall(VeLivePlayer veLivePlayer, long l) {

        }

        @Override
        public void onResolutionSwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution, VeLivePlayerError veLivePlayerError, VeLivePlayerDef.VeLivePlayerResolutionSwitchReason veLivePlayerResolutionSwitchReason) {

        }

        @Override
        public void onVideoSizeChanged(VeLivePlayer veLivePlayer, int i, int i1) {

        }

        @Override
        public void onReceiveSeiMessage(VeLivePlayer veLivePlayer, String s) {

        }

        @Override
        public void onMainBackupSwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerStreamType veLivePlayerStreamType, VeLivePlayerError veLivePlayerError) {

        }

        @Override
        public void onPlayerStatusUpdate(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerStatus veLivePlayerStatus) {

        }

        @Override
        public void onSnapshotComplete(VeLivePlayer veLivePlayer, Bitmap bitmap) {

        }

        @Override
        public void onRenderVideoFrame(VeLivePlayer veLivePlayer, VeLivePlayerVideoFrame veLivePlayerVideoFrame) {

        }

        @Override
        public void onRenderAudioFrame(VeLivePlayer veLivePlayer, VeLivePlayerAudioFrame veLivePlayerAudioFrame) {

        }

        @Override
        public void onStreamFailedOpenSuperResolution(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {

        }
    };

}