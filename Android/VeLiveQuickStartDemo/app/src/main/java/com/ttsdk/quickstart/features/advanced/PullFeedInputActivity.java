/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.advanced;

import static com.ttsdk.quickstart.features.advanced.PullFeedActivity.URL_LIST_KEY;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.app.home.MainActivity;

import java.util.ArrayList;

public class PullFeedInputActivity extends AppCompatActivity {

    private EditText mUrlListEt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_feed_input);
        mUrlListEt = findViewById(R.id.url_list_input_view);
    }

    public void startBtnClick(View view) {
        ArrayList <String> urlList = new ArrayList<>();
        String[] splitsStrings = mUrlListEt.getText().toString().split("\n");
        for (int i = 0; i < splitsStrings.length; i++) {
            String str = splitsStrings[i];
            str = str.trim();
            if (!str.isEmpty()) {
                urlList.add(str);
            }
        }
        Intent intent = new Intent(PullFeedInputActivity.this, PullFeedActivity.class);
        intent.putExtra(URL_LIST_KEY, urlList);
        startActivity(intent);
    }
}