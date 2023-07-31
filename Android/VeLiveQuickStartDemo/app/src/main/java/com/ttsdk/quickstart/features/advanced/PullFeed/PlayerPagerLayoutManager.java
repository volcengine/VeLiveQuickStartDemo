/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced.PullFeed;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class PlayerPagerLayoutManager extends LinearLayoutManager {
    private static final String TAG = PlayerPagerLayoutManager.class.getSimpleName();

    private PagerSnapHelper mPagerSnapHelper;
    private OnViewPagerListener mOnViewPagerListener;
    private RecyclerView mRecyclerView;
    private int mDrift;

    public PlayerPagerLayoutManager(Context context, int orientation) {
        super(context, orientation, false);
        init();
    }

    private void init() {
        mPagerSnapHelper = new PagerSnapHelper();
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mPagerSnapHelper.attachToRecyclerView(view);
        this.mRecyclerView = view;
        mRecyclerView.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener);
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        this.mDrift = dy;
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        this.mDrift = dx;
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    public void setOnViewPagerListener(OnViewPagerListener listener){
        this.mOnViewPagerListener = listener;
    }

    private final RecyclerView.OnChildAttachStateChangeListener mChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(View view) {
            boolean isNext = false;
            if (mDrift >= 0) {
                isNext = true;
            }
            Log.d(TAG, "OnChildAttachStateChangeListener->onChildViewAttachedToWindow getChildCount()=" + getChildCount() + "position=" + getPosition(view) + " isNext=" + isNext);
            if (mOnViewPagerListener != null && getChildCount() == 1) {
                mOnViewPagerListener.onInitComplete();
            } else if (mOnViewPagerListener != null && getChildCount() > 1) {
                mOnViewPagerListener.onPageDisplay(isNext,getPosition(view));
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {
            Log.d(TAG, "OnChildAttachStateChangeListener->onChildViewDetachedFromWindow");
            if (mDrift >= 0){
                if (mOnViewPagerListener != null) mOnViewPagerListener.onPageRelease(true,getPosition(view));
            }else {
                if (mOnViewPagerListener != null) mOnViewPagerListener.onPageRelease(false,getPosition(view));
            }

        }
    };

    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            switch (newState) {
                case RecyclerView.SCROLL_STATE_IDLE:
                    View viewIdle = mPagerSnapHelper.findSnapView(PlayerPagerLayoutManager.this);
                    int positionIdle = getPosition(viewIdle);
                    Log.d(TAG, "onScrollStateChanged->SCROLL_STATE_IDLE, positionIdle =" + positionIdle + " getChildCount =" + getChildCount());
                    if (mOnViewPagerListener != null && getChildCount() == 1) {
                        mOnViewPagerListener.onPageSelected(positionIdle,positionIdle == getItemCount() - 1);
                    }
                    break;
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    View viewDrag = mPagerSnapHelper.findSnapView(PlayerPagerLayoutManager.this);
                    int positionDrag = getPosition(viewDrag);
                    Log.d(TAG, "onScrollStateChanged->SCROLL_STATE_DRAGGING, positionDrag =" + positionDrag);
                    break;
                case RecyclerView.SCROLL_STATE_SETTLING:
                    View viewSettling = mPagerSnapHelper.findSnapView(PlayerPagerLayoutManager.this);
                    int positionSettling = getPosition(viewSettling);
                    Log.d(TAG, "onScrollStateChanged->SCROLL_STATE_SETTLING, positionSettling =" + positionSettling);
                    break;
            }
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        }
    };
}