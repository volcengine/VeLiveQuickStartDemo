/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.pk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import android.support.v7.app.AppCompatActivity;

import com.ttsdk.quickstart.R;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.VeLiveRTCTokenMaker;

public class PKActivity extends AppCompatActivity {
    private final String LOG_TAG = "PKActivity";

    private EditText mRoomIDText;
    private EditText mUserIDText;
    private EditText mTokenText;

    private EditText mOtherRoomIDText;

    private EditText mOtherTokenText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pkactivity);
        mRoomIDText = findViewById(R.id.room_id_text_view);
        mUserIDText = findViewById(R.id.user_id_text_view);
        mOtherRoomIDText = findViewById(R.id.other_room_id_text_view);
    }

    public void startControl(View view) {
        if (!checkParams()) {
            return;
        }
        String roomId = mRoomIDText.getText().toString();
        String userId = mUserIDText.getText().toString();
        String otherRoomId = mOtherRoomIDText.getText().toString();
        Intent intent = new Intent(PKActivity.this, PKAnchorActivity.class);
        intent.putExtra(PKAnchorActivity.ROOM_ID, roomId);
        intent.putExtra(PKAnchorActivity.USER_ID, userId);

        String roomToken = VeLiveRTCTokenMaker.shareMaker().genDefaultToken(roomId, userId);
        intent.putExtra(PKAnchorActivity.TOKEN, roomToken);
        intent.putExtra(PKAnchorActivity.OTHER_ROOM_ID, otherRoomId);

        String otherRoomToken = VeLiveRTCTokenMaker.shareMaker().genDefaultToken(otherRoomId, userId);
        intent.putExtra(PKAnchorActivity.OTHER_TOKEN, otherRoomToken);
        startActivity(intent);
    }

    private boolean checkParams() {
        if (!mRoomIDText.getText().toString().isEmpty()
                && !mUserIDText.getText().toString().isEmpty()
                && !mOtherRoomIDText.getText().toString().isEmpty()) {
            return true;
        }
        Log.e(LOG_TAG, "Please Check Params");
        return false;
    }
}