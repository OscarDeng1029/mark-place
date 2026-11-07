# 原生 App 的解析与地图服务

业务模块来自用户指定的本机项目 `Documents/Codex/2026-10-27/new-chat/outputs/xiaohongshu-map`，保留其公开笔记读取、网络重试、规则/模型识别、原文校验、高德门店核对和坐标转换逻辑。`server.mjs` 为当前原生 App 提供精简 JSON API，不提供网页界面。App 页面与地图交互全部由 Compose 绘制。

需要 Node.js 22.12+，无 npm 依赖。

```powershell
Copy-Item .env.example .env
# 在 .env 填写高德 Web 服务 Key 与模型配置
node server.mjs
node --test tests/*.test.mjs
```

默认监听 `127.0.0.1:3212`。如需改变监听地址/端口，设置进程环境变量 `HOST` / `PORT`。本机已复用参考项目的配置；`.env` 不提交 Git，也不会打包进 APK。配置查询只返回服务是否就绪，不返回密钥。

Android 模拟器使用 `adb reverse tcp:3212 tcp:3212` 连接电脑服务，App 默认地址为 `http://127.0.0.1:3212`。桌面版使用相同地址。真机或其他电脑需部署可访问的 HTTPS 服务，再在导入页「解析服务」修改地址；当前本机调试服务未部署到公网。

接口：

- `GET /api/config`：服务就绪状态。
- `POST /api/parse`：`url`（完整分享内容或短链）或 `text`、`city`，先读取正文并识别店名。App 使用 `deferMatching:true` 及时展示候选。
- `POST /api/match`：核对上一阶段的 `shops`、`city`、`title`、`text`，坐标仅来自高德候选。
- `POST /api/search`：按 `keyword`、`city` 查询门店，由用户选择分店。
- `POST /api/address/geocode`：按详细 `address`、`city` 查坐标，返回地址精度供用户核对。
- `GET /tiles/road/:z/:x/:y.png`：高德普通地图的备用瓦片通道。

链接读取仅支持公开页面，不读取评论或视频，不绕过登录。受限、删除、网络与模型故障返回实际原因；App 保留输入，支持粘贴正文或手动填写。服务日志不输出签名链接、正文或密钥。

2026-11-07 实际联网验证：参考短链读取 7 家店，模型识别后 6 家成功核对高德门店，TonTon 小屋保持待定位；真实瓦片 HTTP 200。回归样本中的网页与模型响应只用于自动测试，App 正常使用始终调用实际服务。
