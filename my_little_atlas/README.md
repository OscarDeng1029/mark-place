# My Little Atlas / 私藏去处

根据调研方案和手账拼贴参考图制作的原生多平台 App 初稿。界面由 Kotlin Compose Multiplatform 直接绘制，Android 和桌面 JVM 共用 UI 与业务模型，没有 WebView、Electron、Capacitor 或网页套壳。

## 产品流程

明确挑选灵感候选 -> 保存到私人地点库 -> 放入收藏夹 -> 到访打卡 -> 写下照片和文字 -> 主动生成分享卡。地点去过之后仍可标记“想再去”。

底部导航保留四个一级目的地：图鉴、地图、收藏、我的；正中新增按钮承载主要创建动作。足迹日记属于个人沉淀内容，从“我的”进入，首页足迹统计仍可直达，避免把低频记录页与日常浏览入口并列。

## 已实现

- Android 原生 Activity、系统图片选择器、系统文件备份导入/导出、原生图片和文字分享。
- 桌面 JVM 文件选择器、照片复制、JSON 原子写入、分享图片保存、系统地图/来源打开。
- 首页、离线地图交互示意、收藏夹、足迹日记、个人页、地点详情。
- 结构化文本导入：每行 `店名｜地址｜城市`。候选默认未选，确认后才保存；链接只作为来源，不会抓取或解析小红书页面。
- 多次到访、日期与文字记录、到访照片、想再去开关、重复地点校验、备份恢复和损坏数据保护。
- 奶油纸色、鼠尾草绿、桃色、照片卡片、胶带与纸张纹理。示例图片随包提供，启动不依赖网络。

## 边界

地图是离线布局示意，地点标记不代表真实坐标；内置地点是虚构样例，照片为氛围素材。尚未接入真实地图底图、POI 搜索或小红书自动解析，后续需要按授权、隐私和平台规则接入。JSON 备份不包含照片文件，照片仍保留在原设备的应用目录。

## 构建和运行

需要 JDK 17 或更新版本、Android SDK 36。使用仓库附带的 `gradlew.bat`：

```powershell
gradlew.bat :composeApp:compileKotlinDesktop
gradlew.bat :composeApp:test
gradlew.bat :composeApp:assembleDebug
gradlew.bat :composeApp:run
```

普通构建使用 Google Maven 和 Maven Central 网络仓库，不要设置 `ATLAS_MAVEN_LOCAL`。APK 在 `composeApp/build/outputs/apk/debug/composeApp-debug.apk`。`dist/MyLittleAtlas-desktop` 中的 jar 需要本机安装 JDK 17+，可通过目录内的启动脚本运行。

Windows MSI 安装器还需下载 Compose Desktop 的 WiX 组件；本次网络下载超时，因此未生成 MSI。iOS 当前未纳入可构建 target，需要后续添加 Swift 宿主工程和 UIKit 平台适配。

## 资源授权

示例照片来自 Unsplash Source 图片链接，仅用于原型气氛展示。界面字体为 Noto Serif SC，SIL Open Font License 文本随字体资源附带。
