/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureMicrophone;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureFrontCamera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
/*
码率自适应推流

 本文件展示如何集成推流器码率自适应能力，默认开启
 1、初始化推流器 API:
  VeLivePusherConfiguration config = new VeLivePusherConfiguration();
  config.setContext(this);
  mLivePusher = config.build();
 2、初始化编码配置 API: VeLivePusherDef.VeLiveVideoEncoderConfiguration encoderConfiguration = new VeLivePusherDef.VeLiveVideoEncoderConfiguration();
 3、配置编码码率（仅供参考） API:
        encoderConfiguration.setBitrate(1200);
        encoderConfiguration.setMaxBitrate(1900);
        encoderConfiguration.setMinBitrate(800);
 4、设置编码配置 API: mLivePusher.setVideoEncoderConfiguration(encoderConfiguration);
 5、设置码率自适应灵敏度 API: mLivePusher.setProperty("VeLiveKeySetBitrateAdaptStrategy", "NORMAL");
 6、设置预览视图 API: mLivePusher.setRenderView(findViewById(R.id.render_view));
 7、打开麦克风采集 API: mLivePusher.startAudioCapture(VeLiveAudioCaptureMicrophone);
 8、打开摄像头采集 API: mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 9、开始推流 API：mLivePusher.startPush("rtmp://push.example.com/rtmp");
 */
public class PushAutoBitrateActivity extends AppCompatActivity {

    private VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_auto_bitrate);
        mInfoView = findViewById(R.id.push_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
        setupLivePusher();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  销毁推流器  
        //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
        mLivePusher.release();
    }

    private void setupLivePusher() {
        //  推流配置  
        VeLivePusherConfiguration config = new VeLivePusherConfiguration();
        //  配置上下文  
        config.setContext(this);
        //  失败重连次数  
        config.setReconnectCount(10);
        //  视频编码配置  
        VeLivePusherDef.VeLiveVideoEncoderConfiguration encoderConfiguration = new VeLivePusherDef.VeLiveVideoEncoderConfiguration();
        //  设置视频分辨率，内部会根据分辨率设置最佳码率参数  
        encoderConfiguration.setResolution(VeLivePusherDef.VeLiveVideoResolution.VeLiveVideoResolution720P);
        //  视频编码初始化码率（仅供参考）  
        encoderConfiguration.setBitrate(1200);
        //  视频编码最大码率（仅供参考）  
        encoderConfiguration.setMaxBitrate(1900);
        //  视频编码最小码率（仅供参考）  
        encoderConfiguration.setMinBitrate(800);
        //  创建推流器  
        mLivePusher = config.build();
        //  配置编码  
        mLivePusher.setVideoEncoderConfiguration(encoderConfiguration);
        //  调整码率自适应灵敏度，CLOSE，NORMAL，SENSITIVE，MORE_SENSITIVE  
        mLivePusher.setProperty("VeLiveKeySetBitrateAdaptStrategy", "NORMAL");
        //  配置预览视图  
        mLivePusher.setRenderView(findViewById(R.id.render_view));
        //  设置推流器回调  
        mLivePusher.setObserver(pusherObserver);
        //  设置周期性信息回调  
        mLivePusher.setStatisticsObserver(statisticsObserver, 3);
        //  开始视频采集  
        mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
        //  开始音频采集  
        mLivePusher.startAudioCapture(VeLiveAudioCaptureMicrophone);
    }

    public void pushControl(View view) {
        ToggleButton toggleButton = (ToggleButton)view;
        if (mUrlText.getText().toString().isEmpty()) {
            Log.e("VeLiveQuickStartDemo", "Please Config Url");
            return;
        }
        if (toggleButton.isChecked()) {
            //  开始推流，推流地址支持： rtmp 协议，http 协议（RTM）  
            mLivePusher.startPush(mUrlText.getText().toString());
        } else {
            //  停止推流  
            mLivePusher.stopPush();
        }
    }

    private VeLivePusherObserver pusherObserver = new VeLivePusherObserver() {
        @Override
        public void onError(int code, int subCode, String msg) {
            Log.d("VeLiveQuickStartDemo", "Error" + code + subCode + msg);
        }

        @Override
        public void onStatusChange(VeLivePusherDef.VeLivePusherStatus status) {
            Log.d("VeLiveQuickStartDemo", "Status" + status);
        }
    };

    private VeLivePusherDef.VeLivePusherStatisticsObserver statisticsObserver = new VeLivePusherDef.VeLivePusherStatisticsObserver() {
        @Override
        public void onStatistics(VeLivePusherDef.VeLivePusherStatistics statistics) {
            runOnUiThread(() -> mInfoView.setText(VeLiveSDKHelper.getPushInfoString(statistics)));
        }
    };
}