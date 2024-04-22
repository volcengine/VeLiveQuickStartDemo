/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureMicrophone;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureExternal;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import android.support.v7.app.AppCompatActivity;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveDeviceCapture;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
import com.ss.avframework.live.VeLiveVideoFrame;
import com.ss.avframework.utils.TimeUtils;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePushURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

public class PushCustomActivity extends AppCompatActivity {
    private final String TAG = "PushCustomActivity";
    private VeLiveDeviceCapture mDeviceCapture;
    private VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_custom);
        mInfoView = findViewById(R.id.push_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        setupLivePusher();
        setupCustomCamera();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  销毁推流器  
        //  业务处理时，尽量不要放到此处释放，推荐放到退出直播间时释放。  
        mLivePusher.release();
        mDeviceCapture.stop();
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
        mLivePusher.startVideoCapture(VeLiveVideoCaptureExternal);
        //  开始音频采集  
        mLivePusher.startAudioCapture(VeLiveAudioCaptureMicrophone);
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
                    //  开始推流，推流地址支持： rtmp 协议，http 协议（RTM）  
                    mLivePusher.startPush(model.result.getRtmpPushUrl());
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
            mLivePusher.stopPush();
        }
    }

    private final VeLivePusherObserver pusherObserver = new VeLivePusherObserver() {
        @Override
        public void onError(int code, int subCode, String msg) {
            Log.e(TAG, "Error" + code + subCode + msg);
        }

        @Override
        public void onStatusChange(VeLivePusherDef.VeLivePusherStatus status) {
            Log.d(TAG, "Status" + status);
        }
    };

    private final VeLivePusherDef.VeLivePusherStatisticsObserver statisticsObserver = new VeLivePusherDef.VeLivePusherStatisticsObserver() {
        @Override
        public void onStatistics(VeLivePusherDef.VeLivePusherStatistics statistics) {
            runOnUiThread(() -> mInfoView.setText(VeLiveSDKHelper.getPushInfoString(statistics)));
        }
    };
    private void setupCustomCamera() {
        mDeviceCapture = new VeLiveDeviceCapture();
        mDeviceCapture.start((data, width, height) -> {
            VeLiveVideoFrame videoFrame =  new VeLiveVideoFrame(width, height, TimeUtils.currentTimeUs(), data);
            videoFrame.setReleaseCallback(videoFrame::release);
            mLivePusher.pushExternalVideoFrame(videoFrame);
        });
    }
}