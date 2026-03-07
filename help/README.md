# Code Reading Mark Note Pro - Help / 帮助文档

> IntelliJ IDEA code reading notes plugin | [GitHub](https://github.com/z-soulx/CodeReadingMarkNotePro) | [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro)
>
> IntelliJ IDEA 代码阅读笔记插件 | [GitHub](https://github.com/z-soulx/CodeReadingMarkNotePro) | [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro)

## Overview / 功能概览

Code Reading Mark Note Pro helps you create notes and bookmarks while reading source code, with group management, cross-device sync, and AI config workspace.

Code Reading Mark Note Pro 帮助你在阅读源码时创建笔记和书签，支持分组管理、跨设备同步、AI 配置空间管理。

### Core Features / 核心功能

| Feature / 功能 | Description / 说明 |
|------|------|
| **Code Notes / 代码笔记** | Create notes on any source line with Topic grouping, tags, and trash bin / 在源码任意行创建笔记，支持 Topic 分组、标签管理、回收站 |
| **Gutter Marks / Gutter 标记** | Note icons in editor gutter with hover preview & edit / 编辑器左侧显示笔记图标，悬浮查看/编辑 |
| **Search / 搜索** | Global search across all notes / 全局搜索笔记内容 |
| **GitHub Sync / GitHub 同步** | Push/pull notes to a dedicated GitHub repo for cross-device sync / 笔记数据推送/拉取到独立 GitHub 仓库，跨设备同步 |
| **AI Workspace / AI 工作空间** | Manage personal AI config files (Cursor Rules, Claude, Codex, etc.) with independent sync, ignore rules, empty dir support / 管理个人 AI 配置文件（Cursor Rules、Claude、Codex 等），独立同步、忽略规则、空目录管理 |
| **Multi-language / 多语言** | Chinese/English UI, runtime switching / 中文/英文 UI，运行时切换 |

---

## Guides / 详细指南

- **[Sync Guide / 同步功能指南](SYNC_GUIDE.md)** — GitHub sync setup, push/pull, merge strategies, remote repo structure / GitHub 同步配置、推送拉取、合并策略、远程仓库结构
- **[AI Workspace Guide / AI 工作空间指南](AI_WORKSPACE_GUIDE.md)** — AI config management, file tree, sync status, quick create, ignore rules / AI 配置管理、文件树、同步状态、快速新建、忽略规则

---

## Quick Start / 快速入门

### 1. Create a Note / 创建笔记

1. Place cursor on the code line you want to annotate / 在编辑器中将光标放在要标注的代码行
2. Right-click → **Add to Code Reading Note** (or use shortcut `Alt+M`) / 右键 → **Add to Code Reading Note**（或快捷键 `Alt+M`）
3. Select an existing Topic or create a new one / 选择已有 Topic 或创建新 Topic
4. Enter note description / 输入笔记描述

### 2. View Notes / 查看笔记

- **Tool Window**: View → Tool Windows → Code Reading Note / **工具窗口**：View → Tool Windows → Code Reading Note
- **Gutter Icon**: Bookmark icon in editor gutter, hover to preview / **Gutter 图标**：编辑器左侧的书签图标，悬浮即可查看
- **Search Tab**: Second tab in the tool window / **搜索标签页**：在工具窗口第二个标签页中搜索
- **Help Button (?)**: Located on the right side of the main toolbar, opens this help doc / **帮助按钮 (?)**：主工具栏右侧，可随时打开本帮助文档

### 3. Configure Sync / 配置同步

See [Sync Guide](SYNC_GUIDE.md). / 详见 [同步功能指南](SYNC_GUIDE.md)。

### 4. AI Workspace / AI 工作空间

See [AI Workspace Guide](AI_WORKSPACE_GUIDE.md). / 详见 [AI 工作空间指南](AI_WORKSPACE_GUIDE.md)。

---

## Settings / 设置

**Settings → Tools → Code Reading Note Sync**

| Setting / 设置项 | Description / 说明 |
|--------|------|
| Enable Sync | Enable/disable sync / 启用/禁用同步功能 |
| Repository | GitHub repo address (`owner/repo`) / GitHub 仓库地址（格式：`owner/repo`） |
| Token | GitHub Personal Access Token (requires `repo` scope) / GitHub Personal Access Token（需要 `repo` 权限） |
| Branch | Target branch (default `main`) / 目标分支（默认 `main`） |
| Base Path | Remote storage root (default `code-reading-notes`) / 远程存储根路径（默认 `code-reading-notes`） |

---

## FAQ / 常见问题

### Q: Where is note data stored? / 笔记数据存在哪里？

Stored locally in `.idea/CodeReadingNote.xml`. Sync pushes data to your configured GitHub repo.

本地存储在 `.idea/CodeReadingNote.xml`。同步功能将数据推送到你配置的独立 GitHub 仓库。

### Q: Which IDEs are supported? / 支持哪些 IDE？

All IDEs based on IntelliJ Platform 2024.3+: IntelliJ IDEA, WebStorm, PyCharm, GoLand, etc.

所有基于 IntelliJ Platform 2024.3+ 的 IDE：IntelliJ IDEA、WebStorm、PyCharm、GoLand 等。

### Q: Does the plugin modify project code? / 插件会影响项目代码吗？

No. Note data is stored in `.idea/` directory and does not modify source files.

不会。笔记数据存储在 `.idea/` 目录中，不会修改源码文件。

---

> Issues or suggestions: [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
>
> 问题或建议请提交 [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
