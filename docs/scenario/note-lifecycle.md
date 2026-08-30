# 场景：笔记生命周期

从零开始使用笔记功能的端到端走查（对应旧架构文档的 Core Use Cases 前两条）。

## 创建主题

```
用户 -> ToolWindow「创建」按钮 -> NewTopicDialog
     -> TopicList.addTopic() -> MessageBus(TopicListNotifier) -> UI 树刷新
```

主题是第一级组织单位，后续所有笔记都挂在某个主题下。

## 给代码行加笔记

```
用户 -> 编辑器光标定位 -> AddToTopic 动作 -> AddTopicLineDialog
     -> Topic.addTopicLine() -> 持久化 -> gutter 出现标记图标
```

- `TopicLine` 记录 `url`（文件）+ `line`（行号）+ `description`（备注）
- 之后光标行的 gutter 会渲染 `NoteGutterIconRenderer` 自定义图标
- 点击图标弹出 `NotePopupHelper` 交互弹窗，可就地查看/编辑备注，
  编辑走 `TopicNotifier.lineNoteChanged` 通知刷新

## 导航回代码

`NavigateToNoteAction`（或从树/搜索面板点击笔记）按 `url + line` 跳回源码位置，
实现「读笔记 -> 回现场」的往返。

## 分组与回收站

- 同主题内的行可用 `Group` 按标签分组、自定义命名，便于长主题下的二级整理
- 删除的行包成 `TrashedLine` 进回收站，可恢复，避免误删不可逆

## 行号漂移问题

笔记绑定的是行号，代码变更后行号会漂移。实验性的「mark 位置修复」与 IntelliJ
workspace 恢复行为相关（建议取消勾选 restore workspace，详见
`integration/intellij-platform.md` 的 bookmark 调研一节）。
