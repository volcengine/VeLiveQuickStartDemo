/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.os.Looper;

import com.ss.avframework.opengl.YuvHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VeLiveDeviceCapture implements Camera.PreviewCallback {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final int VIDEO_FPS = 15;
    private ByteBuffer mNV21Buffer;
    private ByteBuffer mI420Buffer;

    private ByteBuffer mI420RotatedBuffer;

    private VideoFrameReadListener mVideoFrameReadListener;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    private boolean mStopped = true;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        synchronized (this) {
            if (mStopped) {
                return;
            }
        }
        if (mVideoFrameReadListener != null) {
            synchronized (this) {
                convertToI420(data);
                rotateI420Buffer(270);
                mI420RotatedBuffer.rewind();
                mVideoFrameReadListener.onFrameAvailable(mI420RotatedBuffer, HEIGHT, WIDTH);
            }
        }
    }


    public interface VideoFrameReadListener {
        void onFrameAvailable(ByteBuffer data, int width, int height);
    }

    public void start(final VideoFrameReadListener videoFrameReadListener) {
        mVideoFrameReadListener = videoFrameReadListener;
        synchronized (this) {
            mStopped = false;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                mSurfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCamera.setPreviewTexture(mSurfaceTexture);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewFormat(ImageFormat.NV21);
                parameters.setPreviewSize(WIDTH, HEIGHT);
                parameters.setPreviewFrameRate(VIDEO_FPS);
                byte[] buffer = new byte[WIDTH * HEIGHT * 3 / 2];
                mCamera.addCallbackBuffer(buffer);
                mCamera.setParameters(parameters);
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        synchronized (this) {
            mStopped = true;
        }

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    private void convertToI420(byte [] data) {
        int yuvSize = WIDTH * HEIGHT * 3 / 2;
        if (mNV21Buffer == null || mNV21Buffer.capacity() < yuvSize) {
            mNV21Buffer = ByteBuffer.allocateDirect(yuvSize);
        }
        mNV21Buffer.clear();
        mNV21Buffer.put(data);

        if (mI420Buffer == null || mI420Buffer.capacity() < yuvSize) {
            mI420Buffer = ByteBuffer.allocateDirect(yuvSize);
        }
        mI420Buffer.clear();

        //NV21 -> I420
        mNV21Buffer.position(0);
        mNV21Buffer.limit(WIDTH * HEIGHT);
        ByteBuffer nv12SliceY = mNV21Buffer.slice();
        mNV21Buffer.position(WIDTH * HEIGHT);
        mNV21Buffer.limit(WIDTH * HEIGHT * 3 / 2);
        ByteBuffer nv12SliceUV = mNV21Buffer.slice();
        mI420Buffer.position(0);
        mI420Buffer.limit(WIDTH * HEIGHT);
        ByteBuffer i420SliceY = mI420Buffer.slice();
        mI420Buffer.position(WIDTH * HEIGHT);
        mI420Buffer.limit(WIDTH * HEIGHT * 5 / 4);
        ByteBuffer i420SliceU = mI420Buffer.slice();
        mI420Buffer.position(WIDTH * HEIGHT * 5 / 4);
        mI420Buffer.limit(WIDTH * HEIGHT * 3 / 2);
        ByteBuffer i420SliceV = mI420Buffer.slice();
        YuvHelper.NV21ToI420(nv12SliceY, WIDTH,
                nv12SliceUV, WIDTH,
                i420SliceY, WIDTH,
                i420SliceU, WIDTH / 2,
                i420SliceV, WIDTH / 2,
                WIDTH, HEIGHT);
    }

    private void rotateI420Buffer(int rotation) {
        int yuvSize = WIDTH * HEIGHT * 3 / 2;
        if (mI420RotatedBuffer == null || mI420RotatedBuffer.capacity() < yuvSize) {
            mI420RotatedBuffer = ByteBuffer.allocateDirect(yuvSize);
        }
        mI420RotatedBuffer.clear();

        mI420Buffer.position(0);
        mI420Buffer.limit(WIDTH * HEIGHT);
        ByteBuffer i420SliceY = mI420Buffer.slice();
        mI420Buffer.position(WIDTH * HEIGHT);
        mI420Buffer.limit(WIDTH * HEIGHT * 5 / 4);
        ByteBuffer i420SliceU = mI420Buffer.slice();
        mI420Buffer.position(WIDTH * HEIGHT * 5 / 4);
        mI420Buffer.limit(WIDTH * HEIGHT * 3 / 2);
        ByteBuffer i420SliceV = mI420Buffer.slice();
        int rotationMode = (rotation + 360 * 10) % 360;
        YuvHelper.I420Rotate(i420SliceY, WIDTH,
                i420SliceU, WIDTH / 2,
                i420SliceV, WIDTH / 2,
                mI420RotatedBuffer, WIDTH, HEIGHT, rotationMode);
    }
}