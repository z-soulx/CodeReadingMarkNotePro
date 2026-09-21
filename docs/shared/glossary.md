# 术语表（Glossary）

统一词汇，写文档/代码/提交时用同一套词。

## 笔记域

| 术语 | 英文/类名 | 定义 |
|------|----------|------|
| 主题 | Topic | 阅读主题，笔记的第一级容器（name + datetime） |
| 笔记行 | TopicLine | 对某文件某行的标注：`url + line + description` |
| 分组 | Group | 主题内按标签对行的二级组织，可自定义命名 |
| 回收站 | TrashedLine | 删除笔记的暂存包装，可恢复 |
| 主题列表 | TopicList | 全部主题的聚合根，挂在 `CodeReadingNoteService` 下 |

## 同步域

| 术语 | 英文/类名 | 定义 |
|------|----------|------|
| 通道 | channel | 两条独立同步链路：笔记通道 / AI 配置通道 |
| 提供者 | SyncProvider | 同步后端策略接口；目前唯一实现 GitHub |
| 增量推送 | incremental push | 逐文件 MD5 比对，只上传变化文件 |
| 组合哈希 | combined hash | 跟踪路径+内容+空目录的总哈希，用于无变化短路 |
| 上次推送哈希 | lastPushedFileHashes | 逐文件内容哈希表；失败文件不记入以保重试 |
| 强制推送 | force push | 绕过 MD5 全量重传，用户在推送报告里手动触发 |
| 冲突 | conflict | 本地与远端各有未同步变更 |
| 基线 | base / last-synced | 上次成功同步的哈希快照，三方合并的公共祖先 |
| 防抖 | debounce | 连续触发时取消旧任务重新计时（3s / 5s） |
| 推送报告 | FilePushReport / PushReportDialog | 推送结果的结构化数据与展示 |
| 项目标识 | projectIdentifier | 项目名清洗 Windows 非法字符后，远端目录名 |

## AI 配置域

| 术语 | 英文/类名 | 定义 |
|------|----------|------|
| AI 配置条目 | AIConfigEntry | 被跟踪的 AI 配置文件（路径 + 内容哈希） |
| 注册表 | AIConfigRegistry | 扫描发现的文件/目录 + 跟踪状态 + 忽略规则 |
| 跟踪 | tracked | 用户勾选纳入同步的文件/空目录 |
| 发现目录 | discoveredDirs | 扫描出的全部目录（含空目录） |
| 忽略规则 | ignorePatterns | 内置 + 用户自定义两层；支持精确名 / `*.ext` / `前缀*` / `dir/` |
| 空目录占位 | `.gitkeep` | 跟踪的空目录在远端的表示形式 |
| 工作台元数据 | ai-config-registry.json | 远端跨平台 JSON：路径/规则/跟踪态/哈希/空目录 |
| 同步角标 | NEW / MODIFIED / SYNCED | 树上文件相对上次推送的状态 |
| 三方合并 | AIConfigMergeAnalyzer | 本地/远端/基线逐文件比对归类 |
| 自定义命令 | AIWorkspaceCommand | `.ai/workspace-commands.json` 中的可执行命令；Terminal 或静默终端。文件缺失时写入一次 Cursor/Typora 默认项，可删且不回填 |
| 静默终端 | Silent Terminal | 同一 IDEA Terminal 会话，不抢焦点，立即启动 shell |
| 工作区版本 | `.ai/VERSION` | `.ai` 知识库的严格 Semver，默认 `1.0.0` |

## 工程术语

| 术语 | 定义 |
|------|------|
| 变更单元 | `.ai/specs/<yyyymm>-<slug>/`，一次有生命周期的待做变更 |
| 冻结 | 变更完成后的终态：不再编辑内容，仅可更新状态行 |
| 回流 | 变更完成后把新现状写回 `docs/` 的义务 |
| 执行证据 | `.ai/runs/` 的纯记录：命令、输出、摘要，不反思 |
