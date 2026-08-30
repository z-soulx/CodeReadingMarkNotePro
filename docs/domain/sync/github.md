# GitHub Provider 差异页

`GitHubSyncProvider`（位于 `sync/github/`）是 `SyncProvider` 策略目前唯一的实现。
本页只写它区别于通用同步逻辑的部分，通用语义看 `README.md`。

## 错误解析（formatApiError）

GitHub 的报错不是纯 JSON，插件按响应内容自适应解析：

- **JSON 响应**：提取 `message` 字段
- **HTML 响应**（如网关/限流页）：提取 `<title>`，并解码 HTML 实体
  （`&middot;`、`&mdash;` 等转回可读字符）
- **按 HTTP 状态码附加上手建议**：
  - `401` -> 检查 token
  - `404` -> 检查仓库是否存在/可访问
  - 其余常见状态码各有对应提示

目标：用户看到的报错永远是「发生了什么 + 该检查什么」，而不是原始报文。

## 路径编码

含非 ASCII 字符的文件路径按**路径段**逐段 URL 编码，避免整条路径一次性编码破坏 `/` 分隔。

## 交互约定

- token 与仓库地址存于应用级 `codeReadingNoteSync.xml`（跨项目复用）
- 远端以项目名（清洗 Windows 非法字符后）作为标识区分不同项目的数据
- 时间戳比较依赖远端文件的 last-modified 信息（`getRemoteLastModifiedTime`）

> 本页基于 `.ai/ARCHITECTURE.md` 既有描述迁移；新增 API 行为时先改代码再回流本页。
