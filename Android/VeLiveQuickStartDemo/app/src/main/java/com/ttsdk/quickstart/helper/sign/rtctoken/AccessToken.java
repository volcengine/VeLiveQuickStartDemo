/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign.rtctoken;

import java.util.Arrays;
import java.util.TreeMap;


public class AccessToken {
    public enum Privileges {
        PrivPublishStream(0),

        // not exported, do not use directly
        privPublishAudioStream(1),
        privPublishVideoStream(2),
        privPublishDataStream(3),

        PrivSubscribeStream(4);

        public short intValue;

        Privileges(int value) {
            intValue = (short) value;
        }
    }

    public String appID;
    public String appKey;
    public String roomID;
    public String userID;
    public int issuedAt;
    public int expireAt;
    public int nonce;
    public TreeMap<Short, Integer> privileges;

    public byte[] signature;

    // Initializes token struct by required parameters.
    public AccessToken(String appID, String appKey, String roomID, String userID) {
        this.appID = appID;
        this.appKey = appKey;
        this.roomID = roomID;
        this.userID = userID;
        this.issuedAt = Utils.getTimestamp();
        this.nonce = Utils.randomInt();
        this.privileges = new TreeMap<>();
    }
    public AccessToken() {
        this.privileges = new TreeMap<>();
    }

    public static String getVersion() {
        return "001";
    }

    // AddPrivilege adds permission for token with an expiration.
    public void AddPrivilege(Privileges privilege, int expireTimestamp) {
        expireTimestamp = Utils.getTimestamp() + expireTimestamp;

        this.privileges.put(privilege.intValue, expireTimestamp);
        if (privilege.intValue == Privileges.PrivPublishStream.intValue) {
            this.privileges.put(Privileges.privPublishVideoStream.intValue, expireTimestamp);
            this.privileges.put(Privileges.privPublishAudioStream.intValue, expireTimestamp);
            this.privileges.put(Privileges.privPublishDataStream.intValue, expireTimestamp);
        }
    }

    // ExpireTime sets token expire time, won't expire by default.
    // The token will be invalid after expireTime no matter what privilege's expireTime is.
    public void ExpireTime(int expireTimestamp) {
        this.expireAt = expireTimestamp;
    }

    public byte[] packMsg() {
        ByteBuf buffer = new ByteBuf();

        return buffer.put(this.nonce).put(this.issuedAt).put(this.expireAt).put(this.roomID).put(this.userID).putIntMap(this.privileges).asBytes();
    }

    // Serialize generates the token string
    public String Serialize() {
        byte[] msg = this.packMsg();
        try {
            this.signature = Utils.hmacSign(this.appKey, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuf buffer = new ByteBuf();
        byte[] content = buffer.put(msg).put(signature).asBytes();

        return getVersion() + this.appID + Utils.base64Encode(content);
    }

    // Verify checks if this token valid, called by server side.
    public boolean Verify(String key) {
        if (this.expireAt > 0 && Utils.getTimestamp() > this.expireAt) {
            return false;
        }

        this.appKey = key;

        byte[] signature;

        try {
            signature = Utils.hmacSign(this.appKey, this.packMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return Arrays.equals(this.signature, signature);
    }

    // Parse retrieves token information from raw string
    public static AccessToken Parse(String raw) {
        AccessToken token = new AccessToken();

        if (raw.length() <= Utils.VERSION_LENGTH + Utils.APP_ID_LENGTH) {
            return token;
        }

        if (!getVersion().equals(raw.substring(0, Utils.VERSION_LENGTH))) {
            return token;
        }

        token.appID = raw.substring(Utils.VERSION_LENGTH, Utils.VERSION_LENGTH + Utils.APP_ID_LENGTH);
        byte[] content = Utils.base64Decode(raw.substring(Utils.VERSION_LENGTH + Utils.APP_ID_LENGTH));

        ByteBuf buffer = new ByteBuf(content);
        byte[] msg = buffer.readBytes();
        token.signature = buffer.readBytes();

        ByteBuf msgBuf = new ByteBuf(msg);
        token.nonce = msgBuf.readInt();
        token.issuedAt = msgBuf.readInt();
        token.expireAt = msgBuf.readInt();
        token.roomID = msgBuf.readString();
        token.userID = msgBuf.readString();
        token.privileges = msgBuf.readIntMap();

        return token;
    }

    @Override
    public String toString() {
        return "AccessToken{" +
                "appID='" + appID + '\'' +
                ", appKey='" + appKey + '\'' +
                ", roomID='" + roomID + '\'' +
                ", userID='" + userID + '\'' +
                ", issuedAt=" + issuedAt +
                ", expireAt=" + expireAt +
                ", nonce=" + nonce +
                ", privileges=" + privileges +
                ", signature=" + Arrays.toString(signature) +
                '}';
    }
}

