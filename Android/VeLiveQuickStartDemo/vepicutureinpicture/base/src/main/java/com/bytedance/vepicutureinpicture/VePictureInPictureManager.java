/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.bytedance.vepicutureinpicture;

import static com.bytedance.vepicutureinpicture.VePictureInPictureVideoActonLayer.*;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

class VePictureInPictureWindowConfig {
    public int width;
    public int height;
    public int x;
    public int y;
    public int margin;
}

public class VePictureInPictureManager {

    public interface VePictureInPicturePermissionResult {
        default void accept() {}
        default void cancel() {}
    }

    public interface VePictureInPicturePermissionCallback {
        void onGranted(boolean granted);
        void onRequestPermission(Context context, VePictureInPicturePermissionResult result);
    }

    public interface VePictureInPictureObserver {
        default void onStopPictureInPicture() {}
        default void onClosePictureInPicture() {}
        default void onPlayerPlay() {}
        default void onPlayerPause() {}
        default void onPlayerStop() {}
        default void onDoubleClick() {}
        default void onWindowFrameChanged(int x, int y, int w, int h) {}
    }

    private final Context mContext;
    private VePictureInPicturePermission mPictureInPicturePermission;
    private VePictureInPictureWindowView pipWindowView;
    private VePictureInPictureVideoActonLayer actonLayer;
    private VePictureInPictureObserver mObserver;
    private VePictureInPictureWindowConfig pipWindowConfig;
    private int minWidth;
    private int minHeight;
    private int maxWidth;
    private int maxHeight;
    private int curWidth;
    private int curHeight;

    public VePictureInPictureManager(Context context) {
        mContext = context;
        mPictureInPicturePermission = new VePictureInPicturePermission(context);

        pipWindowConfig = new VePictureInPictureWindowConfig();
        pipWindowConfig.width = 360;
        pipWindowConfig.height = 540;
        pipWindowConfig.margin = 5;
        pipWindowConfig.x = 0;
        pipWindowConfig.y = 0;
        minWidth = -1;
        minHeight = -1;
        maxWidth = -1;
        maxHeight = -1;

        if (isPermissionGranted()) {
            setupPipWindow();
        }
    }

    public boolean isPermissionGranted() {
        return mPictureInPicturePermission.isPermissionGranted();
    }

    public void requestPictureInPicturePermission(VePictureInPicturePermissionCallback callback) {
        requestPermission(callback);
    }

    public boolean isPictureInPictureShowing() {
        if (isPermissionGranted() && pipWindowView != null) {
            return pipWindowView.isShowing();
        }
        return false;
    }

    public SurfaceView startPictureInPicture() {
        if (pipWindowView != null) {
            SurfaceView surfaceView = actonLayer.getSurfaceView();
            if (surfaceView != null) {
                pipWindowView.show();

                return surfaceView;
            }
        }
        return null;
    }

    public void stopPictureInPicture() {
        if (pipWindowView != null) {
            pipWindowView.dismiss();
        }
    }

    public void setWindowConfig(VePictureInPictureWindowConfig config) {
        pipWindowConfig = config;
        if (pipWindowView != null) {
            pipWindowView.setWindowInitWidth(pipWindowConfig != null ? pipWindowConfig.width : 360);
            pipWindowView.setWindowInitHeight(pipWindowConfig != null ? pipWindowConfig.height : 540);
            // 设置悬浮窗距离屏幕边缘的 margin，单位：px
            pipWindowView.setWindowMargin(pipWindowConfig != null ? pipWindowConfig.margin : 5);
            // 设置悬浮窗显示位置坐标，单位：px
            pipWindowView.setWindowInitX(pipWindowConfig != null ? pipWindowConfig.x : 0);
            pipWindowView.setWindowInitY(pipWindowConfig != null ? pipWindowConfig.y : 0);
        }
    }

    public void setObserver(VePictureInPictureObserver observer) {
        mObserver = observer;
    }

    public void enablePlaybackBar(boolean enable) {
        if (actonLayer != null) {
            actonLayer.enablePlaybackBar(enable);
        }
    }

    public void enablePauseState(boolean enable) {
        if (actonLayer != null) {
            actonLayer.enablePauseState(enable);
        }
    }

    public void setPlaying(boolean playing) {
        if (actonLayer != null) {
            actonLayer.setPlaying(playing);
        }
    }

    private void requestPermission(VePictureInPicturePermissionCallback callback) {
        mPictureInPicturePermission.requestPermission(new VePictureInPicturePermission.Callback() {
            @Override
            public void onResult(boolean isGranted) {
                if (isGranted) {
                    setupPipWindow();
                }
                if (callback != null) {
                    callback.onGranted(isGranted);
                }
            }

            @Override
            public void onRationale(Context context, VePictureInPicturePermission.UserAction action) {
                if (callback != null) {
                    callback.onRequestPermission(context, new VePictureInPicturePermissionResult() {
                        @Override
                        public void accept() {
                            action.granted();
                        }
                        @Override
                        public void cancel() {
                            action.denied();
                        }
                    });
                }
            }
        });
    }

    public void setPictureInPictureWindowSize(int width, int height) {
        this.maxWidth = width;
        this.maxHeight = height;
        this.minWidth = -1;
        this.minHeight = -1;
        if (pipWindowView != null) {
            pipWindowView.resetWindowSize(width, height);
        }
    }

    public void setPictureInPictureWindowSize(int maxWidth, int maxHeight, int minWidth, int minHeight, boolean setMaxSize) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        if (setMaxSize) {
            setWindowSize(maxWidth, maxHeight);
        } else {
            setWindowSize(minWidth, minHeight);
        }
    }

    private void setWindowSize(int width, int height) {
        curWidth = width;
        curHeight = height;
        if (pipWindowView != null) {
            pipWindowView.resetWindowSize(width, height);
        }
    }

    private void setupPipWindow() {
        if (pipWindowView != null) {
            return;
        }
        pipWindowView = new VePictureInPictureWindowView(mContext);
        pipWindowView.setWindowInitWidth(pipWindowConfig != null ? pipWindowConfig.width : 360);
        pipWindowView.setWindowInitHeight(pipWindowConfig != null ? pipWindowConfig.height : 540);
        // 设置悬浮窗距离屏幕边缘的 margin，单位：px
        pipWindowView.setWindowMargin(pipWindowConfig != null ? pipWindowConfig.margin : 5);
        // 设置悬浮窗显示位置坐标，单位：px
        pipWindowView.setWindowInitX(pipWindowConfig != null ? pipWindowConfig.x : 0);
        pipWindowView.setWindowInitY(pipWindowConfig != null ? pipWindowConfig.y : 0);
        // 设置是否在显示时使用设置的大小、坐标信息
        pipWindowView.setInitShow(true);
        pipWindowView.setRadius(12);
        // 设置悬浮窗背景颜色
        pipWindowView.setCardBackgroundColor(Color.BLACK);
        // 设置悬浮窗 Z 轴高度，以及阴影效果，单位：px
        pipWindowView.setCardElevation(10);

        actonLayer = new VePictureInPictureVideoActonLayer(mContext);
        actonLayer.setPictureInPictureActionObserver(new VePictureInPictureActionObserver() {
            @Override
            public void onClosePictureInPicture() {
                if (mObserver != null) {
                    mObserver.onClosePictureInPicture();
                }
            }

            @Override
            public void onStopPictureInPicture() {
                if (mObserver != null) {
                    mObserver.onStopPictureInPicture();
                }
            }

            @Override
            public void onPlayerPlay() {
                if (mObserver != null) {
                    mObserver.onPlayerPlay();
                }
            }

            @Override
            public void onPlayerPause() {
                if (mObserver != null) {
                    mObserver.onPlayerPause();
                }
            }

            @Override
            public void onPlayerStop() {
                if (mObserver != null) {
                    mObserver.onPlayerStop();
                }
            }
        });
        pipWindowView.addView(actonLayer, new FrameLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        pipWindowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < DOUBLE_CLICK_INTERVAL) {
                    if (curWidth == maxWidth && curHeight == maxHeight) {
                        if (minWidth > 0 && minHeight > 0) {
                            setWindowSize(minWidth, minHeight);
                        }
                    } else if (curWidth == minWidth && curHeight == minHeight) {
                        if (maxWidth > 0 && maxHeight > 0) {
                            setWindowSize(maxWidth, maxHeight);
                        }
                    }
                    if (mObserver != null) {
                        mObserver.onDoubleClick();
                    }
                }
                lastClickTime = currentTime;
            }
        });

        pipWindowView.setObserver(new VePictureInPictureWindowView.VePictureInPictureWindowViewObserver() {
            @Override
            public void onPositionChanged(int x, int y, int width, int height) {
                if (mObserver != null) {
                    mObserver.onWindowFrameChanged(x, y, width, height);
                }
            }
        });
    }

    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 300;
}
