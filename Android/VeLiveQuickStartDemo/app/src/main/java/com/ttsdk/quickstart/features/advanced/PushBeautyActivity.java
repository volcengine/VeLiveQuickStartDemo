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
import com.ttsdk.quickstart.helper.VeLiveEffectHelper;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
import com.ss.avframework.live.VeLiveVideoEffectManager;

/*
摄像头推流，集成智能美化特效

 本文件展示如何集成摄像头推流和智能美化特效
 1、首先集成智能美化特效SDK，推荐集成动态库
 2、初始化推流器 API:
 VeLivePusherConfiguration config = new VeLivePusherConfiguration();
 config.setContext(this);
 mLivePusher = config.build();
 3、设置预览视图 API:  mLivePusher.setRenderView(findViewById(R.id.render_view));
 4、打开麦克风采集 API:mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 5、打开摄像头采集 API: mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 6、开始推流 API：[mLivePusher startPush:@"rtmp://push.example.com/rtmp"];
 7、初始化美颜相关参数 API: mLivePusher.getVideoEffectManager().setupWithConfig(new VeLiveVideoEffectLicenseConfiguration.create("licpath"));
 8、设置模型资源包 API: mLivePusher.getVideoEffectManager().setAlgoModelPath(algoModelPath);
 9、设置美颜美型 API: mLivePusher.getVideoEffectManager().setComposeNodes(nodes);
 10、设置滤镜 API: mLivePusher.getVideoEffectManager().setFilter("");
 11、设置贴纸 API:  mLivePusher.getVideoEffectManager().setSticker("");
 */
public class PushBeautyActivity extends AppCompatActivity {
    private VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_beauty);
        mInfoView = findViewById(R.id.push_info_text_view);
        mUrlText = findViewById(R.id.url_input_view);
        mUrlText.setText(VeLiveSDKHelper.LIVE_PUSH_URL);
        setupLivePusher();
        setupEffectSDK();
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

    public void setupEffectSDK() {
        //  注意：本方法只在工程中集成过智能美化特效的SDK时生效  
        VeLiveVideoEffectManager effectManager = mLivePusher.getVideoEffectManager();
        //  特效鉴权License路径，请根据工程配置查找正确的路径  
        String licPath = VeLiveEffectHelper.getLicensePath("xxx.licbag");
        //  特效模型资源包路径  
        String algoModePath = VeLiveEffectHelper.getModelPath();
        if (!VeLiveSDKHelper.isFileExists(licPath)) {
            return;
        }
        //  创建美颜配置  
        VeLivePusherDef.VeLiveVideoEffectLicenseConfiguration licConfig = VeLivePusherDef.VeLiveVideoEffectLicenseConfiguration.create(licPath);
        //  设置美颜配置  
        effectManager.setupWithConfig(licConfig);
        //  设置算法包路径  
        effectManager.setAlgorithmModelPath(algoModePath);
        //  开启美颜特效处理  
        effectManager.setEnable(true, new VeLivePusherDef.VeLiveVideoEffectCallback() {
            @Override
            public void onResult(int result, String msg) {
                if (result != 0) {
                    Log.e("VeLiveQuickStartDemo", "Effect init error:" + msg);
                }
            }
        });
    }

    public void beautyControl(View view) {
        //  根据特效资源包，查找正确的资源路径，一般到 reshape_lite, beauty_IOS_lite 目录  
        String beautyPath = VeLiveEffectHelper.getBeautyPathByName("xxx");
        if (!VeLiveSDKHelper.isFileExists(beautyPath)) {
            return;
        }
        //  设置美颜美型特效资源包  
        mLivePusher.getVideoEffectManager().setComposeNodes(new String[]{ beautyPath });
        //  设置美颜美型特效强度, NodeKey 可在 资源包下的 .config_file 中获取，如果没有 .config_file ，请联系商务咨询  
        mLivePusher.getVideoEffectManager().updateComposerNodeIntensity(beautyPath, "whiten", 0.5F);
    }

    public void filterControl(View view) {
        //  滤镜资源包，查找正确的资源路径，一般到 Filter_01_xx 目录  
        String filterPath = VeLiveEffectHelper.getFilterPathByName("xxx");;
        //  设置滤镜资源包路径  
        mLivePusher.getVideoEffectManager().setFilter(filterPath);
        //  设置滤镜特效强度  
        mLivePusher.getVideoEffectManager().updateFilterIntensity(0.5F);
    }

    public void stickerControl(View view) {
        //  贴纸资源包，查找正确的资源路径，一般到 stickers_xxx 目录  
        String stickerPath = VeLiveEffectHelper.getStickerPathByName("xxx");
        //  设置贴纸资源包路径  
        mLivePusher.getVideoEffectManager().setSticker(stickerPath);
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