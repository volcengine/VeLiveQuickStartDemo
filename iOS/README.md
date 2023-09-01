## 火山引擎直播 SDK 快速集成 iOS Demo

[![MIT License](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](https://raw.githubusercontent.com/volcengine/VeLiveQuickStartDemo/blob/master/iOS/LICENSE)

### VeLiveQuickStartDemo 介绍

    VeLiveQuickStartDemo 基于 TTSDK 直播 SDK 开发，目前接入了基础、高级、互动三大功能模块。提供示例参考代码，后续会持续迭代。
    
    基础功能模块包含：基础推拉流能力
    
    高级功能模块包含：码率自适应推流、美颜推流、自采集推流、H265 硬编码推流、RTM 推流
    
    互动功能模块包含：主播和观众连麦、主播和主播 PK

### 目录结构说明

```tree
VeLiveQuickStartDemo
└── VeLiveQuickStartDemo
    ├── App /// App Demo 入口页面
    ├── Features /// 功能入口
    │   ├── Basic /// 基础功能入口
    │   │   ├── PullStream /// 基础拉流能力
    │   │   └── PushCamera /// 摄像头采集基础推流能力
    │   ├── Advanced /// 高级功能入口
    │   │   ├── PushAutoBitrate /// 码率自适应
    │   │   ├── PushBeauty /// 美颜
    │   │   ├── PushCustom /// 自采集推流
    │   │   ├── PushH265Codec /// 265 硬编码推流
    │   │   └── PushRTM /// RTM 推流
    │   └── Interact /// 互动直播
    │       ├── Link /// 主播和观众连麦
    │       ├── Manager /// 互动直播交互管理
    │       └── PK /// 主播和主播PK
    ├── Helper /// 一些工具类，包含 SDK 初始化和自采集
```

### Demo 运行

1. 进入到 `VeLiveQuickStartDemo` 目录下

2. 执行 `pod install --repo-update`

3. 打开 `VeLiveQuickStartDemo.xcworkspace` 修改 `VeLiveSDKHelper.h` 中的宏定义值

    ```Objective-C

    // AppID
    #define TTSDK_APP_ID @""
    /*
    License 名称，当前 Demo文件存放在与本文件同级目录下，如果做SDK快速验证，可直接替换 ttsdk.lic 文件内容
    */
    #define TTSDK_LICENSE_NAME @"ttsdk.lic"

    /*
    rtmp, RTM, Quic 推流地址
    生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
    */
    #define LIVE_PUSH_URL @""
    #define LIVE_RTM_PUSH_URL @""

    /*
    RTM, rtmp、flv、m3u8 拉流地址
    生成方式：通过控制台生成 https://console.volcengine.com/live/main/locationGenerate
    */
    #define LIVE_PULL_URL @""
    #define LIVE_RTM_PULL_URL @""


    /*
    互动直播AppID
    */
    #define RTC_APPID @""

    /*
    互动直播主播房间ID
    */
    #define RTC_ROOM_ID @""

    /*
    互动直播主播用户ID
    */
    #define RTC_USER_ID @""

    /*
    互动直播主播用户 Token
    生成方式：使用主播房间ID和主播用户ID在RTC控制台生成
    https://console.volcengine.com/rtc/listRTC
    */
    #define RTC_USER_TOKEN @""

    /*
    主播与主播PK时，对方主播的房间ID
    */
    #define RTC_OTHER_ROOM_ID @""

    /*
    主播与主播PK时，当前主播加入对方主播房间内的Token
    生成方式：使用当前主播的用户ID和对方主播的房间ID在控制台生成
    */
    #define RTC_OTHER_ROOM_TOKEN @""
    ```

4. 编译运行

### TTSDK 直播 SDK 集成方式

---

#### 方式一：CocoaPods集成静态库

1. 添加 `source` 源

    ```ruby
    source 'https://github.com/volcengine/volcengine-specs.git'
    ```

2. 添加 `pod` 依赖

    ```ruby
    target 'xxx' do
    # 这里需要明确指定使用 subspecs => %w[LivePull-RTS LivePush-RTS]
    # 如果需要集成其它推拉流能力，可联系商务咨询
    # 可在 ChangeLog 获取版本号，推荐使用最新版本
    pod 'TTSDK', 'xxx-premium', :subspecs => %w[LivePull-RTS LivePush-RTS]
    end
    ```

3. 执行 `pod install --repo-update`

4. 引入头文件

    ```Objective-C
    /// 推流
    #import <TTSDK/VeLivePusher.h>
    /// 拉流
    #import <TTSDK/VeLivePlayer.h>
    /// TTSDK 初试化
    #import <TTSDK/TTSDKManager.h>
    ```

#### 方式二：CocoaPods集成动态库

1. 添加 `source` 源

    ```ruby
    source 'https://github.com/volcengine/volcengine-specs.git'
    ```

2. 添加 `pod` 依赖

    ```ruby
    target 'xxx' do
    # 这里需要明确指定使用 subspecs => %w[Video]
    # 如果需要集成其它推拉流能力，可联系商务咨询
    # 可在 ChangeLog 获取版本号，推荐使用最新版本
    pod 'TTSDKFramework', 'xxx-premium', :subspecs => %w[Video]
    end
    ```

3. 执行 `pod install --repo-update`

4. 引入头文件

    ```Objective-C
    /// 推流
    #import <TTSDKFramework/VeLivePusher.h>
    /// 拉流
    #import <TTSDKFramework/VeLivePlayer.h>
    /// TTSDK 初试化
    #import <TTSDKFramework/TTSDKManager.h>
    ```

### SDK 下载和集成

[SDK 下载](https://www.volcengine.com/docs/6469/81447)

[SDK 集成](https://www.volcengine.com/docs/6469/82185)

[SDK 初始化](https://www.volcengine.com/docs/6469/119125)

### 相关文档

[产品文档](https://www.volcengine.com/docs/6469/76298)

#### 推流接入

[基础功能接入](https://www.volcengine.com/docs/6469/97270)

[进阶功能接入](https://www.volcengine.com/docs/6469/97271)

[消息回调与异常说明](https://www.volcengine.com/docs/6469/97272)

#### 拉流接入

[基础功能接入](https://www.volcengine.com/docs/6469/97858)

[进阶功能接入](https://www.volcengine.com/docs/6469/97857)

[消息回调与异常说明](https://www.volcengine.com/docs/6469/97859)

#### ChangeLog

[版本历史](https://www.volcengine.com/docs/6469/124692)

## 技术支持

[技术支持](https://www.volcengine.com/contact/product)
