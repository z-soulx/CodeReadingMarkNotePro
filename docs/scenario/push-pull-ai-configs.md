# 场景：AI 配置推送与拉取

AI 工作台页签上 Push / Pull 按钮触发的端到端流程。

## 推送（Push）

```
用户 -> AI 工作台 Push 按钮
 -> 组合哈希检查（无变化则直接出提示，附强制推送入口）
 -> AIConfigSyncAdapter.pushAIConfigs(config, id, forceAll)
 -> 逐文件 MD5 比对（或 force 全量）
 -> GitHubSyncProvider.pushFiles()
 -> PushReportDialog（汇总 + 已推/跳过/失败/已删/空目录 + Force Push 按钮）
 -> 更新 lastPushedFileHashes（失败文件不记，自动等下次重试）
```

要点：

- **远端删除**是推送的一部分：新旧 manifest 差分出不再跟踪的文件，调 DELETE
- **空目录**以 `.gitkeep` 占位上传（见 `.ai/adr/0004`）
- **元数据**随文件一起推：`ai-config-registry.json`（customPaths、ignorePatterns、
  trackedEntries、lastPushedFileHashes、trackedEmptyDirs）
- 报告对话框里的统计数字就是用户决定是否 Force Push 的依据

## 拉取（Pull）

```
用户 -> AI 工作台 Pull 按钮
 -> AIConfigSyncAdapter.pullAIConfigs(config, id)
 -> 下载远端文件（覆盖/新增本地 AI 配置文件）
 -> 应用元数据，两阶段（见 .ai/adr/0005）：
     1. 预扫描：customPaths + ignorePatterns（影响文件发现）
     2. 重扫描后：trackedEntries + fileHashes + trackedEmptyDirs
 -> 刷新树
```

## 冲突时的分岔

若本地与远端都有未同步变更（三方比对基线 = 上次同步哈希），
`AIConfigMergeAnalyzer` 产出逐文件归类与动作建议，
`AIConfigSyncConflictDialog` 让用户逐项选择采用哪边。
冲突解决前自动推送保持暂停。

## 状态角标

树上每个文件显示 NEW（本地新跟踪未推送）/ MODIFIED（内容变了）/ SYNCED
（哈希与上次推送一致），角标由存储的推送哈希计算。
