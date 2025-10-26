# 调试右键菜单问题 - 测试指南

## 🔍 调试信息说明

我已经在代码中添加了调试日志，运行时会在 IDEA 控制台输出。

---

## 📋 测试步骤

### 测试 1: 树视图中右键 Topic

**操作**:
1. 在**左侧树视图**中找到一个 Topic 节点（如 "test"）
2. **右键点击 Topic 节点**
3. 观察控制台输出

**期望的控制台输出**:
```
[DEBUG] Added FixTopicRemarkAction for topic: test
[DEBUG] Action count: 1
[DEBUG] Popup menu shown
```

**期望看到的菜单**:
- Fix Topic Offset (N 行错位)  ← 如果有错位的行
- Fix Topic Offset (检测失败)  ← 如果检测出错
- 或者菜单不显示 ← 如果没有错位的行

---

### 测试 2: 树视图中右键 TopicLine

**操作**:
1. 在**左侧树视图**中展开 Topic
2. **右键点击具体的 TopicLine**（如 "T.java:29"）
3. 观察控制台输出

**期望的控制台输出**:
```
[DEBUG] Added FixLineRemarkAction for line: 29
[DEBUG] Action count: 1
[DEBUG] Popup menu shown
```

**期望看到的菜单**:
- Fix Line Offset (行号描述)  ← 如果该行错位
- Fix Line Offset (检测失败)  ← 如果检测出错
- 或者菜单不显示 ← 如果该行已同步

---

### 测试 3: 详情面板中右键 TopicLine

**操作**:
1. 在**右侧详情面板**中
2. **右键点击 TopicLine**
3. 观察控制台输出

**期望的控制台输出**:
```
[DEBUG DetailPanel] Creating context menu for TopicLine: 29
[DEBUG DetailPanel] Added Remove action
[DEBUG DetailPanel] Added ShowBookmarkUid action
[DEBUG DetailPanel] Added FixLineRemark action
[DEBUG DetailPanel] Added MoveTo action
[DEBUG DetailPanel] Total actions: 4
[DEBUG DetailPanel] Popup shown
```

**期望看到的菜单** (按顺序):
1. Remove TopicLine
2. Show BookmarkUid
3. Fix Line Offset (可能被隐藏/禁用)
4. Move to

---

## 🐛 问题诊断

### 问题 1: 树视图右键 Topic 没有菜单

**可能的原因**:
- Console 没有输出 → `showContextMenu` 没有被调用
- Console 输出 "Action count: 0" → Action 被过滤掉了
- Console 输出错误信息 → 检测逻辑出错

**解决方法**:
1. 检查 Console 输出的具体内容
2. 如果没有输出，说明鼠标事件没有触发
3. 如果有错误，看具体的异常堆栈

---

### 问题 2: 详情面板只显示前两个菜单项

**可能的原因**:
- Console 显示 "Total actions: 4" 但菜单只显示 2 项 → `FixLineRemarkAction.update()` 返回 `setEnabledAndVisible(false)`
- Console 显示 "Total actions: 2" → `FixLineRemarkAction` 和 `TopicLineMoveToGroupAction` 添加失败

**解决方法**:
1. 查看 Console 是否显示 "Added FixLineRemark action"
2. 如果显示了，说明 Action 被添加了，但被 `update()` 方法隐藏了
3. 这是正常行为：**如果该行没有错位，"Fix Line Offset" 不显示**

---

### 问题 3: 所有菜单都不显示

**可能的原因**:
- DataContext 创建失败
- JBPopupFactory 抛出异常
- 编译版本问题

**解决方法**:
1. 查看 Console 是否有错误堆栈
2. 确保已经重新编译并安装插件
3. 重启 IDEA

---

## ✅ 验证方法

### 如何制造"错位"的 TopicLine

1. **添加一个 TopicLine**:
   - 打开文件 `T.java`
   - 选中第 29 行
   - 右键 → "Add to Topic" → 选择或创建 Topic

2. **修改代码制造错位**:
   - 在第 29 行**之前**插入几行空行
   - 现在 Bookmark 会自动移动到新位置（如第 32 行）
   - 但 TopicLine 记录的还是第 29 行

3. **测试修复功能**:
   - 右键该 TopicLine
   - 应该看到 "Fix Line Offset (29 → 32 (+3))"
   - 点击后，TopicLine 应该更新到第 32 行

---

## 📊 控制台输出示例

### 正常情况（有错位）

```
[DEBUG DetailPanel] Creating context menu for TopicLine: 29
[DEBUG DetailPanel] Added Remove action
[DEBUG DetailPanel] Added ShowBookmarkUid action
[DEBUG DetailPanel] Added FixLineRemark action
[DEBUG DetailPanel] Added MoveTo action
[DEBUG DetailPanel] Total actions: 4
[DEBUG DetailPanel] Popup shown
```

此时菜单应该显示 4 项（如果该行错位）。

---

### 正常情况（无错位）

```
[DEBUG DetailPanel] Creating context menu for TopicLine: 29
[DEBUG DetailPanel] Added Remove action
[DEBUG DetailPanel] Added ShowBookmarkUid action
[DEBUG DetailPanel] Added FixLineRemark action
[DEBUG DetailPanel] Added MoveTo action
[DEBUG DetailPanel] Total actions: 4
[DEBUG DetailPanel] Popup shown
```

虽然 Console 显示添加了 4 个 Action，但菜单可能只显示 3 项（"Fix Line Offset" 被隐藏），**这是正常的**。

---

### 异常情况

```
[DEBUG DetailPanel] Creating context menu for TopicLine: 29
[DEBUG DetailPanel] Added Remove action
[DEBUG DetailPanel] Added ShowBookmarkUid action
[ERROR DetailPanel] Failed to show context menu: NullPointerException
java.lang.NullPointerException: ...
    at ...
```

此时需要根据堆栈信息定位问题。

---

## 🔧 下一步行动

**请运行插件并执行以下操作**:

1. ✅ 右键点击树视图中的 Topic
2. ✅ 右键点击树视图中的 TopicLine
3. ✅ 右键点击详情面板中的 TopicLine
4. ✅ 复制控制台输出并告诉我

**我需要知道**:
- 控制台输出了什么？
- 菜单显示了什么？
- 是否有异常堆栈？

---

## 💡 临时解决方案

如果调试后发现 `update()` 方法导致菜单项被隐藏，我可以：

1. **方案 A**: 修改 `update()` 逻辑，让菜单项始终显示，只是禁用（灰色）
2. **方案 B**: 移除 `update()` 方法，让所有菜单项始终可见
3. **方案 C**: 在 `update()` 中添加更多容错处理

请先运行测试，根据实际输出决定采用哪个方案。

