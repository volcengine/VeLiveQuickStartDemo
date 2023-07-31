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
import com.ss.avframework.live.VeLiveVideoFrame;

/*
摄像头推流
 本文件展示如何集成摄像头推流功能
 1、初始化推流器 API:
 VeLivePusherConfiguration config = new VeLivePusherConfiguration();
 config.setContext(this);
 mLivePusher = config.build();
 2、设置预览视图 API:  mLivePusher.setRenderView(findViewById(R.id.render_view));
 3、打开麦克风采集 API:mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 4、打开摄像头采集 API: mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 5、开始推流 API：mLivePusher.startPushWithUrls(new String[]{"rtmp://push.example.com/rtm.sdp", "rtmp://push.example.com/rtmp"});
 参考文档：https://www.volcengine.com/docs/6469/155317
 */
public class PushRTMActivity extends AppCompatActivity {
    private VeLivePusher mLivePusher;
    private EditText mRtmUrlText;
    private EditText mRtmpUrlText;
    private TextView mInfoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_rtmactivity);

        mInfoView = findViewById(R.id.push_info_text_view);

        mRtmUrlText = findViewById(R.id.rtm_url_input_view);
        mRtmUrlText.setText(VeLiveSDKHelper.LIVE_RTM_PUSH_URL);

        mRtmpUrlText = findViewById(R.id.rtmp_url_input_view);
        mRtmpUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
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
        //  创建推流器  
        mLivePusher = config.build();
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
        VeLiveVideoFrame videoFrame = null;
    }

    public void pushControl(View view) {
        ToggleButton toggleButton = (ToggleButton)view;
        if (mRtmUrlText.getText().toString().isEmpty() || mRtmpUrlText.getText().toString().isEmpty()) {
            Log.e("VeLiveQuickStartDemo", "Please Config Url");
            return;
        }
        if (toggleButton.isChecked()) {
            //  开始推流，推流地址支持： rtmp 协议，http 协议（RTM）  
            mLivePusher.startPushWithUrls(new String[]{mRtmUrlText.getText().toString(), mRtmpUrlText.getText().toString()});
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