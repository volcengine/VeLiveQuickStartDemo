## 火山引擎直播 SDK 快速集成 Demo

[![MIT License](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://raw.githubusercontent.com/volcengine/VeLiveQuickStartDemo/blob/master/Android/LICENSE)

### VeLiveQuickStartDemo 介绍

    VeLiveQuickStartDemo 基于 TTSDK 直播 SDK 开发，目前接入了基础、高级、互动三大功能模块。提供示例参考代码，后续会持续迭代。
    
    基础功能模块包含：基础推拉流能力
    
    高级功能模块包含：码率自适应推流、美颜推流、自采集推流、H265 硬编码推流、RTM 推流
    
    互动功能模块包含：主播和观众连麦、主播和主播 PK

### 目录结构说明

```tree
VeLiveQuickStartDemo/app/src/main/java/com/ttsdk/quickstart
├── app /// App Demo 入口页面
├── features /// 功能入口
│   ├── basic /// 基础功能入口
│   │   ├── PullStreamActivity.java /// 基础拉流能力
│   │   └── PushCameraActivity.java /// 摄像头采集基础推流能力
│   ├── advanced /// 高级功能入口
│   │   ├── PushAutoBitrateActivity.java /// 码率自适应
│   │   ├── PushBeautyActivity.java /// 美颜 
│   │   ├── PushCustomActivity.java /// 自采集推流
│   │   ├── PushH265CodecActivity.java  /// 265 硬编码推流
│   │   └── PushRTMActivity.java /// RTM 推流
│   └── interact  /// 互动直播
│       ├── link /// 主播和观众连麦
│       ├── manager /// 互动直播交互管理
│       └── pk /// 主播和主播PK
└── helper /// 一些工具类，包含 SDK 初始化和自采集
```

### Demo 运行

1. 使用 `Android Studio` 打开 `VeLiveQuickStartDemo` 目录

2. 点击 `Sync Now` 按钮同步 SDK

3. 修改 `VeLiveSDKHelper.java` 中的常量值

    ```java

    public class VeLiveSDKHelper {

        // AppID
        public static String TTSDK_APP_ID = "";
        /*
        License 名称，当前 Demo文件存放在与本文件同级目录下，如果做SDK快速验证，可直接替换 ttsdk.lic 文件内容
        */
        public static String TTSDK_LICENSE_NAME = "ttsdk.lic";

        /*
        rtmp, RTM, Quic 推流地址
        生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
        */
        public static String LIVE_PUSH_URL = "";
        public static String LIVE_RTM_PUSH_URL = "";

        /*
        RTM, rtmp、flv、m3u8 拉流地址
        生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
        */
        public static String LIVE_PULL_URL = "";
        public static String LIVE_RTM_PULL_URL = "";


        /*
        互动直播AppID
        */
        public static String RTC_APPID = "";

        /*
        互动直播主播房间ID
        */
        public static String RTC_ROOM_ID = "";

        /*
        互动直播主播用户ID
        */
        public static String RTC_USER_ID = "";

        /*
        互动直播主播用户 Token
        生成方式：使用主播房间ID和主播用户ID在RTC控制台生成
        https://console.volcengine.com/rtc/listRTC
        */
        public static String RTC_USER_TOKEN = "";

        /*
        主播与主播PK时，对方主播的房间ID
        */
        public static String RTC_OTHER_ROOM_ID = "";

        /*
        主播与主播PK时，当前主播加入对方主播房间内的Token
        生成方式：使用当前主播的用户ID和对方主播的房间ID在控制台生成
        */
        public static String RTC_OTHER_ROOM_TOKEN = "";
    }
    ```

4. 编译运行

### TTSDK 直播 SDK 集成方式

1. 配置 Maven 仓库

    打开根目录下的 `build.gradle`。

    定义 Maven 仓库。并配置仓库服务器的 URL。URL 为 <https://artifact.bytedance.com/repository/Volcengine/> 集成代码示例如下所示。

    ```gradle
    allprojects {
        repositories {
            google()
            mavenCentral()
            maven {
                url "https://artifact.bytedance.com/repository/Volcengine/" // volc public maven repo
            }
        }
    }

    apply from: 'https://ve-vos.volccdn.com/script/vevos-repo-base.gradle'
    ```

2. 配置环境和依赖

    2.1. 打开主工程下的 build.gradle。

    2.2. 在 defaultConfig 中配置 App 使用的 CPU 架构。支持 armv7a 和 arm64 架构。

    2.3. 在依赖 dependencies 中添加 TTSDK 的在线集成地址。

    ```gradle
    android {
        defaultConfig {
            ndk {
                //设置 SO 库架构，支持 armv7a 和 arm64 架构。
                abiFilters 'armeabi-v7a', 'arm64-v8a'
            }
            // APPLOG_SCHEME 为必填参数，空缺会造成编译失败。
            // online 表示直播场景。
            manifestPlaceholders.put("APPLOG_SCHEME", "online")
        }
    }

    dependencies {
        // 添加 TTSDK 在线集成地址，推荐使用最新稳定版，获取方式请参考[SDK 下载]章节
        implementation 'com.bytedanceapi:ttsdk-ttlivepush:x.x.x.x'
        implementation 'com.bytedanceapi:ttsdk-ttlivepull:x.x.x.x'
        //添加第三方依赖。
        implementation 'commons-net:commons-net:3.6'
        // 开启 HTTPDNS 解析
        implementation 'com.squareup.okhttp3:okhttp:4.2.1'
    }
    ```

    2.4. 单击 Sync Now 按钮同步 SDK，SDK 将自动下载集成到工程。如果出现集成失败，请检查您与 jcenter 仓库的网络连接。

### SDK 下载和集成

[SDK 下载](https://www.volcengine.com/docs/6469/81447)

[SDK 集成](https://www.volcengine.com/docs/6469/81445)

[SDK 初始化](https://www.volcengine.com/docs/6469/119124)

### 相关文档

[产品文档](https://www.volcengine.com/docs/6469/76298)

#### 推流接入
[基础功能接入](https://www.volcengine.com/docs/6469/85694)

[进阶功能接入](https://www.volcengine.com/docs/6469/81475)

[消息回调与异常说明](https://www.volcengine.com/docs/6469/81478)

#### 拉流接入

[基础功能接入](https://www.volcengine.com/docs/6469/95393)

[进阶功能接入](https://www.volcengine.com/docs/6469/95391)

[消息回调与异常说明](https://www.volcengine.com/docs/6469/95392)

#### ChangeLog

[版本历史](https://www.volcengine.com/docs/6469/124692)

## 技术支持

[技术支持](https://www.volcengine.com/contact/product)
