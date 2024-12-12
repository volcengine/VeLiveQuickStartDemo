/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;

import android.content.Context;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//  管理内置媒体资源  
public class VeLiveMediaResourceMgr {
    public static void prepareResource(Context context, int resId, String name, PrepareListener listener) {
        new Thread(() -> {
            try {
                copyRawFileToSdcard(context, resId, name, listener);
            } catch (IOException e) {
                e.printStackTrace();
                listener.onFail();
            }
        }).start();
    }

    public interface PrepareListener {
        default void onSuccess() {}
        default void onFail() {}
    }

    public static void copyRawFileToSdcard(Context context , int resId, String name, PrepareListener listener) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = context.getResources().openRawResource(resId);
            outputStream = new FileOutputStream(name);
            byte[] buffer = new byte[2048];
            int length;
            while (-1 != (length = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            if (null != inputStream) {
                inputStream.close();
            }
            if (null != outputStream) {
                outputStream.close();
            }
        }
        listener.onSuccess();
    }
}
