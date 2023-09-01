/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced.pip;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.ttsdk.quickstart.R;

public class FloatingVideoService extends Service {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    public class Binder extends android.os.Binder {
        public FloatingVideoService getService() {
            return FloatingVideoService.this;
        }
    }

    private Binder mBinder = new Binder();

    private SurfaceView mSurfaceView;
    private FrameLayout mSurfaceContainer;
    private View mSmallWindowView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = 450;
        layoutParams.height = 800;
        layoutParams.x = 300;
        layoutParams.y = 300;

        if (Settings.canDrawOverlays(this)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            mSmallWindowView = layoutInflater.inflate(R.layout.floating_window, null);
            mSurfaceContainer = mSmallWindowView.findViewById(R.id.surface_container);
            mSmallWindowView.setOnTouchListener(new FloatingOnTouchListener());
            windowManager.addView(mSmallWindowView, layoutParams);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeView();
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public FrameLayout getSurfaceContainer() {
        return mSurfaceContainer;
    }

    public void removeView() {
        if (mSmallWindowView != null) {
            windowManager.removeView(mSmallWindowView);
        }
    }

    public void addSurfaceView(SurfaceView view) {
        mSurfaceView = view;
        mSurfaceContainer.addView(view);
    }

    public void removeSurfaceView() {
        if (mSurfaceView != null) {
            mSurfaceContainer.removeView(mSurfaceView);
            mSurfaceView = null;
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
