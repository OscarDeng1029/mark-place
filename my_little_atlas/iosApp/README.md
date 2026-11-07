# iOS 原生版本

iOS 宿主直接调用共享 AtlasApp，不另写页面、不使用 WebView。手账首页 V3、当前恢复的大封面个人页 ProfileV2、收藏、地图、到访、草稿、资料编辑和分享均使用与 Android 相同的 Kotlin Compose 代码、字体与素材。

## 在 Mac 构建

需要 Xcode 16 或更新版本、JDK 21、XcodeGen（brew install xcodegen）。最低支持 iOS 16。

~~~bash
cd my_little_atlas
xcodegen generate --spec iosApp/project.yml
open iosApp/MyLittleAtlas.xcodeproj
~~~

选择 MyLittleAtlas scheme 和 iPhone 模拟器运行。Xcode 的构建脚本自动编译并接入 AtlasShared.framework，无需 CocoaPods。

~~~bash
# Apple Silicon Mac
bash ./gradlew :composeApp:iosSimulatorArm64Test
# Intel Mac
bash ./gradlew :composeApp:iosX64Test
~~~

iosArm64 用于 iPhone/iPad 真机；iosSimulatorArm64、iosX64 分别用于两种 Mac 模拟器。Windows 不具备 Apple 编译工具链，默认不会配置 Apple targets。

## 原生能力

- PhotosUI 单张选择器，照片复制到应用目录；记录保存相对路径，避免系统更换应用容器后丢失引用。
- Photos 添加权限，用于用户主动保存分享图片；等待实际写入结果后才显示成功。
- Files JSON 导入和导出，原记录通过原子文件写入保留。
- 系统分享、Safari 来源打开、高德/Apple 地图导航选择。
- 原生分享扩展接收文字/链接，在下次打开 App 时进入原有导入确认流程；也支持 mylittleatlas://import URL。
- CoreLocation 使用期间定位，包含授权、拒绝、超时和失败处理。
- URLSession 请求公开解析服务和地图瓦片，支持取消、大小限制、HTTPS 和系统 HTTP 缓存。
- Compose safe-area/键盘适配，强制浅色外观以保留既定视觉。

新安装为独立空库，与 Android 一样显示欢迎引导；设备之间不会自动同步本地数据。可使用已有 JSON 备份迁移文字记录，备份本身不含照片文件。

## 解析服务

iOS 模拟器默认 http://127.0.0.1:3212 指向当前 Mac。在 Mac 上运行 atlas-service/server.mjs 后即可连接。iPhone 真机的 127.0.0.1 指向手机本身，需要在 App 的“解析服务”设置中填入可访问的 HTTPS 地址。地图瓦片优先直连高德，解析和门店核对才需要服务。

## GitHub 构建产物

.github/workflows/ios.yml 在 macOS 上运行：

1. iOS 共享测试与桌面业务测试。
2. iPhone 模拟器构建、XCUITest 页面与入口验证、截图。
3. ARM64 真机 Release 编译。

Artifacts：

- MyLittleAtlas-iOS：模拟器 .app.zip、未签名的 MyLittleAtlas-unsigned.ipa。
- iOS-screenshots-and-tests：原生页面截图、测试结果、构建日志和生成的 Xcode 工程。

模拟器包用于 Mac 模拟器。未签名 IPA 不能直接安装到普通 iPhone；真机安装/TestFlight 需要在 Xcode 设置用户自己的 Apple Development Team 和相应签名。主 App 与分享扩展需同时启用 group.com.mylittleatlas.app 的 App Group。仓库不包含证书、私钥或描述文件。

## 页面验证

XCUITest 在独立 AtlasUITests 数据目录使用共享 DemoAtlas，仅 DEBUG 测试启动启用，不读取或重置用户库。覆盖翻页、切换 tab 后页码保留、收藏、原版个人页、资料编辑与草稿入口，并保存截图。

已完成 iPhone ARM64 Release、iPhone 16 Pro 模拟器和 14 项 iOS 共享测试验证。产物版本、校验值与模拟器截图见 [iOS 构建交付](../../design/atlas-ui-handoff-v2/implementation/iOS构建交付.md)。
