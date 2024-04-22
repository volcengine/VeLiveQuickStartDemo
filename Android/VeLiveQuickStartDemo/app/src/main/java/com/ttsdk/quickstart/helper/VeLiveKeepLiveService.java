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
    public class Binder extends android.os.Binder {
        public VeLiveKeepLiveService getService() {
            return VeLiveKeepLiveService.this;
        }
    }
    private Binder mBinder = new Binder();

    public VeLiveKeepLiveService() {
    }
    private NotificationManager notificationManager;
    private String notificationId   = "keep_app_live";
    private String notificationName = "Running In Background";
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "===MyService  onCreate()");
        notificationName = getResources().getString(R.string.pip_running_in_background);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            //不震动
            channel.enableVibration(false);
            //静音
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(1, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("TTSDK")
                .setContentIntent(getIntent())
                .setContentText(getResources().getString(R.string.pip_running_in_background));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(notificationId);
        }
        return builder.build();
    }

    private PendingIntent getIntent() {
        Intent msgIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getPackageName());//获取启动Activity
        return PendingIntent.getActivity(
                getApplicationContext(),
                1,
                msgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        Log.d("TAG", "===MyService  onDestroy()");
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        super.onDestroy();

    }
}