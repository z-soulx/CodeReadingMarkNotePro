# 集成边界：IntelliJ 平台

插件对 IntelliJ Platform 的关键依赖与已知行为。平台行为变化（升级 2024.3+）
时优先复核本页。

## PersistentStateComponent

`CodeReadingNoteService`、`AIConfigService`、`SyncStatusService`、`SyncSettings` 的
持久化通道，XML 落盘位置与作用域（项目级/应用级）见 `architecture/overview.md`
的持久化表。注意：状态由 IDE 异步保存，不能假设 write 之后立即在磁盘上。

## MessageBus

事件解耦的通道（notifier 清单见 `architecture/overview.md`）。订阅方必须在
dispose 时反注册，避免泄漏（constitution 红线）。

## VFS Listener

`AIConfigService` 注册 VFS 监听实时感知 AI 配置文件变化，驱动
`AIConfigAutoSyncScheduler` 的防抖推送。

## 项目打开

`WorkspaceNotesStartupActivity` 启动工作空间笔记发现；WorkspaceNotesService 注册递归 VFS
watch root，监听目录/文件变化并防抖扫描。订阅和 watch 随服务关闭释放。应用级协调者复用
已打开 Project 的笔记服务，子目录不会创建额外 Project；关闭窗口处理快照和待保存队列。

`SyncStartupActivity` 实现 `ProjectActivity`（不再使用已过时的 `StartupActivity`），
注册在 `com.intellij.postStartupActivity`。3.7.7 将窗口注册到应用级 `WorkspaceNotesSyncCoordinator`，该服务根据各项目绑定和自动策略后台检查。根项目同步使用 `StoreUtil.saveSettings` 后验证磁盘；子项目同步使用共享串行 XML 存储，两者都以实际保存结果为准。

`AIWorkspaceVcsStartupActivity` 同样是 `ProjectActivity`：只要存在 `.ai/` 目录，就确保项目根
`.gitignore` 含 `/.ai/`，并把父仓库索引中的 `.ai` 路径 `git rm --cached`（不自动提交）。
若 `.ai/.git` 已存在，再用 `ProjectLevelVcsManager.setDirectoryMappings` 登记 `.ai` → Git，
并把 `.ai` 变更移到独立 changelist。打开 Commit UI 走平台 action `CheckinFiles`，不编译依赖 Git4Idea。

## ToolWindow / Editor / Gutter

- `ManagementPanel` 三页签（树 / 搜索 / AI 工作台）
- gutter 标记渲染 `NoteGutterIconRenderer`，交互弹窗 `NotePopupHelper`
- UI 文案一律走 `CodeReadingNoteBundle`，运行时切换语言无需重启

## Bookmark 行为调研（实测笔记）

> 迁自 `src/main/java/ext/ext.md`（2026-03 前后的调研），解释
> 「实验功能 mark 位置修复」为何建议搭配取消 Restore workspace。

- 猜测：IDE 每个**分支一份 bookmark 文件**，切换分支时自动切到对应分支的文件
- 看起来「跨分支保留」的原因：不勾选 restore workspace 时，UI 缓存里还留着上一
  分支的标签所以仍展示；关闭时会写进**当前分支**的 bookmark 文件
- 存在「已删除但 UI 缓存未更新」的现象
- 结论：搭配取消勾选 Restore workspace 效果最佳（也写进了根 README 的使用提示）
