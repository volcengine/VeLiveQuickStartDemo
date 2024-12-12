/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;

import android.text.TextUtils;

import com.ss.avframework.utils.TimeUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VeLiveFileReader {
    private String mFilePath;
    private int mFrameSize;
    private Callback mCallback;
    private long mInterval;
    private boolean mEnable = false;
    private ByteBuffer mByteBuffer;

    public VeLiveFileReader() {
        mByteBuffer = ByteBuffer.allocateDirect(1920 * 1080 * 3 / 2); //  I420 一帧大小  
    }

    public interface Callback {
        void onByteBuffer(ByteBuffer byteBuffer, long pts);
    }

    private void setEnable(boolean enable) {
        mEnable = enable;
    }

    private boolean getEnable() {
        return mEnable;
    }

    public void start(String path, int frameSize, int interval, Callback callback) {
        if (TextUtils.isEmpty(path) || frameSize <= 0 || interval < 0 || callback == null) {
            return;
        }
        setEnable(true);
        mFilePath = path;
        mFrameSize = frameSize;
        mCallback = callback;
        mInterval = interval;
        new Thread(() -> {
            while (getEnable()) {
                doBusiness();
            }
        }).start();
    }

    public void stop() {
        setEnable(false);
    }

    private void doBusiness() {
        long timeUs = TimeUtils.nanoTime() / 1000;
        byte[] data = new byte[mFrameSize];
        long waitTimeUs = mInterval;
        FileInputStream dis = null;
        try {
            dis = new FileInputStream(mFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (mEnable) {
            try {
                int readCount = 0;
                while (readCount < mFrameSize) {
                    int tmp = dis.read(data, readCount, mFrameSize - readCount);
                    if (tmp >= 0) {
                        readCount += tmp;
                    } else {
                        break;
                    }
                }
                if (readCount < mFrameSize) {
                    break;
                }
                mByteBuffer.position(0);
                mByteBuffer.put(data);
                mByteBuffer.flip();
                mCallback.onByteBuffer(mByteBuffer, timeUs);
                timeUs += mInterval * 1000;
                waitTimeUs = timeUs - (TimeUtils.nanoTime() / 1000);
                if (waitTimeUs > 0) {
                    try {
                        Thread.sleep(waitTimeUs / 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                try {
                    dis.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                break;
            }
        }
        if (dis != null) {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
