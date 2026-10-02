# WNLZ Plus

面向《我的世界》网易版（`com.netease.x19`）的第三方客户端外壳，提供卡密验证、载荷注入、插件与 XP 模块管理，界面采用 MIUI 风格。

- 包名：`com.wunelezi.plus`
- 版本：`1.1`（versionCode 2）
- 支持系统：Android 8.0 – Android 13（minSdk 26 / targetSdk 33）
- 开发语言：纯 Java（不使用 Kotlin）

## 功能特性

- **启动流程**：更新检查 → 公告 → 卡密验证 → 设备解绑。
- **载荷注入**：内置 XOR 加密载荷，运行时解密后写入宿主的 `content://` 提供者，并写入随机诱饵数据以降低被提取风险。
- **插件管理**：导入 zip 插件，逐项开关启用/禁用，长按进入多选批量删除（删除前二次确认）。
- **XP 模块管理**：导入 APK，模块名称直接读取自 APK 的 label，与插件完全区分存储。
- **MIUI 风格界面**：基于 HiMiuix 组件库，卡片式布局、底部导航、彩虹渐变标题。

## 项目结构

```
WNLZPlus/
├── app/                              # 应用模块
│   ├── payload/
│   │   └── base-1.apk                # 载荷源文件（构建期 XOR 加密为 Java 字节数组）
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/wunelezi/plus/
│   │   │   ├── MainActivity.java      # 单 Activity，承载登录与三个标签页
│   │   │   ├── core/                  # 核心逻辑
│   │   │   │   ├── SecurityGuard.java    # 启动门禁
│   │   │   │   ├── Payload.java          # 载荷解密
│   │   │   │   ├── Rc4Util.java          # RC4 加解密
│   │   │   │   ├── WeiYanApi.java        # 微验网络接口
│   │   │   │   ├── NetClient.java        # HttpURLConnection 封装
│   │   │   │   ├── Prefs.java            # 本地偏好存储
│   │   │   │   ├── ContentUriHelper.java # content:// 读写工具
│   │   │   │   ├── InjectionManager.java # 载荷注入与诱饵写入
│   │   │   │   ├── ModuleRepository.java # 插件/XP 模块仓库
│   │   │   │   └── ModuleInfo.java       # 模块数据模型
│   │   │   ├── ui/                    # 页面（登录/核心/插件/XP）
│   │   │   └── widget/                # 自定义控件（渐变标题）
│   │   └── res/                       # 布局、图标、主题等资源
│   └── build.gradle
├── HiMiuix/                          # MIUI 风格组件库（HChenX/HiMiuix）
├── gradle/wrapper/                   # Gradle Wrapper
├── keystore/                         # 签名文件（不纳入版本控制）
├── keystore.properties               # 签名配置（不纳入版本控制）
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 构建

### 环境要求

- JDK 17
- Android SDK（compileSdk 34、build-tools 34.0.0）
- Gradle 8.2（仓库自带 Wrapper）

### 签名配置

签名口令不写入版本库。请在项目根目录创建 `keystore.properties`：

```properties
storeFile=../keystore/wnlz.jks
storePassword=你的签名口令
keyAlias=你的密钥别名
keyPassword=你的签名口令
```

并将签名文件放到 `keystore/` 目录下。缺少该文件时，Release 将不进行签名。

### 构建命令

```bash
./gradlew assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

## 技术说明

- **载荷处理**：以 `app/payload/base-1.apk` 为源，构建期由 `generatePayloadData` 任务用 8 字节密钥做 XOR 加密，生成硬编码的 `PayloadData.java`；源文件不参与打包，运行时再解密使用。
- **模块存储**：插件元数据写入 `plugins.json`，XP 模块元数据写入 `xp.json`，均通过宿主的 `content://com.netease.x19.widget_file_provider/widget_external_files/` 读写。
  - 插件为 zip 包，名称取自包内 `info.json`。
  - XP 模块为 APK，名称取自 APK 的 label。
- **网络**：使用 `HttpURLConnection`，`network_security_config` 仅允许 HTTPS。
- **混淆加固**：Release 开启 R8 与资源压缩，使用自定义混淆字典，保留 MIUI 组件与入口类。

## 致谢

- UI 组件库：[HiMiuix](https://github.com/HChenX/HiMiuix)（HChenX）

## 免责声明

本项目仅供学习与技术研究使用，请勿用于任何违反相关法律法规或服务条款的场景。使用者需自行承担全部风险与责任。
