/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;

import com.google.gson.Gson;
import com.ttsdk.quickstart.helper.VeLiveSDKHelper;
import com.ttsdk.quickstart.helper.sign.model.VeLivePullURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLivePushURLModel;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLError;
import com.ttsdk.quickstart.helper.sign.model.VeLiveURLRootModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
/**
不要在生产环境使用，生产环境的推拉流地址请在服务端生成
*/
public class VeLiveURLGenerator {
    private static  String _accessKey = VeLiveSDKHelper.ACCESS_KEY_ID;
    private static  String _secretKey = VeLiveSDKHelper.SECRET_ACCESS_KEY;
    private static  String _vHost = VeLiveSDKHelper.LIVE_VHOST;
    private static  String _pushDomain = VeLiveSDKHelper.LIVE_PUSH_DOMAIN;
    private static  String _pullDomain = VeLiveSDKHelper.LIVE_PULL_DOMAIN;;
    private static Handler _mainHandler = new Handler(Looper.getMainLooper());

    public static void setup(String accessKey, String secretKey) {
        _accessKey = accessKey;
        _secretKey = secretKey;
        VeLiveURLSignTool.setup(accessKey, secretKey);
    }
    public static void setup(String vHost, String pushDomain, String pullDomain) {
        _vHost = vHost;
        _pushDomain = pushDomain;
        _pullDomain = pullDomain;
    }
    public interface VeLiveURLCallback<T> {
        public void onSuccess(VeLiveURLRootModel <T> model);
        public void onFailed(VeLiveURLError error);
    }
    public static <T> void genPushUrl(String app, String streamName, VeLiveURLCallback<VeLivePushURLModel> callback) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Vhost", _vHost);
            jsonObject.put("Domain", _pushDomain);
            jsonObject.put("App", app);
            jsonObject.put("Stream", streamName);
        } catch (JSONException ignore) {
        }
        sendRequest("GeneratePushURL", jsonObject.toString(), VeLivePushURLModel.class, callback);
    }
    public static <T> void genPullUrl(String app, String streamName, VeLiveURLCallback<VeLivePullURLModel> callback) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Vhost", _vHost);
            jsonObject.put("Domain", _pullDomain);
            jsonObject.put("App", app);
            jsonObject.put("Stream", streamName);
        } catch (JSONException ignore) {
        }
        sendRequest("GeneratePlayURL", jsonObject.toString(),VeLivePullURLModel.class, callback);
    }
    private static <T> void sendRequest(String action, String json, Class<T> retCls, VeLiveURLCallback<T> callback) {
        VeLiveHttpUtil.post(action, json, new VeLiveHttpUtil.VeLiveHttpCallback() {
            @Override
            public void onSuccess(String jsonString) {
                VeLiveURLRootModel<T> model = VeLiveURLRootModel.parser(jsonString, retCls);
                if (model.isSuccessful()) {
                    _mainHandler.post(() -> callback.onSuccess(model));
                } else {
                    _mainHandler.post(() -> callback.onFailed(model.metadata.error));
                }
            }
            @Override
            public void onError(int code, String msg) {
                callback.onFailed(new VeLiveURLError(msg, code, msg));
            }
        });
    }
}
