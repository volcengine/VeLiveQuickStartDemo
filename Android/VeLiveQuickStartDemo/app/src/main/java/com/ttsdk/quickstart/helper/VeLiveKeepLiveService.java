/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.ttsdk.quickstart.R;

public class VeLiveKeepLiveService extends Service {
    private Binder mBinder = new Binder();
    private static final String NOTIFICATION_ID   = "VeLive_NotificationId";
    private static final String NOTIFICATION_NAME = "VeLive_NotificationName";
    private static final String TAG = VeLiveKeepLiveService.class.getSimpleName();

    public VeLiveKeepLiveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(0x01, getNotification());
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this).setDefaults(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            builder.setChannelId(NOTIFICATION_ID);
        }
        return builder.build();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    public class Binder extends android.os.Binder {
        public VeLiveKeepLiveService getService() {
            return VeLiveKeepLiveService.this;
        }
    }
}