# 集成边界：GitHub API

插件作为 GitHub REST API 的客户端。AI 配置及旧接口使用 `GitHubSyncProvider`
（策略模式实现，见 `.ai/adr/0001`）；工作空间笔记使用独立的条件写入接口 `GitHubNotesRemote`。

## 认证与配置

- Token + 仓库 URL 存于应用级 `codeReadingNoteSync.xml`，跨项目共享
- 同步仓库独立于项目仓库，笔记/AI 配置不进项目代码
- 远端按项目名（清洗 Windows 非法字符 `\ / : * ? " < > |` -> `_`）分目录存放
- 配置校验（`SyncConfig.validate()`）给出可操作的错误信息

## 远端布局

- 笔记：`CodeReadingNote.xml` 放在项目标识目录下
- AI 配置：`ai-configs/` 目录 + 每文件一条目；空目录以 `.gitkeep` 占位
- 工作台元数据：`ai-config-registry.json`，字段契约：

| 字段 | 含义 |
|------|------|
| `customPaths` | 用户添加的扫描目录 |
| `ignorePatterns` | 用户自定义忽略规则 |
| `trackedEntries` | 逐文件跟踪状态（路径 + 跟踪标志 + 类型） |
| `lastPushedFileHashes` | 逐文件内容哈希（推送成功才记录） |
| `trackedEmptyDirs` | 显式跟踪的空目录 |

该 JSON 是**跨版本契约**：加字段必须带默认值，不得改名或复用（`.ai/adr/0006`）。

## 调用规约

- 非阻塞：全部走 pooled 线程，不上 EDT
- 读取文件内容及 SHA 必须带配置分支 ref，与 PUT 分支一致；不能把 SHA 读取失败当成文件不存在
- JSON 错误使用结构化解析，完整解码换行/引号并清除凭据；422 的版本缺失和普通校验失败分别提示
- 路径含非 ASCII 时按路径段 URL 编码
- 推送最小化：组合哈希短路 + 逐文件 MD5 增量（`.ai/adr/0003`）
- 删除显式化：manifest 差分驱动 DELETE 调用

## 错误响应处理

GitHub 报错格式不定（JSON / HTML 限流页），`formatApiError` 统一解析并按状态码
附加建议（401 查 token、404 查仓库等）。规则明细见 `domain/sync/github.md`。
