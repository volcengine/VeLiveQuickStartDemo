/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

public class PictureInPictureActivity extends AppCompatActivity {
    private VeLivePlayer mLivePlayer;
    private TextView mTextView;

    private EditText mUrlText;
    private Button mSwitchPip;
    private boolean mIsPipOn;

    private SurfaceView mSurfaceView;
    private FrameLayout mViewContainer;
    private int mOverlayRequestCode = 1001;
    private FloatingVideoService mFloatingVideoService = null;
    private boolean mFloatingVideoServiceIsConnected;

    private ServiceConnection mFloatingVideoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFloatingVideoServiceIsConnected = true;
            mFloatingVideoService = ((FloatingVideoService.Binder)service).getService();
            mViewContainer.removeView(mSurfaceView);
            mFloatingVideoService.addSurfaceView(mSurfaceView);
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

        mTextView = findViewById(R.id.pull_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PULL_URL);
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
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
            }
            return;
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

        //  设置播放地址，支持 rtmp、http、https 协议，flv、m3u8 格式的地址  
        mLivePlayer.setPlayUrl(mUrlText.getText().toString());

        //  开始播放  
        mLivePlayer.play();
    }


    public void playControl(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            //  开始播放  
            mLivePlayer.play();
        } else {
            //  停止播放  
            mLivePlayer.stop();
        }
    }

    public void startPictureInPicture(View view) {
        if (!mIsPipOn) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
                requestSettingCanDrawOverlays();
                return;
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

    private VeLivePlayerObserver mplayerObserver = new VeLivePlayerObserver() {
        @Override
        public void onError(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {
            Log.e("VeLiveQuickStartDemo", "Player Error" + veLivePlayerError.mErrorMsg);
        }
        @Override
        public void onStatistics(VeLivePlayer veLivePlayer, VeLivePlayerStatistics veLivePlayerStatistics) {
            runOnUiThread(() -> mTextView.setText(VeLiveSDKHelper.getPlaybackInfoString(veLivePlayerStatistics)));
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