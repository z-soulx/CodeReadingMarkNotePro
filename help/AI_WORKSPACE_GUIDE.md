# AI Workspace Guide / AI 工作空间使用指南

> Code Reading Mark Note Pro v3.7.1+

## Overview / 概述

The **AI Workspace** brings personal AI config files (Cursor Rules, Claude rules, architecture docs, etc.) under unified management with a hierarchical file tree, sync status tracking, and independent manual sync.

**AI 工作空间** 将个人 AI 配置文件（Cursor Rules、Claude 规则、AI 架构文档等）纳入统一管理，提供层级文件树、同步状态追踪、独立手动同步。

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
| **Scan / 扫描** | Re-scan project for AI config files / 重新扫描项目中的 AI 配置文件 |
| **Add Custom Path / 添加自定义路径** | Track non-default directories (auto-validates path and provides feedback) / 添加非默认目录进行追踪（自动验证路径有效性并反馈结果） |
| **New AI Config / 新建AI配置** | Choose type → auto-create directory & file with skeleton → open in editor / 选择类型 → 自动创建目录和文件（带骨架内容）→ 打开编辑 |
| **Ignore Rules / 忽略规则** | Edit file/directory ignore rules (.gitignore-like, supports `*.ext`, `name`, `dir/`) / 编辑文件/目录忽略规则（类似 .gitignore，支持 `*.ext`、`name`、`dir/` 等模式） |
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
- **AI config sync** (AI Workspace ⬆⬇): Less frequent, manual-only / **AI 配置同步**（AI 工作空间内 ⬆⬇）：变动较少，仅手动触发

### Push / 推送

1. Check the files you want to sync in the file tree / 在文件树中勾选需要同步的文件
2. Click **"Push AI Configs"** / 点击 **「推送AI配置」**
3. Auto-detects changes — skips if nothing changed / 系统自动检测变更 — 无变化则跳过
4. Status icons update after push / 推送完成后状态标记自动更新

### Pull / 拉取

1. Click **"Pull AI Configs"** / 点击 **「拉取AI配置」**
2. Confirmation dialog warns "pull will overwrite local files" / 确认对话框提醒"拉取将覆盖本地文件"
3. Remote files are written to local directories, tree auto-refreshes / 远程文件写入本地对应目录，文件树自动刷新

### Untrack → Push = Remote Delete / 取消追踪 → 推送 = 远端删除

Push compares old and new manifests:

推送时系统对比新旧清单差异：

- **Newly tracked files** → pushed to remote / **新增追踪的文件** → 推送到远端
- **No longer tracked files** → deleted from remote (GitHub DELETE API) / **不再追踪的文件** → 从远端删除（调用 GitHub DELETE API）
- **Empty directories** → written as directory markers in manifest, auto-created on pull / **空目录** → 作为目录标记写入清单，拉取时自动创建

When 0 files are tracked, pushing shows a confirmation: "Push will clear all remote AI config files".

当 0 个文件被追踪时点击推送，会弹出确认框："推送将清空远端所有AI配置文件"。

---

## Quick Create / 快速新建

Click **"New AI Config"** → choose type → enter file name → confirm.

点击 **「新建AI配置」** → 选择类型 → 输入文件名 → 确认。

- Types: Cursor Rule / Claude Rule / AI Doc / Windsurf / Codex / Copilot / Custom
- 类型：Cursor Rule / Claude Rule / AI Doc / Windsurf / Codex / Copilot / Custom
- Auto-creates directory and file with skeleton content / 自动创建目录和文件，填充骨架内容
- Opens in editor after creation / 创建后自动在编辑器中打开

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

Built-in: Cursor, Claude, AI Docs, Windsurf, Codex, Copilot. Track any file via custom paths.

内置 Cursor、Claude、AI Docs、Windsurf、Codex、Copilot，可通过自定义路径追踪任意文件。

**Q: Will AI config files be committed to the project Git? / AI 配置文件会被提交到项目 Git 吗？**

No. AI Workspace does not change files' Git status. Sync uses a separate GitHub repo.

不会。AI 工作空间不改变文件的 Git 状态。同步使用的是独立的 GitHub 仓库。

---

> Issues or suggestions: [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
>
> 问题或建议请提交 [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
