/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.app.home;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.app.home.adapter.HomeListAdapter;
import com.ttsdk.quickstart.app.home.adapter.HomeListAdapter.HomeItem;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.pandora.common.env.Env;

import java.util.ArrayList;

/*
 本文件展示SDK功能入口，只是UI展示无SDK具体功能。
 SDK 具体功能请查看Features 目录下相关能力
 */
public class MainActivity extends AppCompatActivity {

    private HomeListAdapter mListAdapter;
    private TextView mVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupListView();
        mVersionView = findViewById(R.id.version_textview);
        mVersionView.setText(Env.getVersion());
        VeLiveSDKHelper.checkPermission(this, 10010);
    }

    private void setupListView() {
        ListView mListView = findViewById(R.id.list_view);
        ArrayList <HomeItem> list = new ArrayList<>();
        list.add(new HomeItem(getResources().getString(R.string.Home_Basic_Features)));
        String basicPackage = "com.ttsdk.quickstart.features.basic.";
        list.add(new HomeItem(getResources().getString(R.string.Home_Camera_Push), basicPackage + "PushCameraActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Live_Pull_Streaming), basicPackage + "PullStreamActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Advanced_Features)));
        String advancedPackage = "com.ttsdk.quickstart.features.advanced.";
        list.add(new HomeItem(getResources().getString(R.string.Home_Live_Beauty_Filter), advancedPackage + "PushBeautyActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_RTM_Push_Streaming), advancedPackage + "PushRTMActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Custom_Push_Stream), advancedPackage + "PushCustomActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Push_Streaming_Bitrate_Adaptive), advancedPackage + "PushAutoBitrateActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_H265_Hardcoded), advancedPackage + "PushH265CodecActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Screen_Push), advancedPackage + "PushScreenActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Picture_In_Picture), advancedPackage + "PictureInPictureActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Interactive_Features)));
        String interactPackage = "com.ttsdk.quickstart.features.interact.";
        list.add(new HomeItem(getResources().getString(R.string.Home_Anchor_And_Audience_Mic), interactPackage + "link.LinkActivity"));
        list.add(new HomeItem(getResources().getString(R.string.Home_Anchor_VS_Anchor_Pk), interactPackage + "pk.PKActivity"));
        mListAdapter = new HomeListAdapter(this, list);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HomeItem item = mListAdapter.mList.get(position);
                if (item.target != null) {
                    try {
                        Class<?> cls = Class.forName(item.target);
                        Intent intent = new Intent(MainActivity.this, cls);
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }
}