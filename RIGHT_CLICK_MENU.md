# 右键菜单功能说明

## ✅ 已添加的右键菜单

### 1. TopicLine 右键菜单

**位置**: 
- TopicTreePanel 树视图中的 TopicLine 节点
- TopicDetailPanel 列表中的 TopicLine 项

**菜单项**:
- **Fix Line Offset** - 修复该行的行号错位
  - 只在该行确实错位时启用
  - 显示错位信息（如 "128 → 134 (+6)"）
  - 修复后显示结果对话框

### 2. Topic 右键菜单

**位置**: TopicTreePanel 树视图中的 Topic 节点

**菜单项**:
- **Fix Topic Offset** - 修复该 Topic 下所有行的错位
  - 显示错位行数统计（如 "Fix Topic Offset (5 行错位)"）
  - 批量修复所有错位的 TopicLine
  - 修复后显示结果对话框

### 3. 工具栏（已有）

**位置**: ManagementPanel 工具栏

**按钮**:
- **Fix All** - 修复所有 TopicLine 的错位
  - 全局批量修复
  - 显示详细的修复统计

---

## 🎯 使用示例

### 场景 1: 发现某行代码标记错位了

1. 在树视图中找到该 TopicLine
2. 右键点击 → 选择 "Fix Line Offset"
3. 查看修复结果对话框（成功修复 1/1）

### 场景 2: 切换分支后发现某个 Topic 下很多行都错位了

1. 右键点击 Topic 节点
2. 选择 "Fix Topic Offset (15 行错位)"
3. 系统批量修复该 Topic 下所有错位的行
4. 查看修复结果（成功修复 15/15）

### 场景 3: Pull 代码后想修复所有错位

1. 点击工具栏的 "Fix All" 按钮
2. 系统检测并修复所有 TopicLine
3. 查看修复结果（成功修复 42/50，失败 8）

---

## 🔧 技术实现

### TopicTreePanel 右键菜单

**实现位置**: `TopicTreePanel.java` 第 98-123 行

```java
// 右键点击监听器
topicTree.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        // ... 双击导航逻辑 ...
        
        // 右键菜单
        if (SwingUtilities.isRightMouseButton(e)) {
            topicTree.setSelectionPath(path); // 先选中节点
            showContextMenu(e, node);
        }
    }
});
```

**菜单创建逻辑**: `showContextMenu()` 方法（第 601-649 行）

```java
private void showContextMenu(MouseEvent e, TopicTreeNode node) {
    DefaultActionGroup actions = new DefaultActionGroup();
    
    switch (node.getNodeType()) {
        case TOPIC:
            actions.add(new FixTopicRemarkAction(project, (v) -> topic));
            break;
        case TOPIC_LINE:
            actions.add(new FixLineRemarkAction(project, (v) -> new Pair<>(parentTopic, line)));
            break;
        // ...
    }
    
    // 显示弹出菜单
    JBPopupFactory.getInstance().createActionGroupPopup(...).show(new RelativePoint(e));
}
```

### TopicDetailPanel 右键菜单

**实现位置**: `TopicDetailPanel.java` 第 169-183 行

```java
if (SwingUtilities.isRightMouseButton(e)) {
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(new TopicLineRemoveAction(...));
    actions.add(new ShowBookmarkUidAction(...));
    actions.add(new FixLineRemarkAction(project, (v) -> new Pair<>(topic, topicLine)));
    actions.add(new TopicLineMoveToGroupAction(topicLine));
    
    JBPopupFactory.getInstance().createActionGroupPopup(...).show(new RelativePoint(e));
}
```

---

## 📝 修改的文件

1. **TopicTreePanel.java**
   - 添加 `showContextMenu()` 方法
   - 修改 MouseListener 支持右键菜单
   - 添加必要的 import

2. **TopicDetailPanel.java**  
   - 已有 TopicLine 右键菜单
   - 添加了 `FixLineRemarkAction`

3. **FixLineRemarkAction.java**
   - 增强：智能检测、动态文本、结果对话框

4. **FixTopicRemarkAction.java**
   - 增强：统计错位数、动态文本、结果对话框

---

## ✨ 用户体验优化

### 智能菜单项
- **动态启用/禁用**: "Fix Line Offset" 只在真的错位时才启用
- **显示详细信息**: 菜单项文本显示错位详情（如 "+6 行"）
- **结果反馈**: 修复后立即显示结果对话框

### 操作流畅
- **右键选中**: 右键点击自动选中节点
- **双击导航**: 双击 TopicLine 直接跳转代码
- **键盘支持**: 回车键导航，空格键展开/折叠

---

## 🎨 未来可添加的菜单项

### TopicLine 右键菜单
- ⏳ Navigate to New Position (跳转到 Bookmark 的新位置)
- ⏳ Re-create Bookmark (Bookmark 丢失时重建)
- ⏳ Show Diff (显示行号变化)
- ⏳ Copy File Path (复制文件路径)
- ⏳ Open in Editor (在编辑器中打开)

### Topic 右键菜单
- ⏳ Detect All Offset (只检测不修复)
- ⏳ Export Topic (导出该 Topic)
- ⏳ Rename Topic (重命名)
- ⏳ Remove Topic (删除)
- ⏳ Duplicate Topic (复制)

### Group 右键菜单
- ⏳ Fix Group Offset (修复分组内所有行)
- ⏳ Rename Group (重命名分组)
- ⏳ Remove Group (删除分组)

---

**文档最后更新**: 2025-10-25
**版本**: v3.3.0

