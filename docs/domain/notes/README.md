# 笔记领域（Notes）

代码阅读笔记的核心模型。一个笔记 = 「某个主题下、对某文件某行的一段备注」。

## 领域模型

```
CodeReadingNoteService（项目级单例）
└── TopicList
    └── Topic（name, datetime）        一个阅读主题，如"Spring 循环依赖源码"
        └── TopicLine（url, line, description）   被标注的代码行 + 备注文本
            └── Group（tag-based grouping, 自定义命名）   行的标签分组
```

- **Topic**：阅读主题，时间戳记录创建时间
- **TopicLine**：一条笔记。`url` 定位到文件、`line` 定位到行、`description` 是备注
- **Group**：同一主题内按标签组织行，分组名可自定义
- **TrashedLine**：删除不直接丢弃，包一层进回收站，可恢复

回收站的存在让「删错笔记」不再是不可逆操作，也避免远端同步时误判为删除扩散。

## 状态与持久化

### 工作空间多项目（3.7.7）

`notesworkspace.WorkspaceNotesService` 在父目录下递归发现含 `.idea` 的项目，聚合各自的
`TopicList`。后台扫描排除 `.git`、`.idea` 内部、`node_modules`、`build`、`target`、`out`、
`.gradle`，不跟随符号链接或 junction。通过 VFS 监听和防抖刷新发现动态变化；显式刷新入口
位于笔记工具栏。空项目不会仅因扫描产生笔记文件。

`NoteProjectContext` 是不写入 XML 的运行时归属，以规范化绝对路径标识项目。TopicList、
Topic、分组中的行和回收站共享归属；TopicLine 相对路径解析以所属项目根为基准。
导航显式传入当前窗口 Project。缺失文件的笔记保留原相对路径，禁止回退到另一台机器留下的
绝对 URL。主题树增加项目层，同名目录用相对路径区分，搜索结果标明项目来源。

`WorkspaceNotesCoordinator` 是应用级协调者，同一路径只维护一份 TopicList。子项目同时
独立打开时，它的 CodeReadingNoteService 接管同一集合，暂停独立文件写入；关闭时将数据
上下文转回仍打开的工作空间。不会为子目录创建 IDEA Project。

根项目继续通过 PersistentStateComponent 保存。根 getTopicList/getState 与 GitHub 同步
始终只处理根数据；子项目通过 WorkspaceXmlStore 保存。存储保留组件、导入导出目录、
未知扩展属性及元素，沿用旧主题/分组/行/回收站 XML 结构和 UID。

子项目修改在 EDT 捕获快照，应用级后台串行队列防抖 500ms 后保存。保存比对原始磁盘字节，
写临时文件、force 后原子替换；不支持原子替换时报告错误，不退化为直接覆盖。外部变化与
本地修改冲突时暂停，支持重新加载磁盘或备份后保存本地版本；重载与编辑之间用修订号检查
防止覆盖新编辑。关闭窗口完成待保存任务，无法写入的内容保存在 IDEA 配置目录
`CodeReadingNote/workspace-recovery/`，用根路径派生的标识区分恢复文件。

列表加载、排序与回收站通知携带 TopicList；运行时归属变更驱动工作空间刷新与子项目保存。
根自动同步只在根模型实际修改后触发。行标记使用带项目归属的运行时标识，持久化 UID 保持
不变；工作空间原生关联书签使用独立归属分组匹配，同 UID 仍需匹配文件 URL。

导入/导出/新建主题使用选中项目，无选择时弹出项目选择。编辑器新增按最近项目根路由。
移动只允许同一项目，保留原笔记对象、UID、未知 XML 和缺失文件路径；不生成伪删除回收站
条目。回收站恢复在原主题不存在时重建主题，并重新绑定行的主题引用。

3.7.7 支持子项目 GitHub 笔记同步，项目选择、内容基线及自动策略见 `../sync/README.md`。AI 工作台扩展、独立原生书签导入和跨项目移动不在当前范围。

`CodeReadingNoteService` 是 `PersistentStateComponent`，根项目的 `TopicList` 随 IDE 自动
存入 `CodeReadingNote.xml`（项目级），并作为同步通道之一的载荷（见 `domain/sync/`）。

## 界面形态

- **ToolWindow**：`ManagementPanel` 聚合三个页签 -- 主题树（`TopicDetailPanel`）、
  搜索、AI 工作台（`AIWorkspacePanel`）
- **Gutter 标记**：被标注的行左侧显示自定义图标（`NoteGutterIconRenderer`），
  点击弹出 `NotePopupHelper` 交互弹窗，可直接编辑备注
- **入口动作**：编辑器光标处 `AddToTopic` 弹 `AddTopicLineDialog` 加入主题；
  `NavigateToNoteAction` 从笔记跳回代码位置

## 关键流程

建主题 -> 加行 -> 导航 ->（可选）分组整理 -> 删除进回收站，
端到端走查见 `scenario/note-lifecycle.md`。

## 已知方向（未实施）

根 README 的 TODO 中列有「topic 分层」构想，尚未落地，此处仅作备忘。
