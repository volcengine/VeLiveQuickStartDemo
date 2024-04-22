/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

public class VeLiveURLRootModel <T> {
    @SerializedName("ResponseMetadata")
    public VeLiveURLResponseMetadata metadata;
    @SerializedName("Result")
    public T result;

    public static <T> VeLiveURLRootModel<T> parser(String jsonStr, Class<T> retCls) {
        Gson gson = new Gson();
        VeLiveURLRootModel<T> model = gson.fromJson(jsonStr, VeLiveURLRootModel.class);
        model.result = gson.fromJson(gson.toJson(model.result), retCls);
        return model;
    }
    public boolean isSuccessful() {
        if (result == null) {
            if (metadata.error == null) {
                metadata.error = new VeLiveURLError("result is null", -1, "result is null");
            }
            return false;
        }
        return metadata.error == null || metadata.error.codeN == 0 || metadata.error.codeN == 200;
    }
}

