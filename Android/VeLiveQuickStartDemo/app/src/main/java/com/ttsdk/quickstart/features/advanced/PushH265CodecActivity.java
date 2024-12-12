/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureMicrophone;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureFrontCamera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePushURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

/*
码率自适应推流

 本文件展示如何集成推流器码率自适应能力，默认开启
 1、初始化推流器 API:
  VeLivePusherConfiguration config = new VeLivePusherConfiguration();
  config.setContext(this);
  mLivePusher = config.build();
 2、初始化编码配置 API: VeLivePusherDef.VeLiveVideoEncoderConfiguration encoderConfiguration = new VeLivePusherDef.VeLiveVideoEncoderConfiguration();
 3、配置编码类型 API:
        encoderConfiguration.setCodec(VeLivePusherDef.VeLiveVideoCodec.VeLiveVideoCodecByteVC1);
 4、设置编码配置 API: mLivePusher.setVideoEncoderConfiguration(encoderConfiguration);
 5、设置预览视图 API: mLivePusher.setRenderView(findViewById(R.id.render_view));
 6、打开麦克风采集 API: mLivePusher.startAudioCapture(VeLiveAudioCaptureMicrophone);
 7、打开摄像头采集 API: mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 8、开始推流 API：mLivePusher.startPush("rtmp://push.example.com/rtmp");
 */
public class PushH265CodecActivity extends AppCompatActivity {
    private final String TAG = "PushH265CodecActivity";
    private VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_push_h265_codec);
        mInfoView = findViewById(R.id.push_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
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
        //  编码类型  
        encoderConfiguration.setCodec(VeLivePusherDef.VeLiveVideoCodec.VeLiveVideoCodecByteVC1);
        //  创建推流器  
        mLivePusher = config.build();
        //  配置编码  
        mLivePusher.setVideoEncoderConfiguration(encoderConfiguration);
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

    private VeLivePusherObserver pusherObserver = new VeLivePusherObserver() {
        @Override
        public void onError(int code, int subCode, String msg) {
            Log.d(TAG, "Error" + code + subCode + msg);
        }

        @Override
        public void onStatusChange(VeLivePusherDef.VeLivePusherStatus status) {
            Log.d(TAG, "Status" + status);
        }
    };

    private VeLivePusherDef.VeLivePusherStatisticsObserver statisticsObserver = new VeLivePusherDef.VeLivePusherStatisticsObserver() {
        @Override
        public void onStatistics(VeLivePusherDef.VeLivePusherStatistics statistics) {
            runOnUiThread(() -> mInfoView.setText(VeLiveSDKHelper.getPushInfoString(statistics)));
        }
    };
}