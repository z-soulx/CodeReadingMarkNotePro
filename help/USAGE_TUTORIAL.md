# Code Reading Mark Note Pro — Usage Tutorial / 完整使用教程

> Code Reading Mark Note Pro v3.7.5+ | [GitHub](https://github.com/z-soulx/CodeReadingMarkNotePro) | [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro)

---

## Table of Contents / 目录

1. [Core Concepts / 核心概念](#1-core-concepts--核心概念)
2. [Installation / 安装](#2-installation--安装)
3. [Creating Notes / 创建笔记](#3-creating-notes--创建笔记)
4. [Viewing & Managing Notes / 查看与管理笔记](#4-viewing--managing-notes--查看与管理笔记)
5. [Gutter Icons & Note Popup / 行首图标与笔记弹窗](#5-gutter-icons--note-popup--行首图标与笔记弹窗)
6. [Navigation / 导航功能](#6-navigation--导航功能)
7. [Search / 搜索](#7-search--搜索)
8. [Group Management / 分组管理](#8-group-management--分组管理)
9. [Drag & Drop Sorting / 拖拽排序](#9-drag--drop-sorting--拖拽排序)
10. [Batch Operations / 批量操作](#10-batch-operations--批量操作)
11. [Trash Bin / 废纸篓](#11-trash-bin--废纸篓)
12. [Bookmark Repair / 书签修复](#12-bookmark-repair--书签修复)
13. [Line Number Editing / 行号编辑](#13-line-number-editing--行号编辑)
14. [GitHub Sync / GitHub 同步](#14-github-sync--github-同步)
15. [AI Workspace / AI 工作空间](#15-ai-workspace--ai-工作空间)
16. [Settings / 设置](#16-settings--设置)
17. [Keyboard Shortcuts Reference / 快捷键速查表](#17-keyboard-shortcuts-reference--快捷键速查表)
18. [FAQ / 常见问题](#18-faq--常见问题)

---

## 1. Core Concepts / 核心概念

The plugin uses a three-level hierarchy to organize your code reading notes:

插件采用三级层次结构组织代码阅读笔记：

```
Topic (主题)
  └── Group (分组, optional / 可选)
        └── TopicLine (笔记条目)
```

| Concept / 概念 | Description / 说明 |
|------|------|
| **Topic** | A reading theme, e.g. "Authentication Flow", "Database Layer". Like a notebook / 阅读主题，如"认证流程"、"数据库层"，相当于一个笔记本 |
| **Group** | Optional sub-category within a Topic, e.g. "Login", "Token Refresh" / Topic 内的可选子分类，如"登录"、"Token 刷新" |
| **TopicLine** | A single note entry, bound to a specific file + line number, with optional note text / 单条笔记，绑定到特定文件和行号，可附带备注 |

Each TopicLine is backed by an IntelliJ native **Bookmark**, which tracks line number changes automatically when code is edited.

每个 TopicLine 底层关联一个 IntelliJ 原生**书签**，代码编辑时行号会自动跟踪。

---

## 2. Installation / 安装

### From JetBrains Marketplace / 从 Marketplace 安装

1. Open IDE → **Settings → Plugins → Marketplace**
2. Search **"Code Reading Mark Note Pro"**
3. Click **Install** → Restart IDE

打开 IDE → **设置 → 插件 → Marketplace** → 搜索 **"Code Reading Mark Note Pro"** → 安装 → 重启 IDE

### Supported IDEs / 支持的 IDE

All IntelliJ Platform 2024.3+ IDEs: IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, RubyMine, CLion, Android Studio, etc.

所有基于 IntelliJ Platform 2024.3+ 的 IDE 均支持。

---

## 3. Creating Notes / 创建笔记

### 3.1 Add Code Line to Topic / 将代码行添加到主题

**Method 1: Keyboard shortcut / 快捷键**

Press `Alt+M` with cursor on the target line.

将光标放在目标行，按 `Alt+M`。

**Method 2: Context menu / 右键菜单**

Right-click in editor → **Add to Code Reading Note**

在编辑器中右键 → **Add to Code Reading Note**

### 3.2 The "Add to Topic" Dialog / "添加到主题"对话框

The dialog has three panels:

对话框包含三个面板：

```
┌─────────────────────────────────────────────────────────┐
│              Add to Topic (with Group)                  │
├──────────────┬──────────────┬───────────────────────────┤
│ Select Topic │ Select Group │ Note (Optional)           │
│              │  (Optional)  │                           │
│  Topic A     │  No Group    │  [Markdown editor]        │
│  Topic B  ←  │  Group 1     │                           │
│  Topic C     │  Group 2     │  Supports Markdown syntax │
│              │  + New Group │                           │
├──────────────┴──────────────┴───────────────────────────┤
│                 Add to Topic (Ctrl+Enter)   Cancel (Esc)│
└─────────────────────────────────────────────────────────┘
```

| Panel / 面板 | Description / 说明 |
|------|------|
| **Select Topic** | Choose the target topic. First topic is auto-selected / 选择目标主题，默认选中第一个 |
| **Select Group** | Optional. Choose "No Group", an existing group, or "+ Create New Group" / 可选。选择无分组、已有分组、或创建新分组 |
| **Note** | Optional Markdown note text / 可选的 Markdown 格式备注 |

**Dialog keyboard shortcuts / 对话框快捷键：**

| Shortcut / 快捷键 | Action / 操作 |
|------|------|
| `Ctrl+Enter` | Confirm add (same as clicking "Add to Topic" button) / 确认添加 |
| `Esc` | Close dialog / 关闭对话框 |
| `Alt+A` | Mnemonic for Add button / Add 按钮助记键 |
| `Alt+C` | Mnemonic for Cancel button / Cancel 按钮助记键 |

### 3.3 Create a New Topic / 创建新主题

In the tool window toolbar, click the **"+"** (New Topic) button → enter the topic name → OK.

在工具窗口工具栏点击 **"+"**（新建主题）→ 输入主题名 → 确定。

### 3.4 Same-Line Warning / 同行书签警告

If the target line already has a bookmark, a warning dialog will appear explaining IntelliJ's one-bookmark-per-line limitation. You can choose to continue or cancel.

如果目标行已有书签，会弹出警告说明 IntelliJ 每行只支持一个书签的限制。你可以选择继续或取消。

---

## 4. Viewing & Managing Notes / 查看与管理笔记

### 4.1 Open Tool Window / 打开工具窗口

**View → Tool Windows → Code Reading Mark Note Pro** (or click the icon at the bottom panel)

**View → Tool Windows → Code Reading Mark Note Pro**（或点击底部面板图标）

The tool window has three tabs:

工具窗口包含三个标签页：

| Tab / 标签页 | Description / 说明 |
|------|------|
| **Tree View** | Hierarchical view of all Topics → Groups → TopicLines / 所有笔记的树形层级视图 |
| **Search** | Global search across all notes / 全局搜索笔记 |
| **AI Workspace** | AI config file management / AI 配置文件管理 |

### 4.2 Tree View / 树形视图

- **Single-click** a TopicLine → navigate to the code line in editor / 单击 TopicLine → 跳转到编辑器中对应代码行
- **Right-click** → context menu with operations (rename, remove, move, edit line number, etc.) / 右键 → 操作菜单（重命名、删除、移动、编辑行号等）
- **Double-click** a Topic → expand/collapse / 双击 Topic → 展开/折叠

### 4.3 Toolbar Actions / 工具栏操作

| Button / 按钮 | Action / 操作 |
|------|------|
| **+ New Topic** | Create a new topic / 创建新主题 |
| **Rename** | Rename selected topic / 重命名选中主题 |
| **Remove** | Remove selected topic / 删除选中主题 |
| **Group ▼** | Group management dropdown (Add/Rename/Remove Group) / 分组管理下拉菜单 |
| **Expand All** | Expand all tree nodes / 展开所有节点 |
| **Collapse All** | Collapse all tree nodes / 折叠所有节点 |
| **Sync Positions** | Sync bookmark positions / 同步书签位置 |
| **⬆ Push** | Push notes to GitHub / 推送笔记到 GitHub |
| **⬇ Pull** | Pull notes from GitHub / 从 GitHub 拉取笔记 |
| **? Help** | Open help documentation / 打开帮助文档 |

---

## 5. Gutter Icons & Note Popup / 行首图标与笔记弹窗

### 5.1 Gutter Icons / 行首图标

Lines with notes display a bookmark ribbon icon in the editor gutter (left margin). Hover over the icon to see a preview tooltip.

有笔记的行会在编辑器左侧 Gutter 区域显示书签图标。悬停可查看预览提示。

### 5.2 Note Popup / 笔记弹窗

**Click** the gutter icon to open an interactive popup:

**点击**行首图标打开交互弹窗：

```
┌──────────────────────────┐
│ Code Note                │
│ ┌──────────────────────┐ │
│ │ Note text here...    │ │
│ │ (editable)           │ │
│ └──────────────────────┘ │
│ [Locate] [Tree] [Save] [Delete] │
└──────────────────────────┘
```

| Button / 按钮 | Action / 操作 |
|------|------|
| **Locate** | Navigate to note in tree view / 在树视图中定位该笔记 |
| **Tree** | Reverse locate — select note in TreeView / 反定位 — 在 TreeView 中选中该笔记 |
| **Save** | Save edited note (also: press `Enter`) / 保存编辑（也可按 `Enter`）|
| **Delete** | Delete note to trash (also: press `Alt+Delete`) / 删除笔记到废纸篓（也可按 `Alt+Delete`）|

### 5.3 Inline Annotations / 行尾注释

Note text appears as a teal-colored annotation at the end of the code line, clearly distinguishable from syntax highlighting.

笔记文本以青色行尾注释形式显示，与语法高亮明显区分。

---

## 6. Navigation / 导航功能

### 6.1 Navigate to Note / 跳转到笔记

Press `Alt+G` on any line with a note → opens the gutter note popup for editing/viewing.

在有笔记的行按 `Alt+G` → 打开笔记弹窗进行编辑/查看。

### 6.2 Reverse Locate / 反定位

From the editor, find and select the corresponding note node in the TreeView panel.

从编辑器中定位到 TreeView 中对应的笔记节点。

**Trigger methods / 触发方式：**
- Right-click → **Reverse Locate Note in Tree** / 右键 → **Reverse Locate Note in Tree**
- Gutter popup → **Tree** button / 笔记弹窗 → **Tree** 按钮

### 6.3 Tree → Code / 树视图 → 代码

Click any TopicLine in the TreeView → editor navigates to the corresponding file and line.

在 TreeView 中点击任意 TopicLine → 编辑器跳转到对应文件和行。

---

## 7. Search / 搜索

### 7.1 Open Search Tab / 打开搜索标签页

Click the **Search** tab (second tab) in the tool window.

点击工具窗口的**搜索**标签页（第二个）。

### 7.2 Search Features / 搜索功能

| Feature / 功能 | Description / 说明 |
|------|------|
| **Pinyin search** | Type Pinyin to match Chinese topic/note names / 输入拼音匹配中文主题/笔记名 |
| **Fuzzy matching** | Approximate text matching / 模糊文本匹配 |
| **Scope selector** | Choose: Topics Only / Bookmarks Only / All / 选择范围：仅 Topics / 仅 Bookmarks / 全部 |
| **Double-click result** | Navigate to the code location / 双击结果跳转到代码位置 |
| **Right-click result** | Context menu with actions / 右键显示操作菜单 |

---

## 8. Group Management / 分组管理

Groups provide an optional second level of organization within a Topic.

分组在 Topic 内提供可选的第二层组织结构。

### 8.1 Create Group / 创建分组

- Tool window toolbar → **Group ▼** dropdown → **Add Group** / 工具栏 → **分组▼** 下拉 → **添加分组**
- Or choose "+ Create New Group" when adding a note / 或在添加笔记时选择"+ 创建新分组"

### 8.2 Rename / Delete Group / 重命名 / 删除分组

- **Group ▼** dropdown → **Rename Group** / **Remove Group**
- Or right-click a group node in TreeView / 或在 TreeView 中右键分组节点

### 8.3 Move Notes Between Groups / 在分组间移动笔记

Right-click a TopicLine → **Move to Group** → select target group.

右键 TopicLine → **移动到分组** → 选择目标分组。

---

## 9. Drag & Drop Sorting / 拖拽排序

### 9.1 Supported Drag Operations / 支持的拖拽操作

| Source / 拖拽源 | Target / 目标 | Result / 效果 |
|------|------|------|
| **Topic** | Another Topic | Reorder topics / 调整主题顺序 |
| **Group** | Another Group (same Topic) | Reorder groups / 调整分组顺序 |
| **TopicLine** | Group / Ungrouped | Move to different group / 移动到不同分组 |

### 9.2 Rules / 规则

- Topic-level view is **read-only** (drag disabled) — switch to Group/Ungrouped view to enable drag sorting / Topic 级视图为**只读**（禁用拖拽）— 切换到 Group/Ungrouped 视图启用拖拽
- Tree expansion state is preserved after drag operations / 拖拽后树的展开状态保持不变

---

## 10. Batch Operations / 批量操作

### 10.1 Multi-Select / 多选

Hold `Ctrl` and click to select multiple TopicLines. Hold `Shift` to select a range.

按住 `Ctrl` 点击多选 TopicLine。按住 `Shift` 选择范围。

### 10.2 Batch Actions / 批量操作

| Action / 操作 | Description / 说明 |
|------|------|
| **Batch Delete** | Remove multiple selected TopicLines to trash / 批量删除选中的 TopicLine 到废纸篓 |
| **Batch Move** | Move multiple selected TopicLines to a target group / 批量移动选中的 TopicLine 到目标分组 |
| **Batch Adjust Line Numbers** | Adjust line numbers of selected TopicLines by offset / 批量调整选中 TopicLine 的行号 |

Right-click menu adapts automatically when multiple items are selected.

多选时右键菜单自动显示批量操作。

---

## 11. Trash Bin / 废纸篓

Deleted notes go to the **Trash** instead of being permanently removed.

删除的笔记进入**废纸篓**而非永久删除。

### 11.1 Delete a Note / 删除笔记

- Press `Alt+Delete` on a line with a note / 在有笔记的行按 `Alt+Delete`
- Right-click in TreeView → Remove / 在 TreeView 中右键 → 删除
- Gutter popup → Delete button / 笔记弹窗 → Delete 按钮

### 11.2 Restore / Permanently Delete / 恢复 / 彻底删除

In the TreeView, find the **Trash** node → right-click a trashed item:

在 TreeView 中找到**废纸篓**节点 → 右键已删除的条目：

- **Restore** — move back to original Topic / **恢复** — 移回原主题
- **Permanently Delete** — remove forever / **彻底删除** — 永久移除
- **Empty Trash** — clear all trashed items / **清空废纸篓** — 清除所有已删除条目

---

## 12. Bookmark Repair / 书签修复

Since TopicLines rely on IntelliJ Bookmarks, bookmarks can sometimes become "missing" after heavy code changes or branch switches.

由于 TopicLine 依赖 IntelliJ 书签，大量代码修改或分支切换后书签可能变为"丢失"状态。

### 12.1 Repair All / 全部修复

**Tools menu → Repair Code Reading Bookmarks**

**Tools 菜单 → Repair Code Reading Bookmarks**

Scans all TopicLines and recreates missing bookmarks.

扫描所有 TopicLine 并重新创建丢失的书签。

### 12.2 Repair Single / 修复单个

Right-click a TopicLine → **Repair This Bookmark**

右键 TopicLine → **Repair This Bookmark**

### 12.3 Sync Positions / 同步位置

Toolbar → **Sync Positions** — shows a preview dialog with status for each TopicLine:

工具栏 → **同步位置** — 显示预览对话框，列出每个 TopicLine 的状态：

| Status / 状态 | Meaning / 含义 |
|------|------|
| ✅ Synced | Position is correct / 位置正确 |
| ⚠️ Needs Fix | Line number drifted (e.g. 38 → 42) / 行号偏移 |
| ❌ Missing | Bookmark is missing / 书签丢失 |
| 🚫 File Not Found | Source file no longer exists / 源文件不存在 |

You can also **Clean Up Error Entries** to batch-remove items with missing bookmarks or files.

也可以**清理错误条目**来批量移除书签丢失或文件不存在的条目。

---

## 13. Line Number Editing / 行号编辑

### 13.1 Edit Single Line Number / 编辑单个行号

Right-click a TopicLine → **Edit Line Number** → enter new line number.

右键 TopicLine → **编辑行号** → 输入新行号。

The associated bookmark is automatically updated.

关联的书签会自动更新。

### 13.2 Batch Adjust / 批量调整

Select multiple TopicLines → right-click → **Batch Adjust Line Numbers**

多选 TopicLine → 右键 → **批量调整行号**

Modes / 模式:
- **Add offset** — shift all selected lines down / 向下偏移
- **Subtract offset** — shift all selected lines up / 向上偏移
- **Set to specific** — set all to a fixed line number / 设为固定行号

---

## 14. GitHub Sync / GitHub 同步

Sync your code reading notes to a dedicated GitHub repository for cross-device sync and backup.

将代码阅读笔记同步到独立 GitHub 仓库，实现跨设备同步和备份。

### 14.1 Quick Setup / 快速配置

1. Create a GitHub repository (private recommended) / 创建 GitHub 仓库（推荐私有）
2. Generate a Personal Access Token with `repo` scope / 生成 Token（需 `repo` 权限）
3. **Settings → Tools → Code Reading Note Sync** → fill in config → Apply

### 14.2 Push & Pull / 推送与拉取

| Action / 操作 | Button / 按钮 | Description / 说明 |
|------|------|------|
| **Push** | ⬆ | Upload local notes to GitHub / 上传本地笔记到 GitHub |
| **Pull** | ⬇ | Download notes from GitHub (Merge or Overwrite) / 从 GitHub 下载笔记（合并或覆盖）|

### 14.3 Auto-Sync / 自动同步

Enable in Settings → auto-pushes on data changes with conflict detection.

在设置中启用 → 数据变更时自动推送，带冲突检测。

> For detailed setup and advanced scenarios, see [Sync Guide / 同步指南](SYNC_GUIDE.md).

---

## 15. AI Workspace / AI 工作空间

Manage personal AI config files (Cursor Rules, Claude, Codex, Windsurf, Copilot, etc.) with a dedicated tab.

通过专用标签页管理个人 AI 配置文件。

### 15.1 Features / 功能

- Hierarchical file tree with sync status icons (★ New / ● Modified / ✓ Synced) / 带同步状态图标的层级文件树
- Per-file checkbox tracking / 逐文件复选框追踪
- Independent push/pull from notes sync / 独立于笔记同步的推送/拉取
- Auto-sync with conflict detection / 自动同步与冲突检测
- .ai/ skeleton creator with Workspace / Notes Space / All scopes / .ai/ 骨架创建器（Workspace / Notes Space / All）
- Opt-in `.ai` Git with IDEA Commit UI (choose files and message yourself) / 可选 `.ai` Git，用 IDEA 提交界面自己选文件和写说明
- Custom commands in IDEA Terminal or Silent Terminal (no focus); first missing `.ai/workspace-commands.json` is seeded with Cursor and Typora (deletable, not re-injected) / 自定义命令：IDEA Terminal 或静默终端（不抢焦点）；第一次没有 `.ai/workspace-commands.json` 时写入 Cursor、Typora（可删，不回填）
- Configurable ignore rules / 可配置忽略规则
- Custom path tracking / 自定义路径追踪

> For full documentation, see [AI Workspace Guide / AI 工作空间指南](AI_WORKSPACE_GUIDE.md).

---

## 16. Settings / 设置

### 16.1 Plugin Language / 插件语言

**Settings → Tools → Code Reading Note Sync** → Language dropdown

Supports English and Chinese. Changes take effect immediately; no IDE restart is required.

支持英文和中文，立即生效，无需重启 IDE。

### 16.2 Sync Configuration / 同步配置

| Setting / 设置项 | Description / 说明 |
|------|------|
| **Enable Sync** | Enable/disable sync / 启用/禁用同步 |
| **Auto Sync** | Auto-push when data changes / 数据变更时自动推送 |
| **AI Config Auto Sync** | Auto-push AI config changes / AI 配置变更时自动推送 |
| **Repository** | GitHub repo (`owner/repo`) / GitHub 仓库地址 |
| **Token** | Personal Access Token / 个人访问令牌 |
| **Branch** | Target branch (default: `main`) / 目标分支 |
| **Base Path** | Remote storage root (default: `code-reading-notes`) / 远程存储根路径 |

### 16.3 Custom Keymaps / 自定义快捷键

**Settings → Keymap** → search **"Code Reading"** to customize all shortcuts.

**设置 → 按键映射** → 搜索 **"Code Reading"** 来自定义所有快捷键。

---

## 17. Keyboard Shortcuts Reference / 快捷键速查表

### Global Shortcuts / 全局快捷键

| Shortcut / 快捷键 | Context / 上下文 | Action / 操作 |
|------|------|------|
| `Alt+M` | Editor | Add current line to Topic / 将当前行添加到主题 |
| `Alt+G` | Editor | Navigate to note / open note popup / 跳转到笔记 / 打开笔记弹窗 |
| `Alt+Delete` | Editor | Delete note at current line to trash / 删除当前行笔记到废纸篓 |

### "Add to Topic" Dialog Shortcuts / "添加到主题"对话框快捷键

| Shortcut / 快捷键 | Action / 操作 |
|------|------|
| `Ctrl+Enter` | Confirm and add / 确认添加 |
| `Esc` | Close dialog / 关闭对话框 |
| `Alt+A` | Focus Add button / 聚焦添加按钮 |
| `Alt+C` | Focus Cancel button / 聚焦取消按钮 |

### Gutter Note Popup Shortcuts / 笔记弹窗快捷键

| Shortcut / 快捷键 | Action / 操作 |
|------|------|
| `Enter` | Save note / 保存笔记 |
| `Alt+Delete` | Delete note / 删除笔记 |

### Tree View / 树视图

| Shortcut / 快捷键 | Action / 操作 |
|------|------|
| `Ctrl+Click` | Multi-select / 多选 |
| `Shift+Click` | Range select / 范围选择 |
| Drag & Drop | Reorder items / 拖拽排序 |

---

## 18. FAQ / 常见问题

### Q: Where is note data stored? / 笔记数据存在哪里？

Locally in `.idea/CodeReadingNote.xml`. GitHub Sync pushes a copy to your configured remote repo.

本地存储在 `.idea/CodeReadingNote.xml`，GitHub 同步会将副本推送到配置的远程仓库。

### Q: Does the plugin modify project source code? / 插件会修改项目源码吗？

No. All data is stored in `.idea/` directory. Source files are never modified.

不会。所有数据存储在 `.idea/` 目录中，源文件不会被修改。

### Q: Which IDEs are supported? / 支持哪些 IDE？

All IntelliJ Platform 2024.3+ IDEs (no upper version limit).

所有基于 IntelliJ Platform 2024.3+ 的 IDE（无版本上限）。

### Q: I switched Git branches and my notes show errors. / 切换 Git 分支后笔记显示错误。

This is expected — notes reference specific files and lines. Switch back to the original branch to restore. You can also use **Repair Bookmarks** to fix stale references.

这是正常现象 — 笔记引用了特定文件和行。切回原分支即可恢复。也可以使用**修复书签**功能修复过期引用。

### Q: How do I migrate notes to a new machine? / 如何迁移笔记到新电脑？

Use GitHub Sync: Push from old machine → Pull on new machine.

使用 GitHub 同步：旧电脑上推送 → 新电脑上拉取。

### Q: Can multiple team members share notes? / 团队成员能共享笔记吗？

Yes. Use a shared GitHub repository. Members push/pull to stay in sync.

可以。使用共享 GitHub 仓库，成员通过推送/拉取保持同步。

### Q: The inline annotation color is too bright / too dark. / 行尾注释颜色太亮/太暗。

The annotation uses a fixed teal color designed to be distinguishable in both light and dark themes. Custom color is not configurable yet.

注释使用固定青色，在明暗主题下均可区分。暂不支持自定义颜色。

---

> Issues or suggestions: [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
>
> 问题或建议请提交 [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
