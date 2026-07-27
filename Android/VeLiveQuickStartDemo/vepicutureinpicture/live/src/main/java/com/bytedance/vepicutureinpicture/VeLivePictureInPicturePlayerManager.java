/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.bytedance.vepicutureinpicture;

import android.content.Context;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.ss.videoarch.liveplayer.VeLivePlayer;


/**
 * 直播画中画播放器管理器，负责将 {@link VeLivePlayer} 与画中画悬浮窗进行绑定与解绑，
 * 以及在画中画显示 / 关闭时同步播放状态、Surface 输出目标与窗口尺寸。
 * <p>
 * Manager for the live picture-in-picture (PIP) player. It binds and unbinds a
 * {@link VeLivePlayer} with the PIP floating window, and keeps the playback
 * state, Surface output target and window size synchronised while entering or
 * leaving PIP mode.
 */
public class VeLivePictureInPicturePlayerManager {

    /**
     * 画中画播放器管理器的事件回调接口。
     * <p>
     * Observer interface used to receive PIP player events (e.g. resume click).
     */
    public interface VeLivePictureInPicturePlayerManagerObserver {
        /**
         * 当用户点击画中画悬浮窗中的“恢复播放”入口时回调，用于宿主页跳回主播放界面。
         * <p>
         * Invoked when the user clicks the "resume" action on the PIP floating
         * window, typically used by the host page to bring the main player
         * activity back to the foreground.
         */
        default void onClickResumeAction() {}
    }

    private VePictureInPictureManager manager;
    private VeLivePlayer mPlayer;
    private SurfaceHolder mSurfaceHolder;
    private Surface mSurface;
    private Context mContext;
    private VeLivePictureInPicturePlayerManagerObserver mObserver;
    private int lastVideoWidth = 0;
    private int lastVideoHeight = 0;
    private static VeLivePictureInPictureConfiguration mConfiguration;
    private static int x = -1, y = -1, width = -1, height = -1;
    private static final int WINDOW_VIEW_MARGIN = 5;

    /**
     * 构造画中画播放器管理器。会读取上一次记忆的窗口位置和尺寸（如无则采用默认值），
     * 初始化底层 {@link VePictureInPictureManager} 及其窗口布局，并禁用悬浮窗内的
     * 播放进度条与暂停态显示。
     * <p>
     * Construct a PIP player manager. The previously remembered window position
     * and size will be reused if available, otherwise default values are used.
     * The underlying {@link VePictureInPictureManager} is initialised together
     * with its window layout, and the playback bar / pause state overlays are
     * disabled inside the PIP window.
     *
     * @param player  与画中画绑定的直播播放器实例 / the live player instance to be bound with PIP.
     * @param context 用于获取屏幕尺寸和创建窗口的上下文 / context used for reading screen metrics and creating the window.
     */
    public VeLivePictureInPicturePlayerManager(VeLivePlayer player, Context context) {
        mContext = context;
        mPlayer = player;
        manager = new VePictureInPictureManager(context);

        VePictureInPictureWindowConfig config = new VePictureInPictureWindowConfig();
        config.margin = WINDOW_VIEW_MARGIN;
        if (width > 0 && height > 0) {
            config.width = width;
            config.height = height;
        } else {
            config.width = 360;
            config.height = 540;
        }
        if (x >= 0 && y >= 0) {
            config.x = x;
            config.y = y;
        } else {
            android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            config.x = metrics.widthPixels;
            config.y = metrics.heightPixels / 2;
        }

        manager.setWindowConfig(config);

        manager.setObserver(new VeLivePictureInPictureManagerObserver());
        manager.enablePlaybackBar(false);
        manager.enablePauseState(false);
    }

    /**
     * 设置全局画中画配置（例如最大 / 最小宽高、宽高比例限制等），后续所有实例创建
     * 或视频尺寸变化时都会读取该配置。传入 {@code null} 表示清除已有配置。
     * <p>
     * Set the global PIP configuration (e.g. max / min width / height and their
     * aspect ratio limits). All subsequent instances and video size updates
     * will reference this configuration. Passing {@code null} clears it.
     *
     * @param configuration 画中画尺寸约束配置 / the PIP size constraint configuration.
     */
    public static void setConfiguration(VeLivePictureInPictureConfiguration configuration) {
        mConfiguration = configuration;
    }

    /**
     * 启动画中画：校验悬浮窗权限、检查播放器与视频尺寸，弹出悬浮窗并将其 {@link SurfaceView}
     * 交给播放器输出，同时同步当前播放状态。若权限未授予或播放器为空则直接返回。
     * <p>
     * Start PIP: verifies the overlay permission and the player state, shows
     * the floating window, redirects the player output to the window's
     * {@link SurfaceView}, and syncs the current playing state to the PIP UI.
     * The call is a no-op if permission is missing or the player is null.
     */
    public void startPictureInPicture() {
        if (!isPermissionGranted()) {
            return;
        }
        if (mPlayer == null) {
            return;
        }
        if (lastVideoWidth <= 0 || lastVideoHeight <= 0) {
            setVideoSize(1080, 1920);
        }
        manager.startPictureInPicture();
        SurfaceView surfaceView = manager.startPictureInPicture();
        if (surfaceView != null) {
            mPlayer.setSurfaceHolder(surfaceView.getHolder());
        }
        manager.setPlaying(mPlayer.isPlaying());
    }

    /**
     * 关闭画中画：隐藏悬浮窗，并将播放器输出还原到宿主页面之前保存的
     * {@link SurfaceHolder} 或 {@link Surface} 上，从而恢复主界面渲染。
     * 若权限未授予或播放器为空则直接返回。
     * <p>
     * Stop PIP: hides the floating window and restores the player output to
     * the {@link SurfaceHolder} or {@link Surface} previously saved by the
     * host page, so the main UI can render again. The call is a no-op if
     * permission is missing or the player is null.
     */
    public void stopPictureInPicture() {
        if (!isPermissionGranted()) {
            return;
        }
        if (mPlayer == null) {
            return;
        }
        manager.stopPictureInPicture();
        if (mSurfaceHolder != null) {
            mPlayer.setSurfaceHolder(mSurfaceHolder);
        } else if (mSurface != null) {
            mPlayer.setSurface(mSurface);
        }
    }

    /**
     * 设置画中画事件监听器，用于接收如恢复播放点击等回调；传入 {@code null} 可清除监听。
     * <p>
     * Register an observer to receive PIP events (e.g. resume click). Pass
     * {@code null} to remove the previously registered observer.
     *
     * @param observer 事件回调实例 / the observer instance.
     */
    public void setObserver(VeLivePictureInPicturePlayerManagerObserver observer) {
        mObserver = observer;
    }

    /**
     * 根据当前视频宽高更新画中画悬浮窗的目标尺寸。若与上次相同则忽略；否则优先按
     * {@link VeLivePictureInPictureConfiguration} 计算，未配置时按屏幕比例回退到默认策略。
     * <p>
     * Update the target PIP window size according to the current video width
     * and height. The call is skipped when the size does not change; otherwise
     * the {@link VeLivePictureInPictureConfiguration} is applied first, and a
     * screen-ratio based fallback is used when no configuration is present.
     *
     * @param width  视频宽度（像素） / video width in pixels.
     * @param height 视频高度（像素） / video height in pixels.
     */
    public void setVideoSize(int width, int height) {
        if (width > 0 && height > 0) {
            if (width == lastVideoWidth && height == lastVideoHeight) {
                return;
            }
            lastVideoWidth = width;
            lastVideoHeight = height;

            if (mConfiguration != null && setWindowSizeByConfiguration(width, height)) {
                return;
            }
            android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            int targetMaxWidth = 0;
            int targetMaxHeight = 0;
            int targetMinWidth = 0;
            int targetMinHeight = 0;
            if (width > height) {
                targetMaxWidth = (int) (metrics.widthPixels * 0.8f);
                targetMaxHeight = (int) (targetMaxWidth * (height / (float) width));
            } else {
                targetMaxWidth = (int) (metrics.widthPixels * 0.5f);
                targetMaxHeight = (int) (targetMaxWidth * (height / (float) width));
            }
            targetMinWidth = targetMaxWidth / 2;
            targetMinHeight = targetMaxHeight / 2;
            boolean initWithMaxSize = true;
            if (VeLivePictureInPicturePlayerManager.width == targetMinWidth && VeLivePictureInPicturePlayerManager.height == targetMinHeight) {
                initWithMaxSize = false;
            }
            manager.setPictureInPictureWindowSize(targetMaxWidth, targetMaxHeight, targetMaxWidth / 2, targetMaxHeight / 2, initWithMaxSize);
        }
    }

    private int getTargetWidth(int targetHeight, int width, int height) {
        return (int) (targetHeight * (width / (float) height));
    }
    private int getTargetHeight(int targetWidth, int width, int height) {
        return (int) (targetWidth * (height / (float) width));
    }

    private boolean setWindowSizeByConfiguration(int width, int height) {
        int targetMaxWidth = -1;
        int targetMaxHeight = -1;
        int targetMinWidth = -1;
        int targetMinHeight = -1;
        android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();

        if (mConfiguration.maxWidth > 0 || mConfiguration.minWidth > 0) {
            int maxWidth = 0;
            int minWidth = 0;
            if (mConfiguration.maxWidth > 0) {
                maxWidth = mConfiguration.maxWidth;
            }
            if (mConfiguration.minWidth > 0){
                minWidth = mConfiguration.minWidth;
            }
            if (maxWidth <= 0) {
                maxWidth = minWidth * 2;
                if (maxWidth > metrics.widthPixels) {
                    maxWidth = metrics.widthPixels - 2 * WINDOW_VIEW_MARGIN;
                }
            }
            if (minWidth <= 0) {
                minWidth = maxWidth / 2;
            }

            targetMaxWidth = maxWidth;
            targetMaxHeight = getTargetHeight(targetMaxWidth, width, height);
            if (targetMaxHeight > metrics.heightPixels) {
                targetMaxHeight = metrics.heightPixels - 2 * WINDOW_VIEW_MARGIN;
                targetMaxWidth = getTargetWidth(targetMaxHeight, width, height);
            }

            targetMinWidth = minWidth;
            targetMinHeight = getTargetHeight(targetMinWidth, width, height);
        } else if (mConfiguration.maxHeight > 0 || mConfiguration.minHeight > 0) {
            int maxHeight = 0;
            int minHeight = 0;
            if (mConfiguration.maxHeight > 0) {
                maxHeight = mConfiguration.maxHeight;
            }
            if (mConfiguration.minHeight > 0){
                minHeight = mConfiguration.minHeight;
            }
            if (maxHeight <= 0) {
                maxHeight = minHeight * 2;
                if (maxHeight > metrics.heightPixels) {
                    maxHeight = metrics.heightPixels;
                }
            }

            targetMaxHeight = maxHeight;
            targetMaxWidth = getTargetWidth(targetMaxHeight, width, height);
            if (targetMaxWidth > metrics.widthPixels) {
                targetMaxWidth = metrics.widthPixels - 2 * WINDOW_VIEW_MARGIN;
                targetMaxHeight = getTargetHeight(targetMaxWidth, width, height);
            }

            if (minHeight <= 0) {
                minHeight = targetMaxHeight / 2;
            }
            targetMinHeight = minHeight;
            targetMinWidth = getTargetWidth(targetMinHeight, width, height);
        } else if ((mConfiguration.maxWidthRatio > 0 && mConfiguration.maxWidthRatio <= 1) || (mConfiguration.minWidthRatio > 0 && mConfiguration.minWidthRatio <= 1)) {
            int maxWidth = 0;
            int minWidth = 0;
            if ((mConfiguration.maxWidthRatio > 0 && mConfiguration.maxWidthRatio <= 1)) {
                maxWidth = (int) (mConfiguration.maxWidthRatio * metrics.widthPixels);
            }
            if ((mConfiguration.minWidthRatio > 0 && mConfiguration.minWidthRatio <= 1)){
                minWidth = (int) (mConfiguration.minWidthRatio * metrics.widthPixels);
            }
            if (maxWidth <= 0) {
                maxWidth = minWidth * 2;
                if (maxWidth > metrics.widthPixels) {
                    maxWidth = metrics.widthPixels - 2 * WINDOW_VIEW_MARGIN;
                }
            }
            if (minWidth <= 0) {
                minWidth = maxWidth / 2;
            }

            targetMaxWidth = maxWidth;
            targetMaxHeight = getTargetHeight (targetMaxWidth, width, height);
            if (targetMaxHeight > metrics.heightPixels) {
                targetMaxHeight = metrics.heightPixels - 2 * WINDOW_VIEW_MARGIN;
                targetMaxWidth = getTargetWidth(targetMaxHeight, width, height);
            }

            targetMinWidth = minWidth;
            targetMinHeight = getTargetHeight (targetMinWidth, width, height);
        } else if ((mConfiguration.maxHeightRatio > 0 && mConfiguration.maxHeightRatio <= 1) || (mConfiguration.minHeightRatio > 0 && mConfiguration.minHeightRatio <= 1)) {
            int maxHeight = 0;
            int minHeight = 0;
            if (mConfiguration.maxHeightRatio > 0 && mConfiguration.maxHeightRatio <= 1) {
                maxHeight = (int) (mConfiguration.maxHeightRatio * metrics.heightPixels);
            }
            if (mConfiguration.minHeightRatio > 0 && mConfiguration.minHeightRatio <= 1){
                minHeight =(int) (mConfiguration.minHeightRatio * metrics.heightPixels);
            }
            if (maxHeight <= 0) {
                maxHeight = minHeight * 2;
                if (maxHeight > metrics.heightPixels) {
                    maxHeight = metrics.heightPixels;
                }
            }

            targetMaxHeight = maxHeight;
            targetMaxWidth = getTargetWidth (targetMaxHeight, width, height);
            if (targetMaxWidth > metrics.widthPixels) {
                targetMaxWidth = metrics.widthPixels - 2 * WINDOW_VIEW_MARGIN;
                targetMaxHeight = getTargetHeight (targetMaxWidth, width, height);
            }

            if (minHeight <= 0) {
                minHeight = targetMaxHeight / 2;
            }
            targetMinHeight = minHeight;
            targetMinWidth = getTargetWidth(targetMinHeight, width, height);
        }

        if (targetMaxWidth > 0 && targetMaxHeight > 0) {
            boolean setMaxSize = true;
            if (VeLivePictureInPicturePlayerManager.width == targetMinWidth && VeLivePictureInPicturePlayerManager.height == targetMinHeight) {
                setMaxSize = false;
            }
            manager.setPictureInPictureWindowSize(targetMaxWidth, targetMaxHeight, targetMinWidth, targetMinHeight, setMaxSize);
            return true;
        }
        return false;
    }

    /**
     * 判断当前应用是否已获得悬浮窗权限（画中画显示所必需）。
     * <p>
     * Whether the current application has been granted the overlay permission,
     * which is required to display the PIP floating window.
     *
     * @return 已授予返回 {@code true} / {@code true} if the permission is granted.
     */
    public boolean isPermissionGranted() {
        return manager.isPermissionGranted();
    }

    /**
     * 发起悬浮窗权限申请，权限流程结束后通过回调返回结果。若系统已授予权限，回调会以
     * {@code true} 立即触发。
     * <p>
     * Request the overlay permission. The result is delivered via the given
     * callback. If the permission is already granted, the callback fires with
     * {@code true} immediately.
     *
     * @param callback 权限申请结果回调 / callback for the permission result.
     */
    public void requestPermission(VePictureInPictureManager.VePictureInPicturePermissionCallback callback) {
        manager.requestPictureInPicturePermission(callback);
    }

    /**
     * 判断画中画悬浮窗当前是否处于显示状态。
     * <p>
     * Whether the PIP floating window is currently displayed.
     *
     * @return 显示中返回 {@code true} / {@code true} if the PIP window is showing.
     */
    public boolean isPictureInPictureShowing() {
        return manager.isPictureInPictureShowing();
    }

    /**
     * 缓存宿主页面的 {@link SurfaceHolder}，用于 {@link #stopPictureInPicture()} 时把播放输出还原到该 Holder。
     * <p>
     * Cache the host page's {@link SurfaceHolder} so that the player output can
     * be restored to it inside {@link #stopPictureInPicture()}.
     *
     * @param surfaceHolder 宿主页 SurfaceView 的 Holder / the holder from the host SurfaceView.
     */
    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
    }

    /**
     * 缓存宿主页面的 {@link Surface}（例如来自 {@code Surface}），用于
     * {@link #stopPictureInPicture()} 时把播放输出还原到该 Surface。
     * <p>
     * Cache the host page's {@link Surface} (for example from a
     * {@code TextureView}) so that the player output can be restored to it
     * inside {@link #stopPictureInPicture()}.
     *
     * @param surface 宿主页的 Surface / the surface from the host page.
     */
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    /**
     * 销毁画中画管理器：释放对播放器与底层 {@link VePictureInPictureManager} 的引用，
     * 调用后不应再复用该实例。宿主页面应在退出前主动调用，避免内存泄漏。
     * <p>
     * Destroy the manager: release references to the player and to the
     * underlying {@link VePictureInPictureManager}. The instance must not be
     * reused after this call. Host pages should invoke it before exit to avoid
     * memory leaks.
     */
    public void destroy() {
        if (mPlayer != null) {
            mPlayer = null;
        }
        if (manager != null) {
            manager.stopPictureInPicture();
            manager = null;
        }
        if (mSurface != null) {
            mSurface = null;
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder = null;
        }
    }

    private class VeLivePictureInPictureManagerObserver implements VePictureInPictureManager.VePictureInPictureObserver {
        @Override
        public void onStopPictureInPicture() {
            stopPictureInPicture();
            if (mObserver != null) {
                mObserver.onClickResumeAction();
            }
        }

        @Override
        public void onClosePictureInPicture() {
            stopPictureInPicture();
        }

        @Override
        public void onPlayerPlay() {
            if (mPlayer != null) {
                mPlayer.play();
            }
        }

        @Override
        public void onPlayerPause() {
            if (mPlayer != null) {
                mPlayer.stop();
            }
        }

        @Override
        public void onPlayerStop() {
            if (mPlayer != null) {
                mPlayer.stop();
            }
        }

        @Override
        public void onWindowFrameChanged(int x, int y, int w, int h) {
            VeLivePictureInPicturePlayerManager.x = x;
            VeLivePictureInPicturePlayerManager.y = y;
            VeLivePictureInPicturePlayerManager.width = w;
            VeLivePictureInPicturePlayerManager.height = h;
        }
    }
}
