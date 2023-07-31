/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.link;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;

public class LinkActivity extends AppCompatActivity {

    private EditText mRoomIDText;
    private EditText mUserIDText;
    private EditText mTokenText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);
        mRoomIDText = findViewById(R.id.room_id_text_view);
        mRoomIDText.setText(VeLiveSDKHelper.RTC_ROOM_ID);
        mUserIDText = findViewById(R.id.user_id_text_view);
        mUserIDText.setText(VeLiveSDKHelper.RTC_USER_ID);
        mTokenText = findViewById(R.id.token_text_view);
        mTokenText.setText(VeLiveSDKHelper.RTC_USER_TOKEN);
    }

    public void anchorControl(View view) {
        if (!checkParams()) {
            return;
        }
        Intent intent = new Intent(LinkActivity.this, LinkAnchorActivity.class);
        intent.putExtra(LinkAnchorActivity.ROOM_ID, mRoomIDText.getText().toString());
        intent.putExtra(LinkAnchorActivity.USER_ID, mUserIDText.getText().toString());
        intent.putExtra(LinkAnchorActivity.TOKEN, mTokenText.getText().toString());
        startActivity(intent);
    }

    public void audienceControl(View view) {
        if (!checkParams()) {
            return;
        }
        Intent intent = new Intent(LinkActivity.this, LinkAudienceActivity.class);
        intent.putExtra(LinkAudienceActivity.ROOM_ID, mRoomIDText.getText().toString());
        intent.putExtra(LinkAudienceActivity.USER_ID, mUserIDText.getText().toString());
        intent.putExtra(LinkAudienceActivity.TOKEN, mTokenText.getText().toString());
        startActivity(intent);
    }

    private boolean checkParams() {
        return !mRoomIDText.getText().toString().isEmpty()
                && !mUserIDText.getText().toString().isEmpty()
                && !mTokenText.getText().toString().isEmpty();
    }
}