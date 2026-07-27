/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.solution.FloatingWindow;

import static com.ttsdk.quickstart.solution.FloatingWindow.FloatingWindowService.INTENT_EXTRA_KEY_ASPECT_RATIO;
import static com.ttsdk.quickstart.solution.FloatingWindow.FloatingWindowService.INTENT_EXTRA_KEY_X_POS;
import static com.ttsdk.quickstart.solution.FloatingWindow.FloatingWindowService.INTENT_EXTRA_KEY_Y_POS;
import static com.ttsdk.quickstart.solution.FloatingWindow.IFloatingWindowHelper.Listener.ERR_INVALID_PARAMS;
import static com.ttsdk.quickstart.solution.FloatingWindow.IFloatingWindowHelper.Listener.ERR_IS_ALREADY_OPEN;
import static com.ttsdk.quickstart.solution.FloatingWindow.IFloatingWindowHelper.Listener.ERR_NO;
import static com.ttsdk.quickstart.solution.FloatingWindow.IFloatingWindowHelper.Listener.ERR_NOT_SUPPORT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ttsdk.quickstart.R;

import java.util.Map;

public class FloatingWindowHelper implements IFloatingWindowHelper {
    private static FloatingWindowHelper mInstance = null;
    private Listener mListener;
    private Config mConfig;
    private Map<String, Object> mExtraDataMap;
    private boolean isRunning;

    public synchronized static FloatingWindowHelper getInstance() {
        if (mInstance == null) {
            mInstance = new FloatingWindowHelper();
        }
        return mInstance;
    }

    @Override
    public void setEventListener(Listener listener) {
        mListener = listener;
    }

    public void openFloatingWindow(Context context, Config config, Map<String, Object> extraData) {
        if (isRunning) {
            if (mListener != null) {
                mListener.onOpenFloatingWindowResult(ERR_IS_ALREADY_OPEN, extraData);
            }
            return;
        }
        //  参数值非法
        if (context == null || config.aspectRatio <= 0) {
            if (mListener != null) {
                mListener.onOpenFloatingWindowResult(ERR_INVALID_PARAMS, extraData);
            }
            return;
        }

        // Android 6 以下的版本不支持 Overlay 功能
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (mListener != null) {
                mListener.onOpenFloatingWindowResult(ERR_NOT_SUPPORT, extraData);
            }
            return;
        }

        // 没有开启 Overlay 权限时，需要先申请权限
        if (!Settings.canDrawOverlays(context)) {
            if (mListener != null && mListener.onRequestOverlayPermission()) {
                requestOverlayPermission(context);
            }
            return;
        }

        mConfig = config;
        mExtraDataMap = extraData;

        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.putExtra(INTENT_EXTRA_KEY_ASPECT_RATIO, mConfig.aspectRatio);
        intent.putExtra(INTENT_EXTRA_KEY_X_POS, mConfig.x);
        intent.putExtra(INTENT_EXTRA_KEY_Y_POS, mConfig.y);
        context.startService(intent);
    }

    @Override
    public void closeFloatingWindow(Context context) {
        if (!isRunning) {
            return;
        }
        context.stopService(new Intent(context, FloatingWindowService.class));
    }

    @Override
    public void requestOverlayPermission(Context context) {
        Intent intent = new Intent(context, AssistantActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public Config getConfig() {
        return mConfig;
    }

    @Override
    public Map<String, Object> getExtraData() {
        return mExtraDataMap;
    }

    @Override
    public boolean isOpen() {
        return isRunning;
    }

    void setSurfaceView(SurfaceView surfaceView) {
        if (mListener != null) {
            mListener.onUpdateSurfaceView(surfaceView);
        }
    }

    void performClose(Context context) {
        if (mListener != null) {
            mListener.onClickFloatingWindowCloseBtn(context);
        }
    }

    void onClickFloatingWindow(Context context) {
        if (mListener != null) {
            mListener.onClickFloatingWindow(context);
        }
    }

    void onStartService() {
        isRunning = true;
        if (mListener != null) {
            mListener.onOpenFloatingWindowResult(ERR_NO, mExtraDataMap);
        }
    }

    void onStopService() {
        isRunning = false;
        if (mListener != null) {
            mListener.onCloseFloatingWindow(mExtraDataMap);
        }

        mConfig = null;
        mExtraDataMap = null;
    }

    public static class AssistantActivity extends Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            int sdkInt = Build.VERSION.SDK_INT;
            int mOverlayRequestCode = 1001;
            if (sdkInt >= Build.VERSION_CODES.O) { //  8.0以上
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivityForResult(intent, mOverlayRequestCode);
            } else if (sdkInt >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, mOverlayRequestCode);
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "悬浮窗权限申请失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "悬浮窗权限申请成功", Toast.LENGTH_SHORT).show();
                }
            }
            this.finish();
        }
    }
}