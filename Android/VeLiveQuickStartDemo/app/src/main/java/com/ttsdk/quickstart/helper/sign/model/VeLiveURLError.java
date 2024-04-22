/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign.model;

import com.google.gson.annotations.SerializedName;

public class VeLiveURLError {
    @SerializedName("Code")
    public String code;
    @SerializedName("CodeN")
    public int codeN;
    @SerializedName("Message")
    public String message;
    public VeLiveURLError(String code, int codeN, String message) {
        this.code = code;
        this.codeN = codeN;
        this.message = message;
    }

}
