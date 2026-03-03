# Sync Guide / 同步功能指南

> Code Reading Mark Note Pro v3.4.0+

## Overview / 概述

The sync feature pushes code reading notes to a dedicated GitHub repository for cross-device sync and cloud backup. Notes are stored in a separate repo and will not pollute your project code.

同步功能将代码阅读笔记推送到独立 GitHub 仓库，实现跨设备同步和云端备份。笔记存储在独立仓库中，不会污染项目代码。

---

## Quick Setup / 快速配置

### 1. Create a GitHub Repository / 创建 GitHub 仓库

Create a new GitHub repository (private recommended) dedicated to storing note data.

创建一个新的 GitHub 仓库（推荐私有），专门用于存储笔记数据。

### 2. Generate Token / 生成 Token

GitHub → Settings → Developer settings → Personal Access Tokens → generate a token (requires `repo` scope).

GitHub → Settings → Developer settings → Personal Access Tokens → 生成 Token（需要 `repo` 权限）。

### 3. Configure in IDE / 在 IDE 中配置

**Settings → Tools → Code Reading Note Sync**

| Setting / 设置项 | Example / 示例值 |
|--------|--------|
| Enable Sync | ✅ |
| Repository | `your-name/my-code-notes` |
| Token | `ghp_xxxxxxxxxxxx` |
| Branch | `main` |
| Base Path | `code-reading-notes` |

Click **Apply** to save. / 点击 **Apply** 保存。

---

## Usage / 使用方法

### Push Notes / 推送笔记

Tool window toolbar → click **⬆ Push to Remote**

工具窗口工具栏 → 点击 **⬆ Push to Remote**

Auto MD5 check before push — skips if content is unchanged.

推送前会自动进行 MD5 校验，如果内容未变化则跳过。

### Pull Notes / 拉取笔记

Tool window toolbar → click **⬇ Pull from Remote**

工具窗口工具栏 → 点击 **⬇ Pull from Remote**

Pull mode options: / 拉取时可选择：

- **Merge**: Merge remote with local data (safe mode, no local data loss) / **合并**：远程数据与本地数据合并（安全模式，不丢失本地数据）
- **Overwrite**: Replace local data with remote data / **覆盖**：用远程数据替换本地数据

---

## Remote Repo Structure / 远程仓库结构

```
your-sync-repo / 你的同步仓库/
  code-reading-notes/
    <project-name / 项目名>/
      CodeReadingNote.xml           ← note data / 笔记数据
      CodeReadingNote.xml.md5       ← MD5 checksum / MD5 校验文件
      ai-configs/                   ← AI config files (independent sync) / AI 配置文件（独立同步）
        .ai/ARCHITECTURE.md
        .cursor/rules/my-rules.md
        ...
      ai-config-manifest.txt        ← AI config manifest (incl. empty dir markers) / AI 配置清单（含空目录标记）
```

- Each project uses its name as a subdirectory / 每个项目使用项目名作为子目录
- Notes and AI configs are stored separately / 笔记数据和 AI 配置分别存储，互不干扰
- MD5 files for change detection, avoiding unnecessary pushes / MD5 文件用于变更检测，避免无效推送
- AI config manifest contains file paths + empty dir markers (ending with `/`) / AI 配置清单包含文件路径 + 空目录标记（以 `/` 结尾）
- Push auto-deletes remote files that are no longer tracked / 推送时自动删除远端不再追踪的文件

---

## Use Cases / 使用场景

### Cross-device Sync / 跨设备同步

```
Work PC ⬆ Push → GitHub → ⬇ Pull Home PC
公司电脑 ⬆ Push → GitHub → ⬇ Pull 家里电脑
```

### Team Knowledge Sharing / 团队知识共享

```
Member A ⬆ Push → Shared Repo ← ⬇ Pull Member B
成员A ⬆ Push → 共享仓库 ← ⬇ Pull 成员B
```

### Note Backup / 笔记备份

```
Local Notes ⬆ Push → GitHub permanent storage + version history
本地笔记 ⬆ Push → GitHub 永久保存 + 版本历史
```

---

## Notes / 注意事项

- **Security / 安全性**: Token is stored in IDE config; use a private repo / Token 存储在 IDE 配置中，建议使用私有仓库
- **Merge mode / 合并模式**: No local data loss, recommended for daily use / 不会丢失本地数据，推荐日常使用
- **Overwrite mode / 覆盖模式**: Clears local data, use with caution / 会清空本地数据，谨慎使用
- **AI config sync / AI 配置同步**: Independent from notes sync, manual operation in AI Workspace tab. See [AI Workspace Guide](AI_WORKSPACE_GUIDE.md) / 独立于笔记同步，在 AI 工作空间标签页中手动操作，详见 [AI 工作空间指南](AI_WORKSPACE_GUIDE.md)
- **Remote deletion / 远端删除**: Untracking a file then pushing deletes it from remote; untrack all and push clears remote / 取消追踪文件后推送，该文件会从远端删除；若全部取消追踪并推送，远端将清空

---

> Issues or suggestions: [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
>
> 问题或建议请提交 [GitHub Issues](https://github.com/z-soulx/CodeReadingMarkNotePro/issues)
