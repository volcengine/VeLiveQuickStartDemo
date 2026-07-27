/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.solution.FloatingWindow;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.ttsdk.quickstart.R;

public class FloatingWindowService extends Service {
    private static final String TAG = FloatingWindowService.class.getSimpleName();
    private static final int LONGER_SIDE_MAX_LEN = 800;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private SurfaceView mSurfaceView;
    private View mSmallWindowView;

    public static final String INTENT_EXTRA_KEY_ASPECT_RATIO = "aspect_ratio";
    public static final String INTENT_EXTRA_KEY_X_POS = "x_pos";
    public static final String INTENT_EXTRA_KEY_Y_POS = "y_pos";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        FloatingWindowHelper.getInstance().onStartService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        initUI(intent.getFloatExtra(INTENT_EXTRA_KEY_ASPECT_RATIO, 16f / 9f),
                intent.getIntExtra(INTENT_EXTRA_KEY_X_POS, 300),
                intent.getIntExtra(INTENT_EXTRA_KEY_Y_POS, 300));
        FloatingWindowHelper.getInstance().setSurfaceView(mSurfaceView);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (mSmallWindowView != null) {
            mWindowManager.removeView(mSmallWindowView);
        }
        FloatingWindowHelper.getInstance().onStopService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initUI(float aspectRatio, int x, int y) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //  限制悬浮窗不要过大或过小，长边控制为 LONGER_SIDE_MAX_LEN，短边按比例缩放
        int width, height;
        if (aspectRatio >= 1) {
            height = (int) (LONGER_SIDE_MAX_LEN / aspectRatio);
            width = LONGER_SIDE_MAX_LEN;
        } else {
            width = (int) (LONGER_SIDE_MAX_LEN * aspectRatio);
            height = LONGER_SIDE_MAX_LEN;
        }
        mLayoutParams.width = width;
        mLayoutParams.height = height;

        //  悬浮窗的初始位置  
        mLayoutParams.x = x;
        mLayoutParams.y = y;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                LayoutInflater layoutInflater = LayoutInflater.from(this);
                mSmallWindowView = layoutInflater.inflate(R.layout.floating_window, null);
                mSmallWindowView.setOnTouchListener(new FloatingOnTouchListener());
                mWindowManager.addView(mSmallWindowView, mLayoutParams);
                mSurfaceView = mSmallWindowView.findViewById(R.id.surface_view);
                mSmallWindowView.findViewById(R.id.surface_close_btn).setOnClickListener(v -> {
                    FloatingWindowHelper.getInstance().performClose(FloatingWindowService.this);
                });
            }
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private final int touchSlop = ViewConfiguration.get(FloatingWindowService.this.getApplicationContext()).getScaledTouchSlop();
        private int downX, downY;
        private int x, y;
        private boolean isDragging;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (isDragging) {
                        isDragging = false;
                    } else {
                        FloatingWindowHelper.getInstance().onClickFloatingWindow(FloatingWindowService.this);
                    }
                    downX = downY = 0;
                    break;
                case MotionEvent.ACTION_DOWN:
                    downX = x = (int) event.getRawX();
                    downY = y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    if (Math.abs(nowX - downX) > touchSlop || Math.abs(nowY - downY) > touchSlop) {
                        isDragging = true;
                    }
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    mLayoutParams.x = mLayoutParams.x + movedX;
                    mLayoutParams.y = mLayoutParams.y + movedY;
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;

                default:
                    break;
            }
            return true;
        }
    }
}