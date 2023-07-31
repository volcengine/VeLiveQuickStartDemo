/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced.PullFeed;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ttsdk.quickstart.R;

import java.util.List;

public class PlayerAdaptor extends RecyclerView.Adapter<PlayerAdaptor.ViewHolder> {
    private static final String TAG = PlayerAdaptor.class.getSimpleName();
    private Context mContext;
    private List<VideoModel> mDatas;

    public PlayerAdaptor(final Context context, List<VideoModel> datas) {
        this.mContext = context;
        this.mDatas = datas;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.e(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_layout_cover,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.e(TAG,"onBindViewHolder:"+ position);
        holder.img_thumb.setImageResource(R.drawable.bg_room_change);
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private ViewGroup mTextureViewContainer;
        private TextureView mVideoView;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;
        private VeLivePlayer mPlayer;
        ImageView img_thumb;
        RelativeLayout rootView;
        public ViewHolder(View itemView) {
            super(itemView);
            mTextureViewContainer = itemView.findViewById(R.id.surface_container);
            img_thumb = itemView.findViewById(R.id.img_thumb);
            rootView = itemView.findViewById(R.id.root_view);
        }

        public void attachPlayer(VeLivePlayer player) {
            mPlayer = player;
            addTextureView();
        }

        private void addTextureView() {
            if (mTextureViewContainer.getChildCount() > 0) {
                mTextureViewContainer.removeAllViews();
            }
            mVideoView = new TextureView(mContext);
            if (mTextureViewContainer instanceof RelativeLayout) {
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                mTextureViewContainer.addView(mVideoView, layoutParams);
            } else if (mTextureViewContainer instanceof FrameLayout) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.CENTER;
                mTextureViewContainer.addView(mVideoView, layoutParams);
            }
            mVideoView.setSurfaceTextureListener(mTextureListener);
        }

        private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                Log.d(TAG, "onSurfaceTextureAvailable -- object:" + surfaceTexture);
                if (mSurfaceTexture == null) {
                    Log.e(TAG, "onSurfaceTextureAvailable mSurfaceTexture = null");
                }
                mSurfaceTexture = surfaceTexture;
                mSurface = new Surface(surfaceTexture);
                if (mPlayer != null) {
                    mPlayer.setSurface(mSurface);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                Log.d(TAG, "onSurfaceTextureSizeChanged ---");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.d(TAG, "onSurfaceTextureDestroyed ---- object:" + surfaceTexture);
                if (mPlayer != null) {
                    mPlayer.setSurface(mSurface);
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
    }
}
