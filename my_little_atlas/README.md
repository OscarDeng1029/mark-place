# My Little Atlas / 私藏去处

根据调研方案和手账拼贴参考图制作的原生多平台 App 初稿。界面由 Kotlin Compose Multiplatform 直接绘制，Android、iOS 和桌面 JVM 共用 UI 与业务模型。

## 产品流程

收藏想去的地点，去过后写成手账；也可以直接填写新地点和城市、保存生成一页手账，无需先收藏。照片与文字选填，支持草稿续写。链接导入、收藏夹整理和主动分享继续可用。

底部导航为「手账 / 地图 / ＋ / 收藏 / 我的」。手账直接展示最近记录，目录在底部面板中按城市、月份和文字查找，点击直接翻到对应页。收藏在同页按城市及想去/去过筛选；仅一座城市时隐藏多余分类。原收藏夹和批量整理保留在收藏页的次要入口。「＋」只提供写手账、收藏地点。

实际运行截图、Android 安装包版本和验证结果见 [本次改版记录](../design/atlas-ui-handoff-v2/implementation/简化动线与城市分类落地.md)。

## 已实现

- Android 原生 Activity、系统图片选择器、系统文件备份导入/导出、原生图片和文字分享。
- 桌面 JVM 文件选择器、照片复制、JSON 原子写入、分享图片保存、系统地图/来源打开。
- 首页、真实地图、收藏夹、足迹日记、个人页、地点详情。
- 小红书分享链接/完整分享内容导入：读取公开笔记，识别店名，核对高德门店；候选默认未选，确认后才保存。匹配失败保留店名、原文依据与来源，支持手动搜店、查详细地址或先收藏待定位地点。
- 保留每行 `店名｜地址｜城市` 的结构化文本导入与原有手动添加。
- 高德真实中文瓦片由原生 Compose 显示，支持拖动、双指缩放、放大缩小、收藏夹/到访筛选、标记选择与返回收藏中心。WGS-84 存储与 GCJ-02 底图坐标分开处理；未定位地点有补全入口，虚构示例不上真实地图。
- 多次到访、日期与文字记录、到访照片、想再去开关、重复地点校验、备份恢复和损坏数据保护。
- 奶油纸色、鼠尾草绿、桃色、照片卡片、胶带与纸张纹理。示例图片随包提供，启动不依赖网络。

## 边界

内置地点是虚构样例，照片为氛围素材。真实地图与解析依赖联网；小红书仅支持可公开读取的正文，受限或已删除链接提示实际原因，可改贴正文。分店核对可能有误，导入时可以调整门店。JSON 备份不包含照片文件，照片仍保留在原设备的应用目录；旧版备份兼容新增坐标字段。

解析和高德查询使用独立的 [本机 API 服务](atlas-service/README.md)，密钥只保存在服务端 `.env`，不进入 APK 或 Git。Android 模拟器连接方式：先在 `atlas-service` 运行 `node server.mjs`，再执行 `adb reverse tcp:3212 tcp:3212`。当前模拟器已配置；真机需要可访问的 HTTPS 服务地址，在导入页「解析服务」设置。地图优先直连高德瓦片，失败时使用服务备用通道并自动重试。

## 构建和运行

需要 JDK 17 或更新版本、Android SDK 36。使用仓库附带的 `gradlew.bat`：

```powershell
gradlew.bat :composeApp:compileKotlinDesktop
gradlew.bat :composeApp:test
gradlew.bat :composeApp:assembleDebug
gradlew.bat :composeApp:run
```

普通构建使用 Google Maven 和 Maven Central 网络仓库，不要设置 `ATLAS_MAVEN_LOCAL`。APK 在 `composeApp/build/outputs/apk/debug/composeApp-debug.apk`。`dist/MyLittleAtlas-desktop` 中的 jar 需要本机安装 JDK 17+，可通过目录内的启动脚本运行。

Windows MSI 安装器还需下载 Compose Desktop 的 WiX 组件；本次网络下载超时，因此未生成 MSI。

iOS 已添加原生 Swift 宿主、Apple 编译目标和 UIKit 系统能力适配，共用现有页面与素材；构建、模拟器截图测试和真机签名说明见 [iOS 工程说明](iosApp/README.md)。GitHub Actions 使用 macOS 构建模拟器 App 和未签名真机 IPA，实际结果以对应 Actions 运行状态为准。

## 资源授权

示例照片来自 Unsplash Source 图片链接，仅用于原型气氛展示。界面字体为 Noto Serif SC，SIL Open Font License 文本随字体资源附带。
