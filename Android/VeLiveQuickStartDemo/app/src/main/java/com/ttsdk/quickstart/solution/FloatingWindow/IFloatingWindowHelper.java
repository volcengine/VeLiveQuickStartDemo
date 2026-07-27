/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.solution.FloatingWindow;

import android.content.Context;
import android.view.SurfaceView;

import java.util.Map;

public interface IFloatingWindowHelper {
    // 设置事件监听回调
    void setEventListener(Listener listener);

    // 开启悬浮窗（如果没有开启悬浮窗权限，会同时申请权限），同时支持缓存部分业务数据，用于恢复直播页。
    void openFloatingWindow(Context context, Config config, Map<String, Object> extraData);

    // 关闭悬浮窗
    void closeFloatingWindow(Context context);

    // 申请悬浮窗权限
    void requestOverlayPermission(Context context);

    // 获取悬浮窗配置
    Config getConfig();

    // 获取缓存的业务数据
    Map<String, Object> getExtraData();

    // 悬浮窗是否已开启
    boolean isOpen();

    interface Listener {
        // 开启悬浮窗回调的错误码
        int ERR_NO = 0;
        int ERR_NOT_SUPPORT = 1;
        int ERR_INVALID_PARAMS = 2;
        int ERR_IS_ALREADY_OPEN = 3;

        // 申请 Overlay 权限时触发
        // return: boolean，是否允许申请 Overlay 权限
        //      - true: 允许
        //      - false: 不允许。
        default boolean onRequestOverlayPermission() {
            return true;
        }

        // 开启悬浮窗的结果回调
        // param: errCode，ERR_NO 表示成功，其它值为发生了错误
        default void onOpenFloatingWindowResult(int errCode, Map<String, Object> extraData) {}

        // 关闭悬浮窗回调
        default void onCloseFloatingWindow(Map<String, Object> extraData) {}

        // 点击悬浮窗的事件回调，需要由业务进行处理
        default void onClickFloatingWindow(Context context) {}

        // 点击悬浮窗的关闭按钮事件回调，需要由业务进行处理
        default void onClickFloatingWindowCloseBtn(Context context) {
            FloatingWindowHelper.getInstance().closeFloatingWindow(context);
        }

        // 回调悬浮窗的 SurfaceView，需要由业务设置到 SDK 中来渲染视频
        void onUpdateSurfaceView(SurfaceView surfaceView);
    }

    class Config {
        public float aspectRatio;
        int x, y; // 悬浮窗的创建位置

        public Config(float aspectRatio) {
            this(aspectRatio, 0, 0);
        }

        public Config(float aspectRatio, int x, int y) {
            this.aspectRatio = aspectRatio;
            this.x = x;
            this.y = y;
        }
    }
}