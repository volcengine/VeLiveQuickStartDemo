/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper.sign;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
/**
不要在生产环境使用，生产环境的推拉流地址请在服务端生成
*/
public class VeLiveURLSignTool {
    private static final String LOG_TAG = "VeLiveURLSignTool";
    private static final String host = "live.volcengineapi.com";
    private static final String region = "cn-north-1";
    private static final String version = "2023-01-01";
    private static final String relativePath = "/";
    private static final String service = "live";
    private static final String contentType = "application/json; charset=utf-8";
    private static final String signHeader = "content-type;host;x-content-sha256;x-date";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static String accessKey = VeLiveSDKHelper.ACCESS_KEY_ID;
    public static String secretKey = VeLiveSDKHelper.SECRET_ACCESS_KEY;

    public static void setup(String ak, String sk) {
        accessKey = ak;
        secretKey = sk;
    }

    public static Request signRequest(String action, String params, String body, String method) {
        String queryString = getCanonicalQueryString(action, version, params);
        String contentSha256 = hash(body);
        String dataString = getNowDate();
        String shortDateString = dataString.substring(0, 8);

        String canonicalString = method + "\n" +
                relativePath + "\n" +
                queryString + "\n" +
                "content-type:" + contentType + "\n" +
                "host:" + host + "\n" +
                "x-content-sha256:" + contentSha256 + "\n" +
                "x-date:" + dataString + "\n" +
                "\n" +
                signHeader + "\n" +
                contentSha256;

        String algorithm = "HMAC-SHA256";
        String canoicalHash = hash(canonicalString);
        String credentialScope = shortDateString + "/" + region + "/" + service + "/request";
        String strToSign = algorithm + "\n" + dataString + "\n" + credentialScope + "\n" + canoicalHash;
        byte[] signKey = getSignKey(shortDateString);
        String signature = bytes2Hex(hmacShaByte(signKey, string2Byte(strToSign)));
        String authorization = "HMAC-SHA256 Credential=" + accessKey + "/" + shortDateString +
                "/" + region + "/" + service + "/request, SignedHeaders=" + signHeader + ", Signature=" + signature;
        String url = "https://" + host + "?" + queryString;
       RequestBody requestBody = RequestBody.create(body, JSON);
        Log.i(LOG_TAG, authorization);
        return new Request.Builder().url(url)
                .addHeader("X-Date", dataString)
                .addHeader("Authorization", authorization)
                .addHeader("content-type", contentType)
                .addHeader("Host", host)
                .addHeader("X-content-sha256", contentSha256)
                .post(requestBody)
                .build();
    }

    private static byte[] getSignKey(String date) {
        byte[] dateKey = hmacShaByte(string2Byte(secretKey), string2Byte(date));
        byte[] regionKey = hmacShaByte(dateKey, string2Byte(region));
        byte[] serviceKey = hmacShaByte(regionKey, string2Byte(service));
        return hmacShaByte(serviceKey, string2Byte("request"));
    }

    public static String getCanonicalQueryString(String Action, String Version, String Params) {
        String oriquery = "Action=" + Action + "&Version=" + Version + "&" + Params;
        String[] querylist = oriquery.split("&");
        Arrays.sort(querylist);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < querylist.length; i++) {
            if (i != 0) {
                result.append("&");
            }
            result.append(querylist[i]);
        }
        return result.toString();
    }

    public static String hash(String strSrc) {// hash sha 256 算法 
        MessageDigest md;
        String strDes;
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return strDes;
    }

    private static String bytes2Hex(byte[] bts) {
        StringBuilder des = new StringBuilder();
        String tmp;
        for (byte bt : bts) {
            tmp = (Integer.toHexString(bt & 0xFF));
            if (tmp.length() == 1) {
                des.append("0");
            }
            des.append(tmp);
        }
        return des.toString();
    }

    public static String getNowDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.CHINESE);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private static byte[] string2Byte(String str) {
        try {
            return str.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] hmacShaByte(byte[] KEY, byte[] VALUE) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(KEY, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(VALUE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
