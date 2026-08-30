# AI 配置工作台（AI Config Workspace）

发现、跟踪并同步散落在项目里的 AI 工具配置文件（Cursor 规则、CLAUDE.md、
Windsurf、Codex、Copilot 等），把它们变成可跨设备同步的工作台资产。

## 组件结构

```
AIConfigService（项目级，PersistentStateComponent）
├── AIConfigRegistry
│   ├── scan()                扫描已知路径 + 自定义路径，发现文件与空目录
│   ├── AIConfigEntry[]       被跟踪的文件（含内容哈希）
│   ├── discoveredDirs        所有目录（含空目录）
│   ├── userIgnorePatterns    用户自定义忽略规则（持久化）
│   └── collectTrackedFilesContent()   供同步使用
├── AIConfigSyncAdapter       桥接到 SyncProvider.pushFiles/pullFiles
│   ├── findEmptyTrackedDirs()          读持久化的空目录跟踪状态
│   ├── pushWorkspaceMetadata()         序列化并推送 ai-config-registry.json
│   └── pullAndApplyMetadata()          两阶段应用（预扫描/后扫描）
├── PersistentState           customPaths、ignorePatterns、trackedEntries、
│                             lastPushedHash、lastPushedFileHashes、trackedEmptyDirs
└── VFS listener              文件变化的实时感知
```

## 配置类型

`AIConfigType` 枚举识别七类：**Cursor Rules、Claude、AI Docs、Windsurf、Codex、
Copilot、Custom**。`AIConfigTypeRegistry` 负责各类型对应的发现路径约定。
新增一个 AI 工具 = 加一个枚举项 + 其路径约定。

## 忽略系统

两层规则：

- **内置**：`.DS_Store`、`*.swp` 等常见噪音
- **用户自定义**：持久化在 `aiConfigRegistry.xml`，随元数据同步到远端

支持的 pattern 形态：精确文件名、`*.ext`、`前缀*`、`dir/` 目录路径。

## 空目录支持

Git 风格远端存不了空目录，插件的处理（`.ai/adr/0004`）：扫描时 `discoveredDirs`
记录全部目录（含空）；用户在树上**显式勾选**才算跟踪；跟踪的空目录持久化在
`trackedEmptyDirs` 并以 `.gitkeep` 占位文件形式同步到远端。空目录跟踪是显式
选择，不靠 `.gitkeep` 存在与否反推。

## 树 UI

`AIConfigTreePanel` 把「文件路径 + discoveredDirs」构建成 `VirtualDir` 层级树，
每个文件带同步状态角标：**NEW / MODIFIED / SYNCED**（对照存储的推送哈希计算）。
展开/折叠按钮自适应：`hasAnyExpanded()` 探测树的实际状态决定显示哪个。

## 冲突合并

远端与本地不一致时，`AIConfigMergeAnalyzer` 做三方比对（见 `domain/sync/README.md`
冲突一节），`AIConfigSyncConflictDialog` 让用户逐文件决定。合并分类还包括目录条目
（路径以 `/` 结尾的跳过，另行处理）。

## 场景走查

`scenario/push-pull-ai-configs.md`、`scenario/auto-sync.md`。
同步元数据的字段清单见 `integration/github-api.md` 的 manifest 一节。
# AI Workspace 运行时工作区

3.7.4 起，AI Workspace 的文档根目录固定默认在项目内 `.ai/docs`；如需调整由用户手工移动，不自动移动项目根 `docs/`。`.ai` Git 仓库必须由用户显式初始化，`.ai/.gitignore` 排除 `token.txt`、`runs/`、临时文件和嵌套 `.git/` 元数据。`.ai/VERSION` 缺失时创建 `1.0.0`，补丁升级使用严格 Semver。既有 `aiConfigRegistry.xml` 与远端 `ai-config-registry.json` 协议保持不变。

3.7.5 起，只要项目里存在 `.ai/` 目录，就会向项目根 `.gitignore` 追加 `/.ai/`（若尚无该规则），并把父仓库里已经跟踪的 `.ai` 路径从索引中移除（`git rm --cached`，不删磁盘、不自动提交）。父仓库 Commit 里可能先出现一批 `.ai` 的删除，提交那一次之后，项目提交不再带上 `.ai` 文件。工具栏只有一个 Git 按钮：未初始化显示「初始化 .ai Git」（init 后立刻打开 Commit），之后显示「打开 .ai Git」。第一次成功后 Directory Mapping 会写入 `.idea`，项目打开时自动补登记，因此之后即使用 IDEA 自己的 Commit 工具窗口也会一直看到 `.ai` 仓库。`.ai` 变更在独立 changelist `.ai`，不进入 Default。`.ai/VERSION` 可手工编辑。IDEA 每个项目仍只有一个 Commit 窗口。

自定义命令保存在 `.ai/workspace-commands.json`（`schemaVersion: 1`）。只要存在 `.ai/` 且该文件还不存在，插件会写入两条默认可删命令：用 Cursor 打开当前项目（`launch-cursor-project`）、用 Typora 打开选中的 Markdown（`open-selected-md-in-typora`）。文件一旦存在（包括 `commands: []`）就不再注入。管理窗口提供统一表单，可选择 IDEA Terminal 或静默终端；**保存命令**立即写入，没有底部确定/取消，也不再弹出确认（窗口为非模态，执行不必先关闭）。两种模式都通过 `TerminalToolWindowManager` 把命令发给 IDEA Terminal 的 shell；发送前会解析 `cursor`/`typora` 的真实路径（Windows 含 Program Files），带空格的 `.exe` 在 PowerShell 中使用 `&` 调用。静默模式不聚焦 Terminal 工具窗口，但会立即启动会话。参数支持 `$FilePath$`、`$FileDir$`、`$FileName$`。每条命令可选择文件来源：项目工具窗口选中（默认，文件不必打开；无选中时回退到编辑器）或编辑器当前标签页。
