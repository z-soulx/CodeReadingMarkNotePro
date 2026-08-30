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

`CodeReadingNoteService` 是 `PersistentStateComponent`，整个 `TopicList` 随 IDE 自动
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
