/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.app.home.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ttsdk.quickstart.R;

import java.util.ArrayList;

public class HomeListAdapter extends BaseAdapter  {
    public static class HomeItem {
        public HomeItem(String title, String target) {
            this.title = title;
            this.target = target;
        }
        public HomeItem(String title) {
            this.title = title;
            this.target = null;
        }
        public String title;
        public String target;
    }

    public final ArrayList <HomeItem> mList;
    private final Context mContext;
    private final LayoutInflater mLInflater;

    public HomeListAdapter(Context context, ArrayList <HomeItem> list) {
        this.mList = list;
        this.mContext = context;
        this.mLInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return mList.get(position).target != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HomeItem item = mList.get(position);
        if (item.target == null) {
            convertView = mLInflater.inflate(R.layout.home_header, parent,false);
            TextView textView = convertView.findViewById(R.id.textLabel);
            textView.setText(item.title);
        } else {
            convertView = mLInflater.inflate(R.layout.home_item, parent, false);
            TextView textView = convertView.findViewById(R.id.textLabel);
            textView.setText(item.title);
        }
        return convertView;
    }
}
