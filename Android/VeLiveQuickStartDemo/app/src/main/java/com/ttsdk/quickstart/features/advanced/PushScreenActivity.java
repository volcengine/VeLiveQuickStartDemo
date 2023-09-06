/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureVoiceCommunication;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureScreen;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.KeepLiveService;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;

public class PushScreenActivity extends AppCompatActivity {
    private static VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;

    private Intent mScreenIntent = null;
    public static int REQUEST_CODE_FROM_PROJECTION_SERVICE = 1001;
    private ServiceConnection mKeepLiveServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            startCapture();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_screen);
        mInfoView = findViewById(R.id.push_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
        setupLivePusher();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止前台服务
        unbindService(mKeepLiveServiceConnection);
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
        //  设置推流器回调  
        mLivePusher.setObserver(pusherObserver);
        //  设置周期性信息回调  
        mLivePusher.setStatisticsObserver(statisticsObserver, 3);
        startCapture();
    }

    private void startCapture() {
        // 请求 MediaProjection 权限
        if (!checkPermission()) {
            return;
        }
        mLivePusher.startScreenRecording(true, mScreenIntent);
        //  开始视频采集  
        mLivePusher.startVideoCapture(VeLiveVideoCaptureScreen);
        //  开始音频采集  
        mLivePusher.startAudioCapture(VeLiveAudioCaptureVoiceCommunication);
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

    private boolean checkPermission() {
        if (mScreenIntent == null) {
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {
                MediaProjectionManager mgr = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mgr != null) {
                    Intent intent = mgr.createScreenCaptureIntent();
                    startActivityForResult(intent, REQUEST_CODE_FROM_PROJECTION_SERVICE);
                }
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_FROM_PROJECTION_SERVICE) {
            mScreenIntent = data;
            // 使用 MediaProjection 时，需要同时启动一个前台服务
            startKeepLive();
        }
    }

    private void startKeepLive() {
        Intent intent = new Intent(this, KeepLiveService.class);
        bindService(intent, mKeepLiveServiceConnection, BIND_AUTO_CREATE);
    }
}