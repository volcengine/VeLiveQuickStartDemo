/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
/*
 * Copyright (C) 2025 bytedance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Create Date : 2025/3/19
 */

package com.bytedance.vepicutureinpicture;

import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class VePictureInPictureVideoActonLayer extends FrameLayout {

    public interface VePictureInPictureActionObserver {
        void onClosePictureInPicture();
        void onStopPictureInPicture();
        void onPlayerPlay();
        void onPlayerPause();
        void onPlayerStop();
    }


    private SurfaceView surfaceView;
    private VePictureInPictureMediaSeekBar mSeekBar;
    private ImageView mPlayPause;
    private VePictureInPictureActionObserver mObserver;
    private boolean enablePause;//use pause replace stop
    private boolean mPlaying;
    private float mCornerRadius;

    public VePictureInPictureVideoActonLayer(Context context) {
        super(context);

        initView();
    }

    public VePictureInPictureVideoActonLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initView();
    }

    public VePictureInPictureVideoActonLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView();
    }

    public VePictureInPictureVideoActonLayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    public void setPictureInPictureActionObserver(VePictureInPictureActionObserver observer) {
        mObserver = observer;
    }

    public void enablePlaybackBar(boolean enable) {
        if (enable) {
            mSeekBar.setVisibility(VISIBLE);
        } else {
            mSeekBar.setVisibility(GONE);
        }
    }

    public void enablePauseState(boolean enable) {
        enablePause = enable;
    }

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        mPlayPause.setSelected(playing);
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.ve_pip_video_action_layer, this, true);

        surfaceView = findViewById(R.id.pip_surface_view);

        mSeekBar = findViewById(R.id.pip_mediaSeekbar);
        mSeekBar.setTextVisibility(false);
        mSeekBar.seekBar.setThumb(null);
        mSeekBar.seekBar.setOnTouchListener((View v, MotionEvent event) -> true);
        mSeekBar.setOnSeekListener(new VePictureInPictureMediaSeekBar.OnUserSeekListener() {
            @Override
            public void onUserSeekStart(long startPosition) {
            }

            @Override
            public void onUserSeekPeeking(long peekPosition) {
            }

            @Override
            public void onUserSeekStop(long startPosition, long seekToPosition) {

            }
        });
        mPlayPause = findViewById(R.id.pip_playPause);
        mPlayPause.setOnClickListener(v -> togglePlayPause());

        findViewById(R.id.pip_actionBarToggle).setOnClickListener(v -> {
            if (mObserver != null) {
                mObserver.onStopPictureInPicture();
            }
        });
        findViewById(R.id.pip_actionBarClose).setOnClickListener(v -> {
            if (mObserver != null) {
                mObserver.onClosePictureInPicture();
            }
        });
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    private void togglePlayPause() {
        if (mObserver != null) {
            if (mPlaying) {
                if (enablePause) {
                    mObserver.onPlayerPause();
                } else {
                    mObserver.onPlayerStop();
                }
            } else {
                mObserver.onPlayerPlay();
            }
        }
        mPlaying = !mPlaying;
        mPlayPause.setSelected(mPlaying);
    }
}

class VePictureInPictureMediaSeekBar extends RelativeLayout {

    public final TextView text1;
    public final SeekBar seekBar;
    public final TextView text2;

    private boolean mTouchSeeking;
    private long mDuration;

    private OnUserSeekListener mOnUserSeekListener;

    public interface OnUserSeekListener {
        void onUserSeekStart(long startPosition);

        void onUserSeekPeeking(long peekPosition);

        void onUserSeekStop(long startPosition, long seekToPosition);
    }

    public VePictureInPictureMediaSeekBar(Context context) {
        this(context, null);
    }

    public VePictureInPictureMediaSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VePictureInPictureMediaSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(getContext()).inflate(R.layout.ve_media_player_seekbar, this);
        text1 = findViewById(R.id.text1);
        text2 = findViewById(R.id.text2);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int mStartSeekProgress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final float percent = progress / (float) seekBar.getMax();
                final long currentPosition = (int) (percent * mDuration);
                text1.setText(time2String(currentPosition));
                text2.setText(time2String(mDuration));

                if (!mTouchSeeking) return;
                if (mOnUserSeekListener != null && fromUser) {
                    mOnUserSeekListener.onUserSeekPeeking(currentPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mTouchSeeking) return;
                mTouchSeeking = true;
                mStartSeekProgress = seekBar.getProgress();
                final float startSeekPercent = mStartSeekProgress / (float) seekBar.getMax();
                final long startSeekPosition = (long) (startSeekPercent * mDuration);

                if (mOnUserSeekListener != null) {
                    mOnUserSeekListener.onUserSeekStart(startSeekPosition);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mTouchSeeking) return;
                mTouchSeeking = false;
                final float startSeekPercent = mStartSeekProgress / (float) seekBar.getMax();
                final float currentPercent = seekBar.getProgress() / (float) seekBar.getMax();

                final long startSeekPosition = (long) (startSeekPercent * mDuration);
                final long currentPosition = (long) (currentPercent * mDuration);

                if (mOnUserSeekListener != null) {
                    mOnUserSeekListener.onUserSeekStop(startSeekPosition, currentPosition);
                }
            }
        });
    }

    public void setDuration(long duration) {
        this.mDuration = duration;
        this.seekBar.setMax((int) Math.max(mDuration, 100));
        text2.setText(time2String(mDuration));
    }

    public void setCurrentPosition(long currentPosition) {
        if (!mTouchSeeking) {
            int progress = 0;
            if (mDuration > 0) {
                progress = (int) (currentPosition / (float) mDuration * seekBar.getMax());
            }
            seekBar.setProgress(progress);
        }
    }

    public void setCachePercent(int cachePercent) {
        seekBar.setSecondaryProgress((int) (cachePercent * (seekBar.getMax() / 100f)));
    }

    public void setOnSeekListener(OnUserSeekListener listener) {
        this.mOnUserSeekListener = listener;
    }

    public void setSeekEnabled(boolean enabled) {
        seekBar.setEnabled(enabled);
    }

    public void setTextVisibility(boolean visibility) {
        text1.setVisibility(visibility ? VISIBLE : GONE);
        text2.setVisibility(visibility ? VISIBLE : GONE);
    }

    /**
     * 格式化时间 timeMS -> HH:MM:SS
     */
    public static String time2String(long timeMs) {
        if (timeMs < 0) {
            return "";
        }
        long totalSeconds = timeMs / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        StringBuilder timeString = new StringBuilder();
        if (hours > 0) {
            if (hours < 10) {
                timeString.append('0');
            }
            timeString.append(hours).append(':');
        }

        if (minutes < 10) {
            timeString.append('0');
        }
        timeString.append(minutes).append(':');

        if (seconds < 10) {
            timeString.append('0');
        }
        timeString.append(seconds);

        return timeString.toString();
    }
}