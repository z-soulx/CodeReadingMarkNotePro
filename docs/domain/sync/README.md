# 同步域（Sync）

把本地状态推到远端 GitHub 仓库实现跨设备同步与云端备份。笔记存放在独立仓库，
不污染项目代码。

## 两条独立通道

| 通道 | 载荷 | 入口 | 自动调度 |
|------|------|------|---------|
| 笔记同步 | `CodeReadingNote.xml` 单文件 | `SyncService.push()/pull()` | `AutoSyncScheduler`（3 秒防抖） |
| AI 配置同步 | `ai-configs/` 目录 + 元数据 | `AIConfigSyncAdapter.pushFiles()/pullFiles()` | `AIConfigAutoSyncScheduler`（5 秒防抖） |

通道独立的取舍见 `.ai/adr/0002`；Provider 层的策略模式见 `.ai/adr/0001`。
GitHub 特有的行为（错误解析、路径编码、状态码建议）单独在 `github.md`。

## 增量推送语义（MD5）

推送不是全量重传，语义按 `.ai/adr/0003`：

1. **组合哈希短路**：跟踪路径 + 内容 + 空目录算一个总哈希，没变化直接跳过整次推送
2. **逐文件 MD5**：与 `lastPushedFileHashes` 比对，只有变化的文件真正上传
3. **失败不记哈希**：上传失败的文件不写入哈希表，下次推送自动重试 -- 这是不丢数据的关键不变量
4. **远端删除**：新旧 manifest 差分出不再跟踪的文件，逐个调 DELETE API
5. **强制推送**：绕过 MD5 全量重传，入口在 `PushReportDialog` 的 Force Push 按钮，由用户基于统计自行决定

## 拉取与元数据应用

AI 配置拉取时，`ai-config-registry.json` 元数据分两阶段应用（`.ai/adr/0005`）：

- **预扫描**：`customPaths`、`ignorePatterns`（它们决定能发现哪些文件）
- **重扫描**后：`trackedEntries`、`fileHashes`、`trackedEmptyDirs`（套用到新扫描出的条目上）

## 冲突检测与合并

- **笔记通道**：`SyncConflictDetector` 检查远端是否比本地新。本地时间取「上次同步时间
  与最新 Topic 更新时间的较大者」（`getEffectiveLocalTimestamp`），与 Provider 返回的
  远端最后修改时间比较，输出 `ConflictDetectionResult`。远端项目标识由项目名替换
  Windows 非法字符（`\ / : * ? " < > |` -> `_`）生成
- **AI 配置通道**：`AIConfigMergeAnalyzer` 做三方比对（本地 / 远端 / 基线=上次同步哈希），
  逐文件归类（如 `NEW_REMOTE` 远端新增、`DELETED_REMOTE` 远端已删等）并给出动作建议，
  交给 `AIConfigSyncConflictDialog` 由用户逐项决定。纯逻辑类，不依赖 IDE UI

> 以上两段基于类源码粗读（2026-08-29），未逐行验证，细节以代码为准。

冲突期间自动推送会被暂停（pause 标志），避免覆盖未决差异。

## 推送报告 UI

每次推送后 `PushReportDialog` 展示结构化结果：汇总头（状态图标 + 统计）、可折叠分组
（已推送 / 跳过未变 / 失败含原因 / 已删除 / 空目录）、「无变化」场景给提示 + 强推入口。

## 场景走查

`scenario/push-pull-ai-configs.md`（AI 配置推/拉全流程）、`scenario/auto-sync.md`（自动同步）。
