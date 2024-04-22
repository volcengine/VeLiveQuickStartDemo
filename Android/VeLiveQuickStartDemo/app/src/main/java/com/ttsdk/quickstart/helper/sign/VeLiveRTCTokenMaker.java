/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign;

import android.os.Build;

import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.rtctoken.AccessToken;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
不要在生产环境使用，生产环境的 Token 请在服务端生成
*/
public class VeLiveRTCTokenMaker {
    private static final VeLiveRTCTokenMaker _tokenMaker = new VeLiveRTCTokenMaker();
    private String appId;
    private String appKey;
    private VeLiveRTCTokenMaker() {
        this.appId = VeLiveSDKHelper.RTC_APPID;
        this.appKey = VeLiveSDKHelper.RTC_APPKEY;
    }
    public  static VeLiveRTCTokenMaker shareMaker() {
        return _tokenMaker;
    }
    public void setup(String appId, String appKey) {
        this.appId = appId;
        this.appKey = appKey;
    }
    public String genDefaultToken(String roomId, String userId) {
        AccessToken token = new AccessToken(appId, appKey, roomId, userId);
        token.AddPrivilege(AccessToken.Privileges.PrivPublishStream, 3600);
        token.AddPrivilege(AccessToken.Privileges.PrivSubscribeStream, 3600);
        return token.Serialize();
    }
}
