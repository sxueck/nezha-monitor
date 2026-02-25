# Android Kotlin 本地环境配置记录（2026-02-24）

## 1. 目的

记录本次为 `com.sxueck.monitor` 项目完成的本地开发环境安装与环境变量配置，便于后续复用与排障。

## 2. 已安装工具与版本

- JDK: Eclipse Temurin 17 (`17.0.18+8`)
- Gradle Wrapper: `8.7`
- Android SDK Command-line Tools: `19.0`
- Android SDK Platform-Tools: `36.0.2`
- Android SDK Build-Tools: `34.0.0`
- Android SDK Platform: `android-34`

## 3. 环境变量（用户级）

通过 PowerShell 持久化到当前 Windows 用户配置：

- `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`
- `ANDROID_SDK_ROOT=E:\git\monitor\tools\android-sdk`
- `ANDROID_HOME=E:\git\monitor\tools\android-sdk`

并在 `Path` 中追加：

- `%JAVA_HOME%\bin`
- `%ANDROID_SDK_ROOT%\platform-tools`
- `%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin`

> 注意：修改用户环境变量后，需要新开终端窗口才会生效。

## 4. 项目内固定配置

- 已生成 Gradle Wrapper：`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.properties`
- Wrapper 下载源：`https://services.gradle.org/distributions/gradle-8.7-bin.zip`
- 本地 SDK 路径写入：`local.properties`

`local.properties` 当前内容：

```properties
sdk.dir=E\:\\git\\monitor\\tools\\android-sdk
```

## 5. 常用命令

### 5.1 编译 Debug APK

```bash
./gradlew :app:assembleDebug
```

Windows CMD 版本：

```bat
gradlew.bat :app:assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 5.2 安装到指定 ADB 设备

```powershell
adb -s 26cde7d9 install -r E:\git\monitor\app\build\outputs\apk\debug\app-debug.apk
```

> 若出现 `more than one device/emulator`，请务必加 `-s <serial>` 指定设备。

## 6. 本次验证结果

- `:app:assembleDebug` 构建成功
- 已成功安装到设备：`26cde7d9`

## 7. 后续建议

- 保持 `compileSdk`、`targetSdk` 与已安装的 `platforms;android-34` 一致。
- 若更换电脑，仅需恢复本文件中的环境变量与 `local.properties` 后重新执行构建命令。
