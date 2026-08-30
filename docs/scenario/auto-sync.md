# 场景：自动同步（Auto Sync）

> 本页基于源码粗读（2026-08-29，`AutoSyncScheduler`、`AIConfigAutoSyncScheduler`、
> `SyncStatusService`）整理，未逐行验证，细节以代码为准。

两条通道各有自己的防抖调度器，行为对称而参数不同：

| | 笔记通道 | AI 配置通道 |
|---|---|---|
| 调度器 | `AutoSyncScheduler` | `AIConfigAutoSyncScheduler` |
| 防抖延迟 | 3 秒 | 5 秒 |
| 触发源 | 笔记数据变化事件 | VFS 文件变化事件 |
| 失败表现 | 静默，仅记日志 | 静默，仅记日志 |
| 冲突时 | 暂停（`SyncStatusService.isAutoSyncPaused`） | 暂停（`autoPushPaused` 标志） |

## 共同行为模式

1. **防抖**：触发时若已有待执行任务，取消旧的重新计时；避免连续编辑期间频繁推送
2. **执行前置检查**（任一不满足则跳过）：
   - 同步功能已启用（`config.isEnabled()`）
   - 自动同步开关已开（笔记 `isAutoSync()`；AI 配置 `isAiConfigAutoSync()`）
   - 配置校验通过（`config.validate()`）
   - 未因冲突被暂停
3. **实际推送**在 pooled 线程上执行（不阻塞 EDT），静默失败
4. 笔记通道调度时先通过 `SyncStatusService` 标记 PENDING 状态（供状态栏指示）

## AI 配置通道的独有门槛

`AIConfigAutoSyncScheduler` 额外要求存在同步历史
（`getLastSyncedRemoteMetadataHash()` 非空）：**从未手动推送/拉取过的环境不做自动
首推**，避免首次就把本地状态单方面铺到远端。首次同步必须用户手动执行。

## 冲突暂停与恢复

- 远端检测到更新/冲突时置暂停标志，自动推送跳过
- 冲突经用户解决后恢复（`resumeAutoSync()`），期间的手动 Push/Pull 不受影响
