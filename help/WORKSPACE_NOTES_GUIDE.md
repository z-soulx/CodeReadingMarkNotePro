# 多项目工作空间笔记 / Workspace Notes

在 IDEA 中打开包含多个项目的父目录。插件会自动发现各层含 `.idea` 的子目录，加载它们的主题、分组、笔记和回收站，无需逐个打开或导入。

Open the parent directory in IDEA. The plugin discovers nested directories containing `.idea` and loads their topics, groups, notes, and trash without opening additional IDE projects.

```text
工作空间 / Workspace
├─ workspace-root
│  └─ 主题 / Topics
├─ project-a
│  └─ 主题 → 分组 → 笔记
└─ packages/project-b
   └─ Topics → Groups → Notes
```

同名主题属于各自项目；同名目录通过相对路径区分。没有子项目时保持单项目视图。含 `.idea` 但没有笔记文件的子项目显示为空，第一次添加主题或笔记时才保存文件。

Identical topic names stay separate. Relative project paths distinguish directories with the same name. With no nested projects, the original single-project view remains. Empty projects appear immediately and receive a notes file on the first edit.

## 日常操作 / Everyday use

1. 选择项目、主题、分组或笔记后，工具栏“新建主题”“导入”“导出”作用于该项目；没有选择时会要求选择项目。导出包含该项目的回收站。
2. 编辑器 `Alt+M` 根据代码文件最近的项目根选择归属；该项目没有主题时先提示创建主题。根目录直属文件属于根项目。
3. 搜索覆盖工作空间全部笔记，结果显示来源项目。双击结果或树中笔记在当前 IDEA 窗口打开代码。左侧行标记及弹窗可直接编辑来源笔记。
4. 删除笔记后，在所属项目的回收站右键恢复或永久删除。移动笔记和主题限于同一项目。
5. 目录或笔记文件变化会自动刷新，也可点击工具栏“刷新工作空间”。代码文件暂时缺失时，笔记保留并显示无法定位，文件恢复后可继续跳转。

Select a project or one of its notes before creating topics, importing, or exporting. `Alt+M` uses the nearest project root for the code file. Search spans all note projects and shows each result's source. Tree navigation and gutter editing use the current IDEA window. Trash and moves stay within their owning project. Use **Refresh Workspace** to rescan after directory or file changes.

扫描跳过 `.git`、`.idea` 内部、`node_modules`、`build`、`target`、`out`、`.gradle`，不跟随目录符号链接或 Windows junction。

Discovery skips those generated/internal directories and does not follow directory symlinks or Windows junctions.

## 保存与冲突 / Saving and conflicts

每个项目仍保存到自己的 `.idea/CodeReadingNote.xml`。子项目编辑不会合并到根项目笔记文件，也不会进入根项目 GitHub 同步载荷。旧版本插件仍可读取这些文件。

Each project keeps its own compatible `.idea/CodeReadingNote.xml`. Child edits remain separate from the root notes file and root GitHub sync payload.

如果磁盘文件在本地编辑期间变化，插件会暂停覆盖并提示：

- **重新加载磁盘版本 / Reload Disk Version**：放弃当前待保存的本地版本，采用磁盘数据。
- **备份磁盘版本并保存本地修改 / Back Up Disk and Save Local Changes**：先在笔记文件旁创建唯一名称的 `.bak` 文件，保留完整磁盘版本，再保存本地修改。

没有本地修改时自动重载。XML 损坏或权限不足时显示项目路径，读取失败不会被当成空数据写回。关闭窗口会处理待保存任务；未解决或保存失败的本地内容会保存在 IDEA 配置目录的 `CodeReadingNote/workspace-recovery/`，下次打开该工作空间时提供冲突恢复。不要在确认恢复前手动清除此目录。

Unmodified data reloads automatically. Read errors preserve the existing data. On close, pending writes are flushed; unresolved or failed writes are retained under `CodeReadingNote/workspace-recovery/` in the IDEA configuration directory and offered for recovery when the workspace is reopened.

同一 IDEA 同时独立打开子项目时，两窗口共享已有笔记数据服务和笔记同步队列。AI 工作台及 IDEA 原生书签搜索仍按当前独立打开的 IDEA 项目工作，本功能不导入子项目独立窗口中的原生书签。

If a child project is also open in the same IDE, both windows share its notes service and notes sync queue. The AI workspace and native IDEA bookmark search retain their IDEA-project scope.

## 按项目同步（3.7.7）/ Per-project sync

先在插件设置中启用同步，配置 GitHub 仓库、分支和 Token。右键项目节点选择“推送 / 拉取 / 检查更新 / 同步设置”。工具栏推拉跟随当前选中的项目、主题或笔记；无项目选择时打开“工作空间笔记同步”面板。面板可勾选多个项目批量操作，失败项可单独重试；先勾选一个项目再点“处理所选项目差异”查看冲突。

Enable sync and configure the GitHub repository, branch and token in Settings. Use a project's context menu or the selection-aware toolbar to push, pull or check updates. The **Workspace Notes Sync** overview supports checked project batches, per-project status, failed-item retries and conflict review.

首次操作需要绑定远端目录，可从列表选择已有项目或输入新目录名。默认建议原项目名；同名项目应绑定不同目录，只有确认是同一逻辑项目的本地副本才允许共用。绑定保存在各项目 `.idea/notesSyncBinding.xml`，不含 Token，父目录改名不会改变远端目录。新设备没有本地基线时仍需首次对齐。根项目旧“自动同步”选项现在作为首次绑定默认自动推送的偏好，各项目实际策略在同步面板管理。

Bind each project to an existing or new remote directory before its first sync. The portable `.idea/notesSyncBinding.xml` contains no token. Identically named projects need separate remote directories unless they intentionally represent the same project. A new device still needs initial alignment. The old automatic-sync checkbox supplies the default policy for a new root binding; manage existing project policies in the overview.

| 策略 / Policy | 行为 / Behavior |
|---|---|
| 仅手动 / Manual only | 只执行用户指定的操作 / Run requested operations only |
| 自动推送 / Auto push | 本地修改防抖 3 秒后，核对远端并保存成功再推送；远端变化需手动处理 / Debounce edits for 3 seconds, verify remote and persist before pushing; remote changes await manual action |
| 安全双向 / Safe bidirectional | 本地单变推送，远端单变拉取，双方不同变化暂停该项目 / Push local-only changes, pull remote-only changes, pause divergent edits |

新发现项目默认仅手动；可勾选多个项目后点“自动策略”批量启用。首次未对齐的项目不会自动覆盖任何一侧。自动项目在打开工作空间后检查，并默认每 5 分钟检查远端（每项目可配置 1–60 分钟），切回 IDEA 时也会节流检查；无需展开项目树或编辑子项目。关闭全部包含该项目的窗口后停止后台同步，下次打开恢复检查。离线自动退避，鉴权/冲突等暂停可修复后手动检查或重新保存同步设置。

New projects default to manual. Select several projects and use **Automatic policy** to enable automation. Unaligned projects never overwrite either side automatically. Automatic projects are checked after workspace opening, every 5 minutes by default (1–60 minutes per project), and on throttled IDE activation. Closing all relevant windows stops background sync. Offline requests back off; repair authentication or conflict issues and check manually or save the project's sync settings to resume.

“备份并采用本地 / 远端”会再次核对差异是否变化；如果期间继续编辑，会要求重新检查。恢复备份位于 IDEA 配置目录 `CodeReadingNote/notes-sync/backups/`，同步基线和未完成应用日志位于 `CodeReadingNote/notes-sync/`，这些文件不会推到 GitHub。远端文件缺失或损坏不会当作空集合清空本地；已确认同步后的合法空集合可以传播清空操作。旧载荷未包含回收站时保留本地回收站。

Replacing either version rechecks the reviewed data and saves recovery backups under the IDEA configuration directory's `CodeReadingNote/notes-sync/backups/`. Baselines and recovery journals are local-only. Missing or corrupt remote files never clear local notes. Valid empty collections can propagate intentional removal; legacy payloads without trash preserve existing local trash.

## 已有远端文件推送失败 / Updating an existing remote file

3.7.7 修复了 issue #15 涉及的分支/SHA 读取及错误提示问题。远端 `CodeReadingNote.xml` 已存在时可直接更新，不需要删除再推送。若提示缺少 SHA，请确认配置分支确实存在，并检查 Token 对该仓库的读取和写入权限后重新检查；并发冲突先对比并处理差异。其他 422 错误请检查文件路径、分支及仓库规则。

Version 3.7.7 fixes branch/SHA handling and error reporting related to issue #15. Existing remote notes can be updated directly. If a file version (SHA) cannot be read, check the configured branch and token read/write permissions before checking again. Review concurrent changes instead of deleting the remote file. Other 422 errors require checking the path, branch and repository rules.
