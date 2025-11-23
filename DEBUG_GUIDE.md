# 调试指南 - TreeView 拖拽功能

## 问题诊断步骤

### 1. 检查拖拽是否被触发

**操作步骤:**
1. 打开 IntelliJ IDEA
2. 打开 Help → Diagnostic Tools → Debug Log Settings
3. 添加以下日志类别：
   ```
   #jp.kitabatakep.intellij.plugins.codereadingnote.ui.dnd.TopicTreeTransferHandler
   #jp.kitabatakep.intellij.plugins.codereadingnote.operations.TopicLineOperationService
   ```
4. 点击 OK
5. 尝试拖拽一个 TopicLine 到一个 Group

**预期日志输出:**
```
TopicTreeTransferHandler: Creating transferable for N TopicLine(s)
TopicTreeTransferHandler: Dropping N line(s) onto [GroupName]
TopicTreeTransferHandler: About to call moveBetweenGroups with target topic: [TopicName], target group: [GroupName]
TopicLineOperationService: Moving N lines to group '[GroupName]'
TopicLineOperationService: Removed line from source group: [SourceGroup]
TopicLineOperationService: Added line to target group: [TargetGroup]
TopicLineOperationService: Successfully moved N lines to [GroupName]
TopicTreeTransferHandler: Successfully moved N line(s) to group: [GroupName]
TopicTreeTransferHandler: Tree model reloaded
TopicTreeTransferHandler: Export done with action: 2
```

### 2. 常见问题排查

#### 问题A: 没有任何日志输出
**可能原因:** 拖拽没有被启用
**检查:**
- 确认 `TopicTreePanel.initTree()` 中有以下代码：
  ```java
  topicTree.setDragEnabled(true);
  topicTree.setDropMode(DropMode.ON);
  topicTree.setTransferHandler(new TopicTreeTransferHandler(project, topicTree));
  ```

#### 问题B: 有 "Creating transferable" 但没有 "Dropping"
**可能原因:** Drop 验证失败
**检查:**
- 确保拖拽到 Group 节点或 "Ungrouped Lines" 文件夹
- 不能拖拽到 Topic 节点或其他 TopicLine 节点

#### 问题C: 有 "Dropping" 但 "target topic: null"
**可能原因:** `TopicTreeNode.getTopic()` 返回 null
**解决方案:** 已修复，现在 `UNGROUPED_LINES_FOLDER` 类型会从父节点获取 Topic

#### 问题D: 操作成功但 UI 没有更新
**可能原因:** TreeView 没有刷新
**解决方案:** 已添加 `model.reload()` 强制刷新

### 3. 验证数据一致性

**在拖拽操作后，检查以下内容:**

#### 3.1 检查 Topic 的 groups 列表
```java
Topic topic = ...;
System.out.println("Topic groups: " + topic.getGroups().size());
for (TopicGroup group : topic.getGroups()) {
    System.out.println("  - " + group.name() + ": " + group.getLines().size() + " lines");
}
```

#### 3.2 检查 Topic 的 ungroupedLines 列表
```java
System.out.println("Ungrouped lines: " + topic.getUngroupedLines().size());
```

#### 3.3 检查 TopicLine 的 group 引用
```java
TopicLine line = ...;
TopicGroup group = line.getGroup();
System.out.println("Line group: " + (group != null ? group.name() : "null/ungrouped"));
```

#### 3.4 检查 Group 的 lines 列表
```java
TopicGroup group = ...;
System.out.println("Group lines: " + group.getLines().size());
for (TopicLine line : group.getLines()) {
    System.out.println("  - " + line.pathForDisplay() + ":" + line.line());
}
```

### 4. 手动测试步骤

#### 测试用例1: 从 Ungrouped 拖拽到 Group
1. 创建一个 Topic，例如 "TestTopic"
2. 添加一个 Group，例如 "Group1"
3. 添加一个 TopicLine（默认在 Ungrouped）
4. 在 TreeView 中，将 TopicLine 拖拽到 "Group1"
5. **预期结果:**
   - TopicLine 从 "Ungrouped Lines" 消失
   - TopicLine 出现在 "Group1" 下
   - 数据持久化（重启 IDE 后仍在 Group1）

#### 测试用例2: 从 Group 拖拽到另一个 Group
1. 在同一个 Topic 下创建两个 Groups: "Group1" 和 "Group2"
2. 在 "Group1" 下添加一个 TopicLine
3. 将 TopicLine 从 "Group1" 拖拽到 "Group2"
4. **预期结果:**
   - TopicLine 从 "Group1" 消失
   - TopicLine 出现在 "Group2" 下
   - "Group1" 的行数减少，"Group2" 的行数增加

#### 测试用例3: 从 Group 拖拽到 Ungrouped Lines
1. 在 "Group1" 下有一个 TopicLine
2. 将 TopicLine 拖拽到 "Ungrouped Lines" 文件夹
3. **预期结果:**
   - TopicLine 从 "Group1" 消失
   - TopicLine 出现在 "Ungrouped Lines" 下
   - TopicLine 的 `getGroup()` 返回 null

#### 测试用例4: 批量拖拽（多选）
1. 在 TreeView 中按住 Ctrl 选择多个 TopicLine
2. 拖拽到目标 Group
3. **预期结果:**
   - 所有选中的 TopicLine 都移动到目标 Group
   - UI 正确更新

### 5. 已知限制

1. **跨 Topic 拖拽暂不支持**: 只能在同一个 Topic 内拖拽移动 TopicLine
2. **只支持拖拽 TopicLine 节点**: 不能拖拽 Group 或 Topic 节点
3. **Drop 目标限制**: 只能拖放到 Group 节点或 "Ungrouped Lines" 文件夹

### 6. 如果问题仍然存在

**请提供以下信息:**

1. **完整的日志输出** (从 idea.log 文件)
2. **操作步骤** (详细描述你做了什么)
3. **预期行为** vs **实际行为**
4. **TreeView 结构截图** (操作前后)
5. **数据检查结果** (按照第3节的方法)

### 7. 临时解决方案

如果拖拽功能仍然有问题，可以使用替代方案：

**方法1: 使用右键菜单**
- 右键点击 TopicLine → "Move to Group" → 选择目标 Group

**方法2: 使用详细面板的拖拽**
- 在右侧的详细面板（TopicDetailPanel）中使用拖拽功能
- 该面板的拖拽功能是独立实现的，更稳定

---

## 代码修复清单

### 已完成的修复:

- ✅ `TopicTreeNode.getTopic()` - 支持从 UNGROUPED_LINES_FOLDER 获取 Topic
- ✅ `TopicLineOperationService.moveBetweenGroups()` - 正确维护所有列表
- ✅ `TopicTreeTransferHandler` - 添加详细日志和强制刷新
- ✅ `TopicTreePanel.initTree()` - 启用拖拽和多选

### 数据流:

```
用户拖拽 TopicLine
    ↓
TopicTreeTransferHandler.createTransferable()
    → 创建 TopicLineTransferable
    ↓
TopicTreeTransferHandler.canImport()
    → 验证是否可以 drop
    ↓
TopicTreeTransferHandler.importData()
    → 获取目标 Topic 和 Group
    → 调用 TopicLineOperationService.moveBetweenGroups()
        ↓
        moveBetweenGroups():
            1. 从源位置移除 (sourceGroup.lines 或 topic.ungroupedLines)
            2. 添加到目标位置 (targetGroup.lines 或 topic.ungroupedLines)
            3. 更新 line.group 引用
            4. topic.touch() (触发持久化)
            5. notifyGroupChanged() (触发 UI 更新)
    → 强制 TreeView 刷新 (model.reload())
    ↓
UI 更新完成
```

---

**如果按照本指南仍无法解决问题，请提供详细的日志和截图！**


