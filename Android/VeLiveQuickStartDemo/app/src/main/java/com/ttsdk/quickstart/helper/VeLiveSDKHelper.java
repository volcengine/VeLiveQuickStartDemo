/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;
/*
 本文件存放 SDK 基础配置信息，不分信息在进入相应页面是可以在界面上进行修改
 本文件存放SDK基础配置信息，包含 SDK AppID， License 文件名，推拉流地址，连麦互动房间ID、主播/观众 UID及临时Token
 SDK 配置信息申请：https://console.volcengine.com/live/main/sdk
 推拉流地址生成参考文档：https://console.volcengine.com/live/main/locationGenerate
 互动直播相关参考文档：https://console.volcengine.com/rtc/listRTC
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ttsdk.quickstart.R;
import com.pandora.common.env.Env;
import com.pandora.common.env.config.Config;
import com.pandora.common.env.config.LogConfig;
import com.pandora.ttlicense2.LicenseManager;
import com.ss.avframework.live.VeLivePusherDef;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;

import java.util.ArrayList;
import java.util.List;

public class VeLiveSDKHelper {

    // AppID
    public static String TTSDK_APP_ID = "";
    /*
     License 名称，当前 Demo文件存放在与本文件同级目录下，如果做SDK快速验证，可直接替换 ttsdk.lic 文件内容
     */
    public static String TTSDK_LICENSE_NAME = "ttsdk.lic";

    /*
     rtmp, RTM, Quic 推流地址
     生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
     */
    public static String LIVE_PUSH_URL = "";
    public static String LIVE_RTM_PUSH_URL = "";

    /*
     RTM, rtmp、flv、m3u8 拉流地址
     生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
     */
    public static String LIVE_PULL_URL = "";
    public static String LIVE_RTM_PULL_URL = "";


    /*
     互动直播AppID
     */
    public static String RTC_APPID = "";

    /*
     互动直播主播房间ID
     */
    public static String RTC_ROOM_ID = "";

    /*
     互动直播主播用户ID
     */
    public static String RTC_USER_ID = "";

    /*
     互动直播主播用户 Token
     生成方式：使用主播房间ID和主播用户ID在RTC控制台生成
     https://console.volcengine.com/rtc/listRTC
     */
    public static String RTC_USER_TOKEN = "";

    /*
     主播与主播PK时，对方主播的房间ID
     */
    public static String RTC_OTHER_ROOM_ID = "";

    /*
     主播与主播PK时，当前主播加入对方主播房间内的Token
     生成方式：使用当前主播的用户ID和对方主播的房间ID在控制台生成
     */
    public static String RTC_OTHER_ROOM_TOKEN = "";

    private static Context sAppContext;

    public static void initTTSDK(final Context context) {
        sAppContext = context;

        LogConfig.Builder logBuilder = new LogConfig.Builder(context);
        //  是否输出日志到控制台  
        logBuilder.setEnableConsole(true);
        //  是否写入日志到文件  
        logBuilder.setEnableLogFile(true);
        //  当前设备唯一ID  
        logBuilder.setDeviceID(Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        //  文件最大大小，单位MB  
        logBuilder.setMaxLogSizeM(10);
        //  单个文件大小，单位MB  
        logBuilder.setSingleLogSizeM(1);
        //  文件过期时间，单位秒  
        logBuilder.setLogExpireTimeS(24 * 60 * 60);
        //  日志输出级别  
        logBuilder.setLogLevel(LogConfig.LogLevel.Debug);

        Config.Builder configBuilder = new  Config.Builder();
        //  配置 App 上下文  
        configBuilder.setApplicationContext(context);
        //  通道配置，一般传分发类型，内测、公测、线上等  
        configBuilder.setAppChannel("GoogleStore");
        //  App 名称  
        configBuilder.setAppName(getAppName(context));
        //  配置服务区域，默认CN  
        configBuilder.setAppRegion("china");

        //  版本号  
        configBuilder.setAppVersion(getVersionName(context));
        //  配置日志  
        configBuilder.setLogConfig(logBuilder.build());
        //  配置 TTSDK AppID  
        configBuilder.setAppID(TTSDK_APP_ID);
        //  配置 License 路径  
        configBuilder.setLicenseUri("assets:///" + TTSDK_LICENSE_NAME);
        //  配置 License 解析回调  
        configBuilder.setLicenseCallback(mLicenseCallback);
        //  初始化 SDK  
        Env.init(configBuilder.build());

        VeLiveEffectHelper.initVideoEffectResource();
    }

    private static final LicenseManager.Callback mLicenseCallback = new LicenseManager.Callback() {
        @Override
        public void onLicenseLoadSuccess(@NonNull String licenseUri, @NonNull String licenseId) {
            Log.e("VeLiveQuickStartDemo", "License Load Success" + licenseId);
        }
        @Override
        public void onLicenseLoadError(@NonNull String licenseUri, @NonNull Exception e, boolean retryAble) {
            Log.e("VeLiveQuickStartDemo", "License Load Error" + e);
        }
        @Override
        public void onLicenseLoadRetry(@NonNull String licenseUri) {

        }
        @Override
        public void onLicenseUpdateSuccess(@NonNull String licenseUri, @NonNull String licenseId) {

        }
        @Override
        public void onLicenseUpdateError(@NonNull String licenseUri, @NonNull Exception e, boolean retryAble) {

        }
        @Override
        public void onLicenseUpdateRetry(@NonNull String licenseUri) {

        }
    };

    static private String getInfoString(int resId, Object value, String end) {
        return sAppContext.getString(resId) + ":" + value + end;
    }

    static public String getPushInfoString(VeLivePusherDef.VeLivePusherStatistics statistics) {
        String infoStr = "";
        infoStr += getInfoString(R.string.Camera_Push_Info_Url, statistics.url, "\n");
        infoStr += getInfoString(R.string.Camera_Push_Info_Video_MaxBitrate, statistics.maxVideoBitrate / 1000, " kbps ");
        infoStr += getInfoString(R.string.Camera_Push_Info_Video_StartBitrate, statistics.videoBitrate / 1000, " kbps\n");
        infoStr += getInfoString(R.string.Camera_Push_Info_Video_MinBitrate, statistics.minVideoBitrate / 1000, " kbps ");
        infoStr += getInfoString(R.string.Camera_Push_Info_Video_Capture_Resolution, statistics.captureWidth + ", " + statistics.captureHeight, "\n");

        infoStr += getInfoString(R.string.Camera_Push_Info_Video_Push_Resolution, statistics.encodeWidth + ", " + statistics.encodeHeight, " ");
        infoStr += getInfoString(R.string.Camera_Push_Info_Video_Capture_FPS, (int)statistics.captureFps, "\n");

        infoStr += getInfoString(R.string.Camera_Push_Info_Video_Capture_IO_FPS, (int)statistics.captureFps + "/" + (int)statistics.encodeFps, " ");

        infoStr += getInfoString(R.string.Camera_Push_Info_Video_Encode_Codec, statistics.codec, "\n");

        infoStr += getInfoString(R.string.Camera_Push_Info_Real_Time_Trans_FPS, (int)statistics.transportFps, "\n");
        infoStr += getInfoString(R.string.Camera_Push_Info_Real_Time_Encode_Bitrate, (int)(statistics.encodeVideoBitrate / 1000), " kbps ");
        infoStr += getInfoString(R.string.Camera_Push_Info_Real_Time_Trans_Bitrate, (int)(statistics.transportVideoBitrate / 1000), " kbps ");
        return infoStr;
    }

    static public String getPlaybackInfoString(VeLivePlayerStatistics statistics) {
        String infoStr = "";
        infoStr += getInfoString(R.string.Pull_Stream_Info_Url, statistics.url, "\n");
        String videoSize = "width:" + statistics.width + "height:" + statistics.height;
        infoStr += getInfoString(R.string.Pull_Stream_Info_Video_Size, videoSize, "\n");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Video_FPS, (int)statistics.fps, " ");
        infoStr += getInfoString(R.string.Pull_Stream_Info_Video_Bitrate, statistics.bitrate, " kbps\n");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Video_BufferTime, statistics.videoBufferMs, "ms ");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Audio_BufferTime, statistics.audioBufferMs, " ms\n");
        infoStr += getInfoString(R.string.Pull_Stream_Info_Stream_Format, statistics.format, " ");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Stream_Protocol, statistics.protocol, "\n");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Video_Codec, statistics.videoCodec, "\n");

        infoStr += getInfoString(R.string.Pull_Stream_Info_Delay_Time, statistics.delayMs, "ms ");
        infoStr += getInfoString(R.string.Pull_Stream_Info_Stall_Time, statistics.stallTimeMs, " ms\n");
        infoStr += getInfoString(R.string.Pull_Stream_Info_Is_HardWareDecode, statistics.isHardwareDecode, " ");
        return infoStr;
    }

    static public boolean checkPermission(Activity activity, int request) {
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        List<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(sAppContext, permission) == PackageManager.PERMISSION_GRANTED;
            if (granted) continue;
            permissionList.add(permission);
        }
        if (permissionList.isEmpty()) return true;
        String[] permissionsToGrant = new String[permissionList.size()];
        permissionList.toArray(permissionsToGrant);
        ActivityCompat.requestPermissions(activity, permissionsToGrant, request);
        return false;
    }

    /**
     * 获取应用程序名称
     */
    public static synchronized String getAppName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * [获取应用程序版本名称信息]
     * @param context
     * @return 当前应用的版本名称
     */
    public static synchronized String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
