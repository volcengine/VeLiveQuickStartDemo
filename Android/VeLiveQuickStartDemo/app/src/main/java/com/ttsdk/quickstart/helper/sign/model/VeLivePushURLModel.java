/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class VeLivePushURLModel {
    @SerializedName("PushURLList")
    public ArrayList<String> pushUrlList;
    @SerializedName("PushURLListDetail")
    public ArrayList<VeLivePushURLDetailModel> pushUrlListDetail;
    @SerializedName("TsOverSrtURLList")
    public ArrayList<String> tsOverSrtURLList;

    @SerializedName("RtmpOverSrtURLList")
    public ArrayList<String> rtmpOverSrtURLList;
    @SerializedName("RtmURLList")
    public ArrayList<String> rtmURLList;
    @SerializedName("WebTransportURLList")
    public ArrayList<String> webTransportURLList;
    public String getRtmpPushUrl() {
        return pushUrlList.get(0);
    }
    public String getRtmPushUrl() {
        return rtmURLList.get(0);
    }
}

