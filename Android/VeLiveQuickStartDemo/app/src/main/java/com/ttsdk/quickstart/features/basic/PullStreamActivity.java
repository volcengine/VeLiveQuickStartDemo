/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.basic;

import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFill;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeAspectFit;
import static com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode.VeLivePlayerFillModeFullFill;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ss.videoarch.liveplayer.VeLivePlayerAudioFrame;
import com.ss.videoarch.liveplayer.VeLivePlayerConfiguration;
import com.ss.videoarch.liveplayer.VeLivePlayerDef;
import com.ss.videoarch.liveplayer.VeLivePlayerDef.VeLivePlayerFillMode;
import com.ss.videoarch.liveplayer.VeLivePlayerError;
import com.ss.videoarch.liveplayer.VeLivePlayerObserver;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;
import com.ss.videoarch.liveplayer.VeLivePlayerVideoFrame;
import com.ss.videoarch.liveplayer.VideoLiveManager;

/*
直播拉流
 本文件展示如何集成直播拉流功能
 1、初始化推流器 API: mLivePlayer = new VideoLiveManager(this);
 2、配置推流器 API: mLivePlayer.setConfig(new VeLivePlayerConfiguration());
 3、配置渲染视图 API：mLivePlayer.setSurfaceHolder(mSurfaceView.getHolder());
 4、配置播放地址 API: mLivePlayer.setPlayUrl("http://pull.example.com/pull.flv");
 5、开始播放 API: mLivePlayer.play();
 参考文档：https://www.volcengine.com/docs/6469/95393
 */
public class PullStreamActivity extends AppCompatActivity {

    private VeLivePlayer mLivePlayer;
    private TextView mTextView;

    private EditText mUrlText;

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_stream);
        mTextView = findViewById(R.id.pull_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PULL_URL);
        mSurfaceView = findViewById(R.id.render_view);
        setupLivePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  销毁直播播放器  
        //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
        mLivePlayer.destroy();
    }

    private void setupLivePlayer() {
        //  创建直播播放器  
        mLivePlayer = new VideoLiveManager(this);

        //  设置播放器回调  
        mLivePlayer.setObserver(mplayerObserver);

        //  配置播放器  
        //  更多配置参考：https://www.volcengine.com/docs/6469/95393  
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


        //  配置 RTM 低延时地址参考以下代码  
        /*
        // 配置 RTM 主地址
        VeLivePlayerStreamData.VeLivePlayerStream playStreamRTM = new VeLivePlayerStreamData.VeLivePlayerStream();
        playStreamRTM.url = Constant.LIVE_PULL_RTM_URL;
        playStreamRTM.format = VeLivePlayerFormatRTM;
        playStreamRTM.resolution = VeLivePlayerResolutionOrigin;
        playStreamRTM.streamType = VeLivePlayerStreamTypeMain;

        // 配置 Flv 降级地址
        VeLivePlayerStreamData.VeLivePlayerStream playStreamFLV = new VeLivePlayerStreamData.VeLivePlayerStream();
        playStreamFLV.url = Constant.LIVE_PULL_URL;
        playStreamFLV.format = VeLivePlayerFormatFLV;
        playStreamFLV.resolution = VeLivePlayerResolutionOrigin;
        playStreamFLV.streamType = VeLivePlayerStreamTypeMain;

        // 创建 VeLivePlayerStreamData
        VeLivePlayerStreamData streamData = new VeLivePlayerStreamData();
        List<VeLivePlayerStreamData.VeLivePlayerStream> streamList = new ArrayList<>();
        // 添加 RTM 主地址
        streamList.add(playStreamRTM);
        // 添加 FLV 降级地址
        streamList.add(playStreamFLV);

        streamData.mainStreamList = streamList;
        streamData.defaultFormat = VeLivePlayerFormatRTM;
        streamData.defaultProtocol = VeLivePlayerFormatTLS;
        mLivePlayer.setPlayStreamData(streamData);
        */

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

    public void fillModeControl(View view) {
        showFillModeDialog();
    }

    private void changeFillMode(VeLivePlayerFillMode fillMode) {
        //  设置填充模式  
        mLivePlayer.setRenderFillMode(fillMode);
    }

    public void muteControl(View view) {
        //  静音/取消静音  
        ToggleButton toggleButton = (ToggleButton) view;
        mLivePlayer.setMute(toggleButton.isChecked());
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

    private void showFillModeDialog() {
        final String[] items = {
                getString(R.string.Pull_Stream_Fill_Mode_Alert_AspectFill),
                getString(R.string.Pull_Stream_Fill_Mode_Alert_AspectFit),
                getString(R.string.Pull_Stream_Fill_Mode_Alert_FullFill)};
        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(this);
        singleChoiceDialog.setTitle(getString(R.string.Pull_Stream_Fill_Mode_Alert_Title));
        singleChoiceDialog.setItems(items, ((dialog, which) -> {
            if (which == 0) {
                changeFillMode(VeLivePlayerFillModeAspectFill);
            } else if (which == 1) {
                changeFillMode(VeLivePlayerFillModeAspectFit);
            } else if (which == 2) {
                changeFillMode(VeLivePlayerFillModeFullFill);
            }
        }));
        singleChoiceDialog.show();
    }

}