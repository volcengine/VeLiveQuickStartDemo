/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import com.ss.videoarch.liveplayer.VeLivePlayer;
import com.ss.videoarch.liveplayer.VeLivePlayerAudioFrame;
import com.ss.videoarch.liveplayer.VeLivePlayerConfiguration;
import com.ss.videoarch.liveplayer.VeLivePlayerDef;
import com.ss.videoarch.liveplayer.VeLivePlayerError;
import com.ss.videoarch.liveplayer.VeLivePlayerObserver;
import com.ss.videoarch.liveplayer.VeLivePlayerStatistics;
import com.ss.videoarch.liveplayer.VeLivePlayerStreamData;
import com.ss.videoarch.liveplayer.VeLivePlayerVideoFrame;
import com.ss.videoarch.liveplayer.VideoLiveManager;
import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.features.advanced.PullFeed.OnViewPagerListener;
import com.ttsdk.quickstart.features.advanced.PullFeed.PlayerAdaptor;
import com.ttsdk.quickstart.features.advanced.PullFeed.PlayerPagerLayoutManager;
import com.ttsdk.quickstart.features.advanced.PullFeed.VideoModel;
import java.util.ArrayList;
import java.util.List;

public class PullFeedActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    public static final String URL_LIST_KEY = "URL_LIST_KEY";
    private static final String TAG = PullFeedActivity.class.getSimpleName();
    SwipeRefreshLayout mSwipeLayout;
    private RecyclerView mRecyclerView;
    private PlayerAdaptor mAdapter;
    private PlayerPagerLayoutManager mLayoutManager;

    private List<VideoModel> mDataList;
    private List<VeLivePlayer> mVeLivePlayerList;
    private List<Boolean> mIsPreloading;
    private static final int PLAYER_NUM = 3;
    private ArrayList<String> mUrlList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_feed);

        mUrlList = getIntent().getStringArrayListExtra(URL_LIST_KEY);

        mDataList = new ArrayList<>();
        mVeLivePlayerList = new ArrayList<>();
        mIsPreloading = new ArrayList<>();

        initView();
        initListener();

        for (int i = 0; i < PLAYER_NUM; i++) {
            VeLivePlayer livePlayer = new VideoLiveManager(getApplicationContext());
            livePlayer.setObserver(new MyPlayerObserver(i));

            VeLivePlayerConfiguration config = new VeLivePlayerConfiguration();
            config.enableHardwareDecode = true;
            livePlayer.setConfig(config);

            mVeLivePlayerList.add(livePlayer);
            mIsPreloading.add(false);
        }
        onRefresh();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy");
        super.onDestroy();
        releaseAllPlayer();
    }

    @Override
    public void onRefresh() {
        stopAllVideos();
        mSwipeLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                fetch(0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        for (int i = 0; i < PLAYER_NUM; i++) {
                            bindPlayer(i);
                        }
                        mRecyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                attachViewHolder(0);
                                startPlayer(0);
                                if (mDataList.size() > 1) {
                                    preLoad(1);
                                }
                                if (mSwipeLayout.isRefreshing()) {
                                    mSwipeLayout.setRefreshing(false);
                                }
                            }
                        }, 300);
                    }
                });
            }
        }).start();
    }

    private void fetch(final int cursor) {
        if (cursor == 0) {
            mDataList.clear();
        }
        ArrayList<String> urlList = new ArrayList<String>(1);
        urlList = mUrlList;
        for (int i = 0; i < urlList.size(); i++) {
            VideoModel videoModel = new VideoModel();
            videoModel.url = urlList.get(i);
            mDataList.add(videoModel);
        }
    }

    private void initView() {
        mSwipeLayout = findViewById(R.id.swipe_layout);
        mSwipeLayout.setOnRefreshListener(this);

        mLayoutManager = new PlayerPagerLayoutManager(this, OrientationHelper.VERTICAL);
        mAdapter = new PlayerAdaptor(this, mDataList);
        mRecyclerView = findViewById(R.id.recycler);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initListener() {
        mLayoutManager.setOnViewPagerListener(new OnViewPagerListener() {

            @Override
            public void onInitComplete() {
                Log.e(TAG,">>>>>> onInitComplete");
            }

            @Override
            public void onPageDisplay(boolean isNext,int position) {
                Log.d(TAG,">>>>>> onPageDisplay, position: " + position + ", isNext: " + isNext);
                attachViewHolder(position);
                setMute(position, true);
                startPlayer(position);
            }

            @Override
            public void onPageRelease(boolean isNext,int position) {
                Log.d(TAG,">>>>>> onPageRelease, position: " + position + ", isNext: " + isNext);
                stopPlayer(position);
            }

            @Override
            public void onPageSelected(int position,boolean isBottom) {
                Log.d(TAG,">>>>>> onPageSelected, position: " + position + ", isBottom: " + isBottom);
                setMute(position, false);
                if (position > 0) {
                    bindPlayer(position - 1);
                }
                if (!isBottom) {
                    bindPlayer(position + 1);
                    preLoad(position + 1);
                }
            }
        });
    }

    private void bindPlayer(int position) {
        VeLivePlayerStreamData streamData = new VeLivePlayerStreamData();
        streamData.mainStreamList = new ArrayList<>();
        VeLivePlayerStreamData.VeLivePlayerStream main = new VeLivePlayerStreamData.VeLivePlayerStream();
        VideoModel model = mDataList.get(position);
        main.url = model.url;
        streamData.mainStreamList.add(main);
        VeLivePlayer player = getVeLivePlayer(position);
        player.setPlayStreamData(streamData);
    }

    private void attachViewHolder(int position) {
        if (null != mRecyclerView.findViewHolderForAdapterPosition(position)) {
            PlayerAdaptor.ViewHolder viewHolder = (PlayerAdaptor.ViewHolder)mRecyclerView.findViewHolderForAdapterPosition(position);
            viewHolder.attachPlayer(getVeLivePlayer(position));
        }
    }

    private void startPlayer(int position) {
        VideoModel model = mDataList.get(position);
        Log.i("HHQQ", "startPlayer, position:" + position % PLAYER_NUM + "url:" + model.url);
        if (mIsPreloading.get(position % PLAYER_NUM)) {
            mIsPreloading.set(position % PLAYER_NUM, false);
            return;
        }
        VeLivePlayer player = getVeLivePlayer(position);
        player.play();
    }

    private void preLoad(int position) {
        Log.i("HHQQ", "preLoad, position:" + position % PLAYER_NUM);
        startPlayer(position);
        mIsPreloading.set(position % PLAYER_NUM, true);
    }

    private void setMute(int position, boolean enable) {
        VeLivePlayer player = getVeLivePlayer(position);
        player.setMute(enable);
    }

    private void stopPlayer(int position) {
        Log.i("HHQQ", "stopPlayer, position:" + position % PLAYER_NUM);
        VeLivePlayer player = getVeLivePlayer(position);
        player.stop();
    }

    private void stopAllVideos() {
        for (int i = 0; i < PLAYER_NUM; i++) {
            stopPlayer(i);
        }
        for (int i = 0; i < mIsPreloading.size(); i++) {
            mIsPreloading.set(i, false);
        }
    }

    private void releaseAllPlayer() {
        for (VeLivePlayer player : mVeLivePlayerList) {
            player.destroy();
        }
    }

    private VeLivePlayer getVeLivePlayer(int position) {
        return mVeLivePlayerList.get(position % PLAYER_NUM);
    }

    class MyPlayerObserver implements VeLivePlayerObserver {
        private int number;
        public MyPlayerObserver(int n) {
            number = n;
        }

        @Override
        public void onError(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {}

        @Override
        public void onFirstVideoFrameRender(VeLivePlayer veLivePlayer, boolean b) {}

        @Override
        public void onFirstAudioFrameRender(VeLivePlayer veLivePlayer, boolean b) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mIsPreloading.get(number)) {
                        mIsPreloading.set(number, false);
                        Log.i("HHQQ", "onFirstAudioFrameRender, position:" + number);
                        stopPlayer(number);
                    }
                }
            });
        }

        @Override
        public void onStallStart(VeLivePlayer veLivePlayer) {}

        @Override
        public void onStallEnd(VeLivePlayer veLivePlayer) {}

        @Override
        public void onVideoRenderStall(VeLivePlayer veLivePlayer, long l) {}

        @Override
        public void onAudioRenderStall(VeLivePlayer veLivePlayer, long l) {}

        @Override
        public void onResolutionSwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerResolution veLivePlayerResolution, VeLivePlayerError veLivePlayerError, VeLivePlayerDef.VeLivePlayerResolutionSwitchReason veLivePlayerResolutionSwitchReason) {}

        @Override
        public void onVideoSizeChanged(VeLivePlayer veLivePlayer, int i, int i1) {}

        @Override
        public void onReceiveSeiMessage(VeLivePlayer veLivePlayer, String s) {}

        @Override
        public void onMainBackupSwitch(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerStreamType veLivePlayerStreamType, VeLivePlayerError veLivePlayerError) {}

        @Override
        public void onPlayerStatusUpdate(VeLivePlayer veLivePlayer, VeLivePlayerDef.VeLivePlayerStatus veLivePlayerStatus) {}

        @Override
        public void onStatistics(VeLivePlayer veLivePlayer, VeLivePlayerStatistics veLivePlayerStatistics) {}

        @Override
        public void onSnapshotComplete(VeLivePlayer veLivePlayer, Bitmap bitmap) {}

        @Override
        public void onRenderVideoFrame(VeLivePlayer veLivePlayer, VeLivePlayerVideoFrame veLivePlayerVideoFrame) {}

        @Override
        public void onRenderAudioFrame(VeLivePlayer veLivePlayer, VeLivePlayerAudioFrame veLivePlayerAudioFrame) {}

        @Override
        public void onStreamFailedOpenSuperResolution(VeLivePlayer veLivePlayer, VeLivePlayerError veLivePlayerError) {}
    }
}