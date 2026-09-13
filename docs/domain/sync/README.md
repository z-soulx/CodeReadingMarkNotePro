# 同步域（Sync）

把本地状态推到远端 GitHub 仓库实现跨设备同步与云端备份。笔记存放在独立仓库，
不污染项目代码。

## 两条独立通道

| 通道 | 载荷 | 入口 | 自动调度 |
|------|------|------|---------|
| 笔记同步 | 各项目完整笔记 XML（含回收站） | `WorkspaceNotesSyncCoordinator`；`SyncService` 为根门面 | 应用级每项目队列，3 秒防抖及周期检查 |
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

- **笔记通道（3.7.7）**：`NotesSyncDecision` 比较上次同步内容基线、本地和远端的完整 XML 摘要。本地单变可推送、远端单变可拉取、双方不同变化暂停该项目。同名主题不做自动按名合并，设备时间不决定覆盖。远端目录由每项目显式绑定，旧项目名是首次绑定候选。
- **AI 配置通道**：`AIConfigMergeAnalyzer` 做三方比对（本地 / 远端 / 基线=上次同步哈希），
  逐文件归类（如 `NEW_REMOTE` 远端新增、`DELETED_REMOTE` 远端已删等）并给出动作建议，
  交给 `AIConfigSyncConflictDialog` 由用户逐项决定。纯逻辑类，不依赖 IDE UI

> AI 配置段落仍基于既有源码粗读；笔记通道已按 3.7.7 实现回流。

冲突期间自动推送会被暂停（pause 标志），避免覆盖未决差异。

## 工作空间笔记同步（3.7.7）

`sync/workspace/WorkspaceNotesSyncCoordinator` 在应用级串行调度所有可见项目，同一根目录在父窗口和独立子窗口间复用模型、绑定、基线及任务。手动入口冻结选中项目，批量面板逐项执行，一项失败不阻断其他项目；项目关闭、目录消失、配置或绑定代次变化会使旧任务失效。各项目数据保持独立，不混入父项目载荷。

`NotesSyncBinding` 使用每项目 `.idea/notesSyncBinding.xml` 保存仓库、分支、目录标识、自动策略及检查间隔；`NotesSyncState` 将内容基线、远端 SHA、暂停状态及未完成拉取日志放在 IDEA 配置目录 `CodeReadingNote/notes-sync/`，按本地根与完整远端身份分区。Token 仍只取应用级配置。

`WorkspaceNotesCoordinator` 在 EDT 获取不可变模型快照和 revision，子项目使用共享串行存储，根项目调用平台 `StoreUtil.saveSettings`；真实磁盘 XML 验证成功后才允许记录同步基线。拉取重新检查模型 revision、磁盘内容和项目可用性，完整解析后才应用；替换前备份至 `notes-sync/backups/`。上传期间新编辑仍是待同步数据。

完整载荷包含回收站（空回收站显式写出）、排序和未知扩展字段。旧载荷缺失 trash 时保留本地回收站，保持内容基线差异；合法空集合可同步，损坏或缺失文件不能视为空。现有 `SyncConflictDetector` 等旧接口不再参与新入口的自动覆盖判断。

## 推送报告 UI

每次推送后 `PushReportDialog` 展示结构化结果：汇总头（状态图标 + 统计）、可折叠分组
（已推送 / 跳过未变 / 失败含原因 / 已删除 / 空目录）、「无变化」场景给提示 + 强推入口。

## 场景走查

`scenario/push-pull-ai-configs.md`（AI 配置推/拉全流程）、`scenario/auto-sync.md`（自动同步）。
