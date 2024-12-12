/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioCaptureType.VeLiveAudioCaptureMicrophone;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioChannel.VeLiveAudioChannelStereo;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioMixType.VeLiveAudioMixPlayAndPush;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveAudioSampleRate.VeLiveAudioSampleRate44100;
import static com.ss.avframework.live.VeLivePusherDef.VeLivePusherRenderMode.VeLivePusherRenderModeHidden;
import static com.ss.avframework.live.VeLivePusherDef.VeLiveVideoCaptureType.VeLiveVideoCaptureFrontCamera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ss.avframework.live.VeLiveAudioFrame;
import com.ss.avframework.live.VeLivePusher;
import com.ss.avframework.live.VeLivePusherConfiguration;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.avframework.live.VeLivePusherObserver;
import com.ss.avframework.live.VeLiveVideoFrame;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveFileReader;
import com.ttsdk.quickstart.helper.VeLiveMediaResourceMgr;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.VeLiveURLGenerator;
import com.ttsdk.quickstart.helper.sign.model.VeLivePushURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

import java.io.File;

/*
推流同时混入音视频流

 本文件展示如何集成摄像头推流和音视频混流功能
 1、初始化推流器 API:
 VeLivePusherConfiguration config = new VeLivePusherConfiguration();
 config.setContext(this);
 mLivePusher = config.build();
 2、设置预览视图 API:  mLivePusher.setRenderView(findViewById(R.id.render_view));
 3、打开麦克风采集 API:mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 4、打开摄像头采集 API: mLivePusher.startVideoCapture(VeLiveVideoCaptureFrontCamera);
 5、开始推流 API：[mLivePusher startPush:@"rtmp://push.example.com/rtmp"];
 6、设置音频混流 API:
 int mAudioStreamHandle = mLivePusher.getMixerManager().addAudioStream(VeLiveAudioMixPlayAndPush);
 mLivePusher.getMixerManager().sendCustomAudioFrame(VeLiveAudioFrame, mAudioStreamHandle);
 7、设置视频混流 API:
 int mVideoStreamHandle = mLivePusher.getMixerManager().addVideoStream();
 mLivePusher.getMixerManager().updateStreamMixDescription(description);
 mLivePusher.getMixerManager().sendCustomVideoFrame(VeLiveVideoFrame, mVideoStreamHandle);
 */

public class PushMixStreamActivity extends AppCompatActivity {
    private static final String TAG = "PushBeautyActivity";
    private VeLivePusher mLivePusher;
    private EditText mUrlText;
    private TextView mInfoView;

    private int mAudioStreamHandle = -1;
    private VeLiveFileReader mAudioFileReader = null;
    private int mVideoStreamHandle = -1;
    private VeLiveFileReader mVideoFileReader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_push_mix_stream);
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
        ToggleButton toggleButton = (ToggleButton) view;
        if (mUrlText.getText().toString().isEmpty()) {
            Log.e(TAG, "Please Config Url");
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

    public void mixAudioControl(View view) {
        String audioResource = getFilesDir().getAbsolutePath() + File.separator + "audio_44100_16bit_2ch.pcm";
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            //  如果音频资源没有准备好，先准备  
            if (!new File(audioResource).exists()) {
                toggleButton.setTag(getResources().getString(R.string.Push_Mix_Stream_Audio));
                changeButtonState(true, toggleButton);
                VeLiveMediaResourceMgr.prepareResource(this, R.raw.audio_44100_16bit_2ch, audioResource, new VeLiveMediaResourceMgr.PrepareListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            changeButtonState(false, toggleButton);
                            mixAudioControl(view);
                        });
                    }
                    @Override
                    public void onFail()  {
                        runOnUiThread(() -> {
                            changeButtonState(false, toggleButton);
                            toggleButton.setChecked(false);
                        });
                    }
                });
                return;
            }

            //  添加音频流, 同时本地渲染  
            mAudioStreamHandle = mLivePusher.getMixerManager().addAudioStream(VeLiveAudioMixPlayAndPush);
            mAudioFileReader = new VeLiveFileReader();
            mAudioFileReader.start(audioResource, 441 * 2 * 2, 10, (byteBuffer, pts) -> {
                        //  混入音频流  
                        mLivePusher.getMixerManager().sendCustomAudioFrame(
                                new VeLiveAudioFrame(VeLiveAudioSampleRate44100, VeLiveAudioChannelStereo,pts, byteBuffer),
                                mAudioStreamHandle);
                    });
        } else {
            if (mAudioFileReader != null) {
                mAudioFileReader.stop();
                mAudioFileReader = null;
            }
            if (mAudioStreamHandle!= -1) {
                mLivePusher.getMixerManager().removeAudioStream(mAudioStreamHandle);
                mAudioStreamHandle = -1;
            }
        }
    }

    public void mixVideoControl(View view) {
        String videoResource = getFilesDir().getAbsolutePath() + File.separator + "video_320x180_25fps_yuv420.yuv";
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            //  如果视频资源没有准备好，先准备  
            if (!new File(videoResource).exists()) {
                toggleButton.setTag(getResources().getString(R.string.Push_Mix_Stream_Video));
                changeButtonState(true, toggleButton);
                VeLiveMediaResourceMgr.prepareResource(this, R.raw.video_320x180_25fps_yuv420, videoResource, new VeLiveMediaResourceMgr.PrepareListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            changeButtonState(false, toggleButton);
                            mixVideoControl(view);
                        });
                    }
                    @Override
                    public void onFail()  {
                        runOnUiThread(() -> {
                            changeButtonState(false, toggleButton);
                            toggleButton.setChecked(false);
                        });
                    }
                });
                return;
            }
            //  添加视频流  
            mVideoStreamHandle = mLivePusher.getMixerManager().addVideoStream();
            VeLivePusherDef.VeLiveStreamMixDescription description = new VeLivePusherDef.VeLiveStreamMixDescription();
            VeLivePusherDef.VeLiveMixVideoLayout layout = new VeLivePusherDef.VeLiveMixVideoLayout();
            layout.x = 0.5f;
            layout.y = 0.5f;
            layout.width = 0.3f;
            layout.height = 0.3f;
            layout.zOrder = 10;
            layout.renderMode = VeLivePusherRenderModeHidden;
            layout.streamId = mVideoStreamHandle;
            description.mixVideoStreams.add(layout);
            //  更新视频流配置  
            mLivePusher.getMixerManager().updateStreamMixDescription(description);
            mVideoFileReader = new VeLiveFileReader();
            mVideoFileReader.start(videoResource, 320 * 180 * 3 / 2, 1000 / 25, (byteBuffer, pts) -> {
                        //  混入视频流  
                        mLivePusher.getMixerManager().sendCustomVideoFrame(
                                new VeLiveVideoFrame(320, 180, pts, byteBuffer),
                                mVideoStreamHandle);
                    });
        } else {
            if (mVideoFileReader != null) {
                mVideoFileReader.stop();
                mVideoFileReader = null;
            }
            if (mVideoStreamHandle!= -1) {
                mLivePusher.getMixerManager().removeVideoStream(mVideoStreamHandle);
                mVideoStreamHandle = -1;
            }
        }
    }

    private void changeButtonState(boolean isPreparing, ToggleButton toggleButton) {
        runOnUiThread(() -> {
            if (isPreparing) {
                toggleButton.setEnabled(false);
                toggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_disable));
                toggleButton.setText(getResources().getString(R.string.preparing));
            } else {
                toggleButton.setEnabled(true);
                toggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.state_button_bg_color));
                toggleButton.setText((String)toggleButton.getTag());
            }
        });
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