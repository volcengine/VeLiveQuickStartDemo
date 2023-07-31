/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.features.interact.manager;

import com.ss.bytertc.engine.RTCStream;
import com.ss.bytertc.engine.SubscribeConfig;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.data.AVSyncState;
import com.ss.bytertc.engine.handler.IRTCRoomEventHandler;
import com.ss.bytertc.engine.type.LocalStreamStats;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.RTCRoomStats;
import com.ss.bytertc.engine.type.RemoteStreamStats;
import com.ss.bytertc.engine.type.StreamRemoveReason;
import java.nio.ByteBuffer;

public class RtcRoomEventHandlerAdapter extends IRTCRoomEventHandler {
    @Override
    public void onLeaveRoom(RTCRoomStats stats) {

    }

    @Override
    public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {

    }

    @Override
    public void onStreamStateChanged(String roomId, String uid, int state, String extraInfo) {

    }

    @Override
    public void onRoomWarning(int warn) {

    }

    @Override
    public void onRoomError(int err) {

    }

    @Override
    public void onAVSyncStateChange(AVSyncState state) {

    }

    @Override
    public void onRoomStats(RTCRoomStats stats) {

    }

    @Override
    public void onUserJoined(UserInfo userInfo, int elapsed) {

    }

    @Override
    public void onUserLeave(String uid, int reason) {

    }

    @Override
    public void onTokenWillExpire() {

    }

    @Override
    public void onUserPublishStream(String uid, MediaStreamType type) {

    }

    @Override
    public void onUserUnpublishStream(String uid, MediaStreamType type, StreamRemoveReason reason) {

    }

    @Override
    public void onUserPublishScreen(String uid, MediaStreamType type) {

    }

    @Override
    public void onUserUnpublishScreen(String uid, MediaStreamType type, StreamRemoveReason reason) {

    }

    @Override
    public void onLocalStreamStats(LocalStreamStats stats) {

    }

    @Override
    public void onRemoteStreamStats(RemoteStreamStats stats) {

    }

    @Override
    public void onStreamRemove(RTCStream stream, StreamRemoveReason reason) {

    }

    @Override
    public void onStreamAdd(RTCStream stream) {

    }

    @Override
    public void onStreamSubscribed(int stateCode, String userId, SubscribeConfig info) {

    }

    @Override
    public void onStreamPublishSuccess(String uid, boolean isScreen) {

    }

    @Override
    public void onRoomMessageReceived(String uid, String message) {

    }

    @Override
    public void onRoomBinaryMessageReceived(String uid, ByteBuffer message) {

    }

    @Override
    public void onUserMessageReceived(String uid, String message) {

    }

    @Override
    public void onUserBinaryMessageReceived(String uid, ByteBuffer message) {

    }

    @Override
    public void onUserMessageSendResult(long msgid, int error) {

    }

    @Override
    public void onRoomMessageSendResult(long msgid, int error) {

    }

    @Override
    public void onVideoStreamBanned(String uid, boolean banned) {

    }

    @Override
    public void onAudioStreamBanned(String uid, boolean banned) {

    }
}
