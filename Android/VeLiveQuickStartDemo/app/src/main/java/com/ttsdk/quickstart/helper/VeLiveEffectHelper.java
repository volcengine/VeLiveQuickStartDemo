/**
 * Copyright (c) 2023 Beijing Volcano Engine Technology Ltd. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.ttsdk.quickstart.helper;

import android.content.Context;
import android.text.TextUtils;

import com.pandora.common.env.Env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VeLiveEffectHelper {

    /**
     * 获取证书文件路径
     */
    public static String getLicensePath(String name) {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath()
                + "/resource/LicenseBag.bundle/" + name;
    }

    /**
     * 获取模型文件路径
     */
    public static String getModelPath() {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath()
                + "/resource/ModelResource.bundle";
    }

    /**
     * 获取美颜文件路径
     */
    public static String getBeautyPathByName(String subPath) {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath()
                + "/resource/ComposeMakeup.bundle/ComposeMakeup/" + subPath;
    }

    /**
     * 获取贴纸文件路径
     * @param name 贴纸文件名称
     */
    public static String getStickerPathByName(String name) {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath()
                + "/resource/StickerResource.bundle/stickers/" + name;
    }

    /**
     * 获取滤镜文件路径
     * @param name 滤镜文件名称
     */
    public static String getFilterPathByName(String name) {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath()
                + "/resource/FilterResource.bundle/Filter/" + name;
    }

    /**
     * 初始化美颜资源文件
     * 将安装包内的资源文件拷贝到外部存储上
     */
    public static void initVideoEffectResource() {
        Context context = Env.getApplicationContext();
        File versionFile = new File(getExternalResourcePath(), "version");
        if (versionFile.exists()) {
            String oldVer = readVersion(versionFile.getAbsolutePath());
            copyAssetFolder(context, "resource/version", versionFile.getAbsolutePath());
            String newVer = readVersion(versionFile.getAbsolutePath());
            if (TextUtils.equals(oldVer, newVer)) {
                return;
            }
        } else {
            copyAssetFile(context, "resource/version", versionFile.getAbsolutePath());
        }
        updateEffectResource(context);
    }

    private static String readVersion(String fileName) {
        String version = "";
        try {
            FileInputStream fin = new FileInputStream(fileName);
            int length = fin.available();
            byte [] buffer = new byte[length];
            fin.read(buffer);
            version = new String(buffer);
            fin.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        return version;
    }

    private static void updateEffectResource(Context context) {
        File licensePath = new File(getExternalResourcePath(), "LicenseBag.bundle");
        removeFile(licensePath.getAbsolutePath());
        copyAssetFolder(context, "resource/LicenseBag.bundle", licensePath.getAbsolutePath());
        File modelPath = new File(getExternalResourcePath(), "ModelResource.bundle");
        removeFile(modelPath.getAbsolutePath());
        copyAssetFolder(context, "resource/ModelResource.bundle", modelPath.getAbsolutePath());
        File stickerPath = new File(getExternalResourcePath(), "StickerResource.bundle");
        removeFile(stickerPath.getAbsolutePath());
        copyAssetFolder(context, "resource/StickerResource.bundle", stickerPath.getAbsolutePath());
        File filterPath = new File(getExternalResourcePath(), "FilterResource.bundle");
        removeFile(filterPath.getAbsolutePath());
        copyAssetFolder(context, "resource/FilterResource.bundle", filterPath.getAbsolutePath());
        File composerPath = new File(getExternalResourcePath(), "ComposeMakeup.bundle");
        removeFile(composerPath.getAbsolutePath());
        copyAssetFolder(context, "resource/ComposeMakeup.bundle", composerPath.getAbsolutePath());
    }

    private static void removeFile(String filePath) {
        if(filePath == null || filePath.length() == 0){
            return;
        }
        try {
            File file = new File(filePath);
            if(file.exists()){
                removeFile(file);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static void removeFile(File file){
        // 如果是文件直接删除 
        if(file.isFile()){
            file.delete();
            return;
        }
        // 如果是目录，递归判断，如果是空目录，直接删除，如果是文件，遍历删除 
        if(file.isDirectory()){
            File[] childFile = file.listFiles();
            if(childFile == null || childFile.length == 0){
                file.delete();
                return;
            }
            for(File f : childFile){
                removeFile(f);
            }
            file.delete();
        }
    }

    public static String getExternalResourcePath() {
        return Env.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath() + "/resource/";
    }

    public static boolean copyAssetFolder(Context context, String srcName, String dstName) {
        try {
            boolean result = true;
            String fileList[] = context.getAssets().list(srcName);
            if (fileList == null) return false;

            if (fileList.length == 0) {
                result = copyAssetFile(context, srcName, dstName);
            } else {
                File file = new File(dstName);
                result = file.mkdirs();
                for (String filename : fileList) {
                    result &= copyAssetFolder(context, srcName + File.separator + filename, dstName + File.separator + filename);
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyAssetFile(Context context, String srcName, String dstName) {
        try {
            InputStream in = context.getAssets().open(srcName);
            File outFile = new File(dstName);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
