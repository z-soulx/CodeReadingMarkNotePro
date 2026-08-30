# 全局架构

## 系统是什么

IntelliJ IDEA 插件（`soulx.CodeReadingMarkNotePro`）：在阅读源码时给代码行做笔记与书签。
Pro 版在开源版基础上增加了分组管理、GitHub 跨设备同步、多语言界面（运行时切换、无需重启）、
AI 配置工作台。技术栈：Java 17 + Gradle + IntelliJ Platform 2024.3+。

## 分层拓扑

```
┌─ 用户入口 ────────────────────────────────────────────────┐
│ Actions（TopicLineAddAction、NavigateToNoteAction 等）      │
│ ToolWindow UI（ManagementPanel：树 / 搜索 / AI 工作台 三页签）│
│ Gutter（NoteGutterIconRenderer + NotePopupHelper 行内编辑） │
├─ 服务层 ──────────────────────────────────────────────────┤
│ CodeReadingNoteService   笔记状态 + 持久化                  │
│ AIConfigService          AI 配置发现/跟踪 + 持久化           │
│ AIWorkspaceService       `.ai` 文档根 / Git / VERSION / 自定义命令 │
│ SyncService / AIConfigSyncAdapter   两条独立同步通道          │
│ AutoSyncScheduler / AIConfigAutoSyncScheduler   自动推送     │
├─ 领域层 ──────────────────────────────────────────────────┤
│ TopicList - Topic - TopicLine - Group；TrashedLine（回收站） │
│ AIConfigRegistry - AIConfigEntry；AIConfigMergeAnalyzer     │
├─ 同步 Provider（策略模式）────────────────────────────────┤
│ SyncProvider -> AbstractSyncProvider -> GitHubSyncProvider │
└──────────────────────────────────────────────────────────┘
```

同步后端为什么做成策略模式、两条通道为什么独立，见 `.ai/adr/0001`、`.ai/adr/0002`。

## 事件通信

组件间解耦靠 IntelliJ `MessageBus`（而非直接互相持有引用）：

| Notifier | 触发内容 | 典型订阅方 |
|----------|---------|-----------|
| `TopicListNotifier` | 主题增删、回收站变化 | UI 树刷新 |
| `TopicNotifier` | 单主题内行增删、`lineNoteChanged` | 详情面板、gutter 图标刷新 |
| `AIConfigNotifier` | AI 配置注册表/文件变化 | AI 工作台树、自动同步调度器 |
| `SyncStatusNotifier` | 同步状态流转（PENDING/成功/失败） | 状态栏指示 |

## 持久化格局

| 文件 | 内容 | 作用域 |
|------|------|-------|
| `CodeReadingNote.xml` | 主题、行、分组 | 项目级，随笔记同步到远端 |
| `aiConfigRegistry.xml` | 跟踪状态、自定义路径、忽略规则、推送哈希、跟踪的空目录 | 项目级 |
| `syncStatus.xml` | 同步时间戳、MD5 缓存 | 项目级，**仅本地** |
| `codeReadingNoteSync.xml` | Token、仓库 URL | 应用级（跨项目共享） |
| `ai-config-registry.json` | 远端工作台元数据 | 远端，跨平台 JSON 契约 |
| `aiWorkspace.xml` | 运行时文档根（默认 `.ai/docs`） | 项目级，仅本地 |
| `.ai/VERSION` | 工作区 Semver | `.ai` 工作区 |
| `.ai/workspace-commands.json` | 自定义命令目录 | `.ai` 工作区 |

本地 XML 与远端 JSON 双格式并存的理由见 `.ai/adr/0006`。

## 平台依赖

`PersistentStateComponent`（状态持久化）、`MessageBus`（事件）、`VFS listener`（AI 配置
文件实时感知）、`ToolWindow`/`Editor` API。详细行为与坑见 `integration/intellij-platform.md`。
