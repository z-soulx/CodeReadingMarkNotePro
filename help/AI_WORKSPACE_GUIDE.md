# AI Workspace Guide / AI 工作空间使用指南

> Code Reading Mark Note Pro v3.7.6+

## Workspace Git and Version

The runtime docs root is `.ai/docs`; if you later want the notes tree elsewhere, move it manually. The existing project `docs/` directory is never moved automatically. When `.ai/` exists, the plugin writes `/.ai/` into the project `.gitignore` and untracks `.ai` from the parent Git index (files stay on disk). Commit that parent deletion once; later **project** commits will not include `.ai`. The toolbar Git button is **Initialize .ai Git** until `.ai/.git` exists (it inits and then opens Commit). After that it becomes **Open .ai Git**. The first successful init registers an IDEA Directory Mapping, so the project Commit window keeps the `.ai` root even if you only use IDEA's own Commit icon later. Nested `.ai` changes go to a dedicated `.ai` changelist. Edit `.ai/VERSION` yourself if you want a workspace Semver file.

只要存在 `.ai/`，插件就会向项目根 `.gitignore` 写入 `/.ai/`，并从父仓库索引取消跟踪 `.ai`（磁盘文件保留）。请先在项目 Commit 里提交那一次删除；之后**项目提交**不再包含 `.ai`。工具栏 Git 按钮在尚未有 `.ai/.git` 时显示 **初始化 .ai Git**（初始化后立刻打开 Commit）；之后变成 **打开 .ai Git**。第一次成功后会写入 IDEA Directory Mapping，所以以后即使用 IDEA 自己的 Commit 图标，窗口里也会一直有 `.ai` 这个仓库。嵌套仓库的 `.ai` 变更在独立 changelist `.ai` 里。需要工作区 Semver 时请直接编辑 `.ai/VERSION`。

### Custom Commands / 自定义命令

Use **Manage Custom Commands** (the only command entry point in the toolbar) to see the saved command list. The first time `.ai/` exists and `.ai/workspace-commands.json` is missing, the plugin writes two ordinary commands: **Launch Cursor for this project** and **Open selected Markdown in Typora**. You can edit or delete them; an existing JSON file (including an empty `commands` list) is never re-seeded. **Add** and **Edit** both open the same editor form with fields for id, display name, executable, arguments, project-relative working directory, enabled state, execution mode, and where `$FilePath$` comes from. Click **Save Command** to write immediately (no OK/Cancel footer and no extra confirmation). Close the window with the title-bar close button or Esc. Select a command and click **Run**: both modes send the command to IDEA Terminal (PowerShell on Windows). **IDEA Terminal** focuses the tab; **Silent Terminal** starts the same shell without stealing focus. Add, Save, Delete, and Run remain on one fully visible action row across supported desktop platforms and display scaling. Shell built-ins such as `cd ai3` work in either mode. Each command is stored in `.ai/workspace-commands.json`, which is included when you commit the `.ai` repository and can therefore be synchronized with the workspace Git repository.

When the executable is `wt` or `wt.exe` on Windows, the configured working directory is passed explicitly with Windows Terminal's `-d` option so the new tab opens in that directory.

**Execution mode is a real launch switch, not a comment.** **IDEA Terminal** types the command into a Terminal tab and focuses it (PowerShell on Windows, the default shell on macOS). **Silent Terminal (no focus)** uses the same IDEA Terminal session but does not steal focus — the shell still starts immediately so `cursor` / `typora` actually run. Both modes work on Windows and macOS because the Terminal shell resolves PATH. Shell built-ins such as `cd ai3` also work in Silent Terminal mode.

Fill **Executable** with a portable program name (`cursor`, `typora`) when you want one `.ai/workspace-commands.json` to work on both operating systems. Absolute `.exe` paths remain valid on Windows.

#### Command form fields / 命令表单字段

| Field / 字段 | Purpose / 作用 | Example / 示例 |
|---|---|---|
| **ID** | Stable unique identifier used to find the command. Do not reuse an existing ID. / 用于查找命令的稳定唯一标识，不要与已有 ID 重复。 | `enter-ai-dir` |
| **Display name** | Friendly name shown in the manager; it does not affect execution. / 管理窗口中显示的名称，不影响执行。 | `Enter AI directory` |
| **Executable** | Program resolved through PATH, an absolute executable path, or a simple Terminal command. Do not quote the whole command. Use portable names (`cursor`, `typora`) for Windows + macOS Silent commands. / PATH 中的程序名、绝对路径或简单 Terminal 命令，不要把整条命令加引号。跨 Windows / macOS 的静默命令请填写便携程序名（`cursor`、`typora`）。 | `cursor` |
| **Arguments** | Space-separated arguments appended after the executable. / 追加在可执行文件后的空格分隔参数。 | `buildPlugin` |
| **Working directory** | Optional project-relative directory where the Terminal session starts. / 可选的项目内相对工作目录。 | `.ai` |
| **Execution mode** | **IDEA Terminal** focuses a Terminal tab. **Silent Terminal (no focus)** uses the same shell without stealing focus. Both actually run the command. / **IDEA Terminal** 会聚焦终端页。**静默终端（不抢焦点）**使用同一个 shell，但不抢焦点。两种方式都会真正执行命令。 | Silent Terminal (no focus) |
| **File for $FilePath$** | **Project tree selection** uses the highlighted file in the Project tool window (the file need not be open); if nothing is selected there, the open editor file is used. **Open editor file** uses only the current editor tab. / **项目树选中**使用项目工具窗口里高亮的文件（不必打开）；树里没有选中时回退到编辑器。**编辑器已打开的文件**只用当前编辑器标签页。 | Project tree selection |
| **Enabled** | Disabled commands remain saved but cannot be run. / 禁用后命令仍保存，但不能执行。 | checked / 勾选 |

Supported file macros / 支持的文件变量：`$FilePath$` is the absolute path of the chosen file, `$FileDir$` is its parent directory, and `$FileName$` is its file name. Each command chooses the source: Project tool window selection (default; falls back to the editor) or the open editor file. / `$FilePath$` 是所选文件的绝对路径，`$FileDir$` 是父目录，`$FileName$` 是文件名。每条命令可自选来源：项目工具窗口选中（默认，无选中则用编辑器）或编辑器已打开的文件。

**Example / 示例: build the plugin / 构建插件**

```text
ID: build-plugin
Display name: Build Plugin
Executable: gradlew.bat
Arguments: buildPlugin
Working directory: (empty / 留空，表示项目根目录)
Enabled: checked / 勾选
Execution mode: Silent Terminal (no focus)
```

**First-run built-in — launch Cursor for the current project / 首次内置——静默启动 Cursor 打开当前项目**

```text
ID: launch-cursor-project
Display name: Launch Cursor for this project
Executable: cursor
Arguments: .
Working directory: (empty / 留空，表示项目根目录)
Execution mode: Silent Terminal (no focus)
Enabled: checked / 勾选
```

Windows / macOS: the IDEA Terminal shell resolves `cursor` from PATH (`cursor.cmd` on Windows). / Windows 与 macOS：由 IDEA Terminal 的 shell 从 PATH 解析 `cursor`（Windows 上是 `cursor.cmd`）。

**First-run built-in — open the selected Markdown file in Typora / 首次内置——用 Typora 打开当前选中的 Markdown 文件**

```text
ID: open-selected-md-in-typora
Display name: Open selected Markdown in Typora
Executable: typora
Arguments: $FilePath$
Working directory: (empty / 留空)
Execution mode: Silent Terminal (no focus)
File for $FilePath$: Project tree selection (fallback: editor)
Enabled: checked / 勾选
```

Windows / macOS: the plugin resolves `typora` / `typora.exe` via PATH, then well-known install locations such as `C:\\Program Files\\Typora\\Typora.exe`. Highlight a Markdown file in the Project tool window (opening it is optional) so `$FilePath$` is that file; switch the command to **Open editor file** if you want the current tab instead. / Windows 与 macOS：插件会从 PATH 以及常见安装目录（如 `C:\\Program Files\\Typora\\Typora.exe`）解析 Typora。在项目工具窗口中高亮 Markdown 文件即可（不必打开）；若要用当前编辑器标签页，把该命令的文件来源改成「编辑器已打开的文件」。

Click **Save Command / 保存命令** to write immediately. Later select the command and click **Run / 执行**. Both modes send the command to IDEA Terminal; Silent does not focus the tab. Close with the window X or Esc. / 点击“保存命令”立即写入；以后选中命令点击“执行”。两种模式都发送到 IDEA Terminal；静默模式不聚焦该标签页。用窗口关闭按钮或 Esc 关闭。

“管理自定义命令”是工具栏中唯一的命令入口，可查看全部已保存命令。只要存在 `.ai/` 且还没有 `workspace-commands.json`，插件会写入两条普通命令（用 Cursor 打开当前项目、用 Typora 打开选中的 Markdown）；之后可自行编辑或删除，已有 JSON（包括空列表）不会再次注入。“添加”和“编辑”共用同一个编辑表单。点击“保存命令”立即写入，没有底部确定/取消，也不再弹出确认。选中命令点击“执行”时两种模式都发到 IDEA Terminal（Windows 使用 PowerShell）：普通模式会聚焦终端，**静默终端不抢焦点**但会立刻启动会话并执行命令。例如 `cd ai3`、`cursor .`、`typora` 都可以用静默终端。`$FilePath$` 默认取项目工具窗口里高亮的文件（不必打开），也可以改成只用编辑器当前标签页。跨系统请把可执行文件写成 `cursor` 或 `typora`。命令保存在 `.ai/workspace-commands.json`，可随 `.ai` 仓库同步。

在 Windows 上，如果可执行文件填写为 `wt` 或 `wt.exe`，插件会通过 Windows Terminal 的 `-d` 参数显式传递配置的工作目录，确保新标签页进入该目录。

## Overview / 概述

The **AI Workspace** brings personal AI config files (Cursor Rules, Claude rules, architecture docs, etc.) under unified management with a hierarchical file tree, sync status tracking, independent manual sync, an opt-in `.ai` Git repository, Semver versioning, and custom Terminal commands.

**AI 工作空间** 将个人 AI 配置文件（Cursor Rules、Claude 规则、AI 架构文档等）纳入统一管理，提供层级文件树、同步状态追踪、独立手动同步，以及可选的 `.ai` Git 仓库、Semver 版本和自定义 Terminal 命令。

---

## Entry Point / 功能入口

Third tab **"AI Workspace"** in the tool window.

工具窗口第三个标签页 **「AI 工作空间」**。

---

## Default Paths / 默认识别路径

| Type / 类型 | Path / 默认路径 | Description / 说明 |
|------|----------|------|
| **Cursor Rules** | `.cursor/rules/` | Cursor IDE AI rule files / Cursor IDE 的 AI 规则文件 |
| **Claude Rules** | `.claude/` | Claude AI project rules / Claude AI 的项目规则 |
| **AI Docs** | `.ai/` | General AI docs / 通用 AI 文档 |
| **Windsurf** | `.windsurf/` | Windsurf IDE config / Windsurf IDE 配置 |
| **Codex** | `.codex/` | OpenAI Codex config / OpenAI Codex 配置 |
| **GitHub Copilot** | `.github/copilot-instructions.md` | Copilot project instructions / Copilot 项目指令 |

Use **"Add Custom Path"** to track any directory or file.

可通过 **「添加自定义路径」** 追踪任意目录或文件。

---

## Toolbar / 工具栏

| Button / 按钮 | Function / 功能 |
|------|------|
| **Scan AI Configs / 扫描** | Re-scan project for AI config files / 重新扫描项目中的 AI 配置文件 |
| **Initialize / Open .ai Git / 初始化或打开 .ai Git** | Not inited: create `.ai/.git` and open Commit. Inited: open Commit. Mapping persists, so IDEA's own Commit window also keeps `.ai`. / 未初始化则创建 `.ai/.git` 并打开 Commit；已初始化则打开 Commit。Mapping 会保留，IDEA 自己的 Commit 窗口也会一直有 `.ai`。 |
| **Manage Custom Commands / 管理自定义命令** | Add, save, delete, and run project commands. First missing `workspace-commands.json` includes Cursor and Typora (deletable). / 添加、保存、删除并执行项目命令。第一次没有该 JSON 时带 Cursor、Typora（可删）。 |
| **Add Custom Path / 添加自定义路径** | Track non-default directories (auto-validates path and provides feedback) / 添加非默认目录进行追踪（自动验证路径有效性并反馈结果） |
| **Create AI Skeleton / 创建 AI 骨架** | Choose Workspace / Notes Space / All → create `.ai/` directories / 选择 Workspace / Notes Space / All → 创建 `.ai/` 目录 |
| **Ignore Patterns / 忽略规则** | Edit file/directory ignore rules (.gitignore-like, supports `*.ext`, `name`, `dir/`) / 编辑文件/目录忽略规则（类似 .gitignore，支持 `*.ext`、`name`、`dir/` 等模式） |
| **Open in Editor / 在编辑器中打开** | Open selected file in IDE editor / 在 IDE 编辑器中打开选中文件 |
| **Push AI Configs / 推送AI配置** | Manually push tracked files to remote / 手动推送已追踪文件到远程 |
| **Pull AI Configs / 拉取AI配置** | Manually pull from remote to local / 手动从远程拉取到本地 |

> **Help button (?)** is on the right side of the main toolbar (globally available, not limited to AI Workspace).
>
> **帮助按钮 (?)** 位于主工具栏右侧（全局可用，不限于 AI 工作空间）。

---

## File Tree / 文件树

### Hierarchical Display / 层级展示

Files are displayed in their real directory structure. **Empty folders are also shown** as long as they are within scan scope.

文件按真实目录结构嵌套显示，**空文件夹也会显示**（只要在扫描范围内）。

```
.ai/            (5)
  ARCHITECTURE.md  ✓
  WORKFLOW.md      ●
  docs/          (2)
    guide.md       ★
    api.md         ✓
  templates/     (0)          ← empty dir shown / 空目录也展示
.claude/        (1)
  CLAUDE.md        ✓
.codex/         (0)           ← empty dir shown / 空目录也展示
```

### Sync Status Icons / 同步状态标记

| Icon / 标记 | Color / 颜色 | Meaning / 含义 |
|------|------|------|
| ★ | Green / 绿色 | **New** — never pushed / **新文件** — 从未推送 |
| ● | Orange / 橙色 | **Modified** — changed since last push / **已修改** — 自上次推送后有变化 |
| ✓ | Gray / 灰色 | **Synced** — matches last push / **已同步** — 与上次推送一致 |

### Checkboxes / 复选框

- **Check a file**: Include in sync tracking / **勾选文件**：纳入同步追踪
- **Check a directory**: Batch toggle all files in that directory / **勾选目录**：批量切换该目录下所有文件
- **Uncheck a single file**: Does not affect sibling files / **取消单个文件**：不影响同目录其他文件

---

## Sync / 同步

### Design Principle / 设计原则

Notes sync and AI config sync are **completely independent**:

笔记同步与 AI 配置同步**完全独立**：

- **Notes sync** (main toolbar ⬆⬇): Frequent changes, supports auto-sync / **笔记同步**（主工具栏 ⬆⬇）：变动频繁，支持自动同步
- **AI config sync** (AI Workspace ⬆⬇): Supports both manual and auto-sync / **AI 配置同步**（AI 工作空间内 ⬆⬇）：支持手动和自动同步

### Push / 推送

1. Check the files you want to sync in the file tree / 在文件树中勾选需要同步的文件
2. Click **"Push AI Configs"** / 点击 **「推送AI配置」**
3. Auto-detects changes — skips if nothing changed / 系统自动检测变更 — 无变化则跳过
4. Status icons update after push / 推送完成后状态标记自动更新

### Pull / 拉取

1. Click **"Pull AI Configs"** / 点击 **「拉取AI配置」**
2. Confirmation dialog warns "pull will overwrite local files" / 确认对话框提醒"拉取将覆盖本地文件"
3. Remote files are written to local directories, tree auto-refreshes / 远程文件写入本地对应目录，文件树自动刷新

### Auto-Sync / 自动同步

> v3.7.2+

#### How to Enable / 如何开启

Settings → Tools → Code Reading Note Sync → check **"AI Config Auto Sync"**.

设置 → Tools → Code Reading Note Sync → 勾选 **「AI配置自动同步」**。

#### How It Works / 工作原理

```
 Local file change (VFS event)
        │
        ▼
 Is the file a tracked AI config entry?
        │ No → ignore
        ▼ Yes
 AIConfigAutoSyncScheduler.scheduleAutoSync()
        │
        ▼
 Debounce 5 seconds (resets timer on each new change)
        │
        ▼
 Pre-flight checks:
   ├─ Sync enabled? + AI Config Auto Sync enabled?
   ├─ Config valid? (repo/token/branch)
   └─ Has prior sync history? (lastSyncedRemoteMetadataHash ≠ "")
        │ Any check fails → skip, log reason
        ▼ All pass
 Remote conflict check:
   Pull remote ai-config-registry.json →
   Compare MD5(remote) with lastSyncedRemoteMetadataHash
        │
        ├─ Hash mismatch → CONFLICT
        │     Pause auto-sync.
        │     Show modal conflict dialog with 3 actions:
        │       [Pull from Remote] — overwrite local with remote
        │       [Force Push]       — overwrite remote with local
        │       [Cancel]           — keep paused, resolve manually later
        │
        └─ Hash matches → SAFE
              Execute pushAIConfigs() (same as manual push)
              Update lastSyncedRemoteMetadataHash on success.
```

自动同步流程：

1. 本地 AI 配置文件发生变更（VFS 事件监听）
2. 判断该文件是否为已追踪(tracked)的 AI 配置 → 否则忽略
3. 进入 5 秒防抖调度（每次新变更重置计时器，避免频繁推送）
4. 飞行前检查：同步已启用？AI 配置自动同步已开启？配置合法？有过手动同步记录？
5. 远端冲突检测：拉取远端 `ai-config-registry.json`，比较 MD5 哈希
   - **哈希不匹配** → 检测到冲突，暂停自动推送，弹出冲突对话框（Pull / Force Push / Cancel）
   - **哈希匹配** → 安全，执行推送

#### Important Notes / 注意事项

- **First sync must be manual**: Auto-sync requires at least one manual push or pull before it activates. This is because the conflict detection baseline (`lastSyncedRemoteMetadataHash`) is only set during a full manual sync.
- **首次同步必须手动**：自动同步需要至少一次手动推送或拉取后才会激活，因为冲突检测基线 (`lastSyncedRemoteMetadataHash`) 仅在完整手动同步时设置。
- **Auto-sync is push-only**: It only pushes local changes to remote. It does NOT auto-pull. To get remote changes, always use manual pull.
- **自动同步仅推送**：只将本地变更推送到远端，不会自动拉取。获取远端变更始终需要手动拉取。
- **Conflict = dialog, not overwrite**: If another device pushed changes, auto-sync pauses and shows a conflict dialog (Pull / Force Push / Cancel), same pattern as the topic sync conflict dialog. If you cancel, auto-sync stays paused until you manually resolve from the AI Workspace tab.
- **冲突 = 对话框而非覆盖**：如果其他设备推送了变更，自动同步会暂停并弹出冲突对话框（拉取 / 强制推送 / 取消），与笔记同步冲突对话框相同模式。如果取消，自动同步保持暂停直到你从 AI 工作空间标签页手动解决。

---

### Untrack → Push = Remote Delete / 取消追踪 → 推送 = 远端删除

Push compares old and new manifests:

推送时系统对比新旧清单差异：

- **Newly tracked files** → pushed to remote / **新增追踪的文件** → 推送到远端
- **No longer tracked files** → deleted from remote (GitHub DELETE API) / **不再追踪的文件** → 从远端删除（调用 GitHub DELETE API）
- **Empty directories** → written as directory markers in manifest, auto-created on pull / **空目录** → 作为目录标记写入清单，拉取时自动创建

When 0 files are tracked, pushing shows a confirmation: "Push will clear all remote AI config files".

当 0 个文件被追踪时点击推送，会弹出确认框："推送将清空远端所有AI配置文件"。

---

## Create AI Skeleton / 创建 AI 骨架

Click **"Create AI Skeleton"** → choose **Workspace**, **Notes Space**, or **All** → confirm.

点击 **「创建 AI 骨架」** → 选择 **Workspace**、**Notes Space** 或 **All** → 确认。

- Creates `.ai/` directories only (no files) / 只创建 `.ai/` 目录，不生成文件
- **Workspace**: `context`, `adr`, `specs`, `runs` / **Workspace**：`context`、`adr`、`specs`、`runs`
- **Notes Space**: `docs` and its architecture/domain/scenario/integration/suppliers/shared/runbooks subdirectories / **Notes Space**：`docs` 及其 architecture/domain/scenario/integration/suppliers/shared/runbooks 子目录
- **All**: both scopes / **All**：上述全部
- Quick Reference in the dialog explains each directory / 对话框内的快速参考说明每个目录的用途

---

## Ignore Rules / 忽略规则

Click the **"Ignore Rules"** button to open the edit dialog.

点击 **「忽略规则」** 按钮打开编辑对话框。

### Built-in Rules (always active) / 内置规则（始终生效）

`.DS_Store`, `Thumbs.db`, `desktop.ini`, `*.swp`, `*.swo`, `*.tmp`, `*.bak`

### Custom Rules / 自定义规则

One rule per line. Supported patterns:

每行一条，支持以下模式：

| Pattern / 模式 | Example / 示例 | Matches / 匹配 |
|------|------|------|
| Exact name / 精确文件名 | `notes.txt` | All files named notes.txt / 所有名为 notes.txt 的文件 |
| Extension wildcard / 扩展名通配 | `*.log` | All .log files / 所有 .log 文件 |
| Prefix wildcard / 前缀通配 | `temp*` | All files starting with temp / 所有 temp 开头的文件 |
| Directory path / 目录路径 | `node_modules/` | Ignore the directory and all contents / 忽略该目录及其所有内容 |
| Comment / 注释 | `# comment` | Matches nothing / 不匹配任何内容 |

Custom rules are persisted and survive IDE restarts.

自定义规则会持久化保存，重启 IDE 后仍然生效。

---

## Remote File Structure (GitHub) / 远端文件结构

Understanding the remote file layout is essential for testing and debugging sync.

了解远端文件布局对于测试和调试同步至关重要。

```
{basePath}/{projectName}/
├── ai-config-manifest.txt        ← File list (one path per line)
├── ai-config-registry.json       ← Metadata (tracked state, hashes, settings)
└── ai-configs/                   ← Actual file content
    ├── .cursor/rules/
    │   └── CLAUDE.md
    ├── .claude/
    │   └── CLAUDE.md
    └── .ai/
        └── ARCHITECTURE.md
```

| File / 文件 | Role / 作用 |
|------|------|
| `ai-config-manifest.txt` | Plain text list of tracked file paths (one per line). Used during pull to know which files to fetch. / 追踪文件路径列表（每行一个），拉取时据此获取文件。 |
| `ai-config-registry.json` | JSON metadata: customPaths, ignorePatterns, trackedEntries (path + tracked flag + type), lastPushedFileHashes, trackedEmptyDirs. **This is the conflict detection target** — auto-sync compares its MD5 hash. / JSON 元数据：自定义路径、忽略规则、追踪条目（路径+追踪标记+类型）、上次推送哈希、追踪空目录。**这是冲突检测的目标**——自动同步比较它的 MD5 哈希。 |
| `ai-configs/{relativePath}` | Actual file content stored as individual GitHub files. / 实际文件内容，存储为独立的 GitHub 文件。 |

---

## Testing Auto-Sync with Remote Changes / 测试自动同步与远端变更

### Scenario 1: Trigger Remote Conflict Detection / 场景一：触发远端冲突检测

This tests what happens when another device (or manual edit) changes the remote, then local auto-sync tries to push.

测试当其他设备（或手动编辑）修改了远端后，本地自动同步尝试推送会发生什么。

**Steps / 步骤：**

1. **Ensure auto-sync is active**: Do a manual push first so `lastSyncedRemoteMetadataHash` is set.

   **确保自动同步已激活**：先手动推送一次，建立基线。

2. **Edit `ai-config-registry.json` on GitHub**: Go to your sync repo on GitHub → navigate to `{basePath}/{projectName}/ai-config-registry.json` → click Edit → make any small change (e.g., add a space or toggle a `"tracked":true` to `false`) → commit.

   **在 GitHub 上编辑 `ai-config-registry.json`**：打开同步仓库 → 找到 `{basePath}/{projectName}/ai-config-registry.json` → 点击编辑 → 做任意小改动（例如加一个空格、或将某个 `"tracked":true` 改为 `false`）→ 提交。

3. **Modify a local tracked file**: Edit any tracked AI config file locally (e.g., add a line to `.cursor/rules/CLAUDE.md`).

   **修改本地追踪文件**：编辑任一本地已追踪的 AI 配置文件（例如在 `.cursor/rules/CLAUDE.md` 中添加一行）。

4. **Observe**: After 5 seconds, a conflict dialog should appear with three buttons: **Pull from Remote** / **Force Push to Remote** / **Cancel**. Auto-sync is paused.

   **观察**：5 秒后应弹出冲突对话框，包含三个按钮：**从远端拉取** / **强制推送到远端** / **取消**。自动同步暂停。

### Scenario 2: Normal Auto-Sync Push / 场景二：正常自动推送

1. Do a manual push to establish baseline / 手动推送建立基线
2. Edit a local tracked file / 编辑本地追踪文件
3. Wait 5 seconds / 等待 5 秒
4. Check GitHub — the file should be updated / 检查 GitHub — 文件应已更新

### Scenario 3: Edit Remote File Content Directly / 场景三：直接编辑远端文件内容

**What to edit**: `{basePath}/{projectName}/ai-configs/.cursor/rules/CLAUDE.md` (or any config file under `ai-configs/`).

**编辑目标**：`{basePath}/{projectName}/ai-configs/.cursor/rules/CLAUDE.md`（或 `ai-configs/` 下任意配置文件）。

**What happens**: If you only edit the file content on GitHub **without** also changing `ai-config-registry.json`, the local auto-sync **will not detect a conflict** — it will push and overwrite the remote file. This is by design: conflict detection is metadata-level, not file-level. To get the remote file changes, you need to **manually pull** before local auto-sync overwrites them.

**结果**：如果只在 GitHub 上编辑文件内容而**不修改** `ai-config-registry.json`，本地自动同步**不会检测到冲突**——会推送并覆盖远端文件。这是设计如此：冲突检测基于元数据级别，非文件级别。要获取远端文件变更，需要在本地自动同步覆盖前**手动拉取**。

### Summary Table / 总结表

| What you edit on GitHub / 在 GitHub 上编辑 | Conflict detected? / 检测到冲突？ | Auto-sync behavior / 自动同步行为 |
|------|------|------|
| `ai-config-registry.json` | Yes ✓ | Paused + notification / 暂停 + 通知 |
| `ai-configs/` files only | No ✗ | Pushes normally (may overwrite) / 正常推送（可能覆盖） |
| Both registry + files | Yes ✓ | Paused + notification / 暂停 + 通知 |

> **Tip**: In a real multi-device scenario, conflict detection always works correctly because the other device's plugin push updates **both** the config files and the registry.json simultaneously. Direct GitHub editing is an edge case.
>
> **提示**：在真实多设备场景中，冲突检测始终有效，因为另一台设备的插件推送会**同时更新**配置文件和 registry.json。直接编辑 GitHub 是边缘场景。

---

## Data Persistence / 数据持久化

The following states are preserved across IDE restarts (stored in `.idea/aiConfigRegistry.xml`):

以下状态跨 IDE 重启保留（存储在 `.idea/aiConfigRegistry.xml`）：

- File tracked/untracked state / 文件追踪/取消追踪状态
- Custom scan paths / 自定义扫描路径
- Custom ignore rules / 自定义忽略规则
- Last push hashes (for change detection and sync status icons) / 上次推送的哈希值（用于变更检测和同步状态标记）

---

## FAQ / 常见问题

**Q: Does pushing notes also push AI configs? / 推送笔记时会自动推送 AI 配置吗？**

No. They are completely independent. / 不会。两者完全独立。

**Q: Which AI tools are supported? / 支持哪些 AI 工具？**

Built-in: Cursor, Claude, AI Docs, Windsurf, Codex, Copilot. Track any file via custom paths. v3.7.2+ also supports registering custom AI tool types at runtime.

内置 Cursor、Claude、AI Docs、Windsurf、Codex、Copilot，可通过自定义路径追踪任意文件。v3.7.2+ 还支持运行时注册自定义 AI 工具类型。

**Q: Will AI config files be committed to the project Git? / AI 配置文件会被提交到项目 Git 吗？**

No. AI Workspace does not change files' Git status. Sync uses a separate GitHub repo.

不会。AI 工作空间不改变文件的 Git 状态。同步使用的是独立的 GitHub 仓库。

**Q: Why doesn't auto-sync push after I enable it? / 为什么开启自动同步后不推送？**

Auto-sync requires at least one manual push or pull to set the conflict detection baseline. Do a manual push first, then auto-sync will activate.

自动同步需要至少一次手动推送或拉取来设置冲突检测基线。先手动推送一次，之后自动同步才会激活。

**Q: I edited a file on GitHub but auto-sync didn't detect the conflict? / 我在 GitHub 上编辑了文件但自动同步没检测到冲突？**

Conflict detection compares the `ai-config-registry.json` metadata hash, not individual file hashes. If you only edited file content under `ai-configs/`, the metadata hash is unchanged. In real multi-device usage, the plugin always updates both files and registry together, so this is only an issue with direct GitHub edits.

冲突检测比较的是 `ai-config-registry.json` 元数据哈希，而非单个文件哈希。如果只编辑了 `ai-configs/` 下的文件内容，元数据哈希不变。在真实多设备使用中，插件总是同时更新文件和 registry，所以这只在直接编辑 GitHub 时才会出现。

---

> Issues or suggestions: [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
>
> 问题或建议请提交 [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
