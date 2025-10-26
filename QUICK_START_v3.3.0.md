# v3.3.0 快速使用指南

## 🎯 如何使用修复功能

### 📍 使用场景说明

根据您的截图，您正在查看：
- **左侧**: Tree View（树视图）- 显示 Topic 和 TopicLine 的层级结构
- **右侧**: Detail Panel（详情面板）- 显示选中 Topic 的 TopicLine 列表

---

## ✅ 可用的修复方式

### 方式 1: 树视图中右键 Topic（推荐用于批量修复）

**步骤**:
1. 在**左侧树视图**中找到 Topic 节点（如 "test" 或 "SpringMVC源码学习"）
2. **右键点击 Topic 节点本身**（不是展开后的 TopicLine）
3. 选择菜单项 "**Fix Topic Offset**"
4. 查看修复结果对话框

**适用场景**: 切换分支后，整个 Topic 下很多行都错位了

---

### 方式 2: 树视图中右键 TopicLine（单行修复）

**步骤**:
1. 在**左侧树视图**中展开 Topic
2. **右键点击具体的 TopicLine**（如 "T.java:29"）
3. 选择菜单项 "**Fix Line Offset**"
4. 查看修复结果

**适用场景**: 发现某一行错位了

---

### 方式 3: 详情面板中右键 TopicLine（单行修复）

**步骤**:
1. 在**右侧详情面板**的列表中
2. **右键点击 TopicLine**（就像您截图中那样）
3. 选择菜单项 "**Fix Line Offset**"（在菜单中应该能看到）
4. 查看修复结果

**注意**: 根据您的截图，菜单显示的是：
- Remove TopicLine
- Show BookmarkUid
- **Fix Line Offset** ← 应该在这里
- Move to

---

### 方式 4: 工具栏全局修复（最快）

**步骤**:
1. 点击工具栏的 "**Fix All**" 按钮（黄色警告图标旁边）
2. 等待修复完成
3. 查看结果对话框：显示修复了多少行

**适用场景**: Pull 代码后，想一次性修复所有错位

---

## 🔍 为什么菜单中可能看不到某些选项？

### 智能菜单机制

修复菜单项是**智能启用**的：

1. **Fix Line Offset** 只在该行**真的错位**时才显示/启用
   - ✅ 如果 TopicLine 和 Bookmark 行号不一致 → 菜单项启用
   - ❌ 如果已经同步 → 菜单项禁用或不显示

2. **Fix Topic Offset** 同理
   - 只在 Topic 下有错位的行时才启用
   - 菜单文本会显示错位数量（如 "Fix Topic Offset (5 行错位)"）

### 如何确认是否错位？

使用 "Show BookmarkUid" 查看：
1. 右键 TopicLine → "Show BookmarkUid"
2. 会显示该行的 Bookmark 信息
3. 对比 TopicLine 显示的行号和实际代码位置

---

## 🐛 常见问题排查

### 问题 1: 右键菜单中没有 "Fix Line Offset"

**可能原因**:
1. 该行没有错位（已经同步）
2. 该行没有 bookmarkUid（老数据）
3. Bookmark 已丢失

**解决方法**:
- 先尝试 "Fix All" 全局修复
- 如果还是不行，重新 "Add to Topic"

---

### 问题 2: 修复后还是错位

**可能原因**:
1. Bookmark 本身就不准确
2. 文件被大幅修改
3. 缓存问题

**解决方法**:
1. 关闭并重新打开文件
2. 再次点击 "Fix Line Offset"
3. 查看 Bookmark 是否还存在（Show BookmarkUid）

---

### 问题 3: 树视图中右键 Topic 没有菜单

**可能原因**:
- 可能点击的是折叠的 Topic 旁边的空白区域

**解决方法**:
- 确保右键直接点击 Topic 的文本部分
- 或者直接使用 "Fix All" 按钮

---

## 📊 修复结果示例

### 成功修复
```
修复完成
━━━━━━━━━━━━━━━━━
成功修复: 15 个
失败: 0 个
耗时: 0.3 秒
```

### 无需修复
```
无需修复
━━━━━━━━━━━━━━━━━
该 Topic 下所有行已同步，无需修复
```

### 部分失败
```
修复完成
━━━━━━━━━━━━━━━━━
成功修复: 12 个
失败: 3 个

错误信息:
1. UserService.java:45 - Bookmark 丢失
2. AuthController.java:128 - 文件不存在
3. TokenManager.java:67 - 行号超出范围
```

---

## 🎨 界面说明

### 树视图（左侧）
```
📁 Topic 1                    ← 右键这里：Fix Topic Offset
  ├─ 📄 File1.java:10         ← 右键这里：Fix Line Offset
  ├─ 📄 File2.java:25
  └─ 📄 File3.java:42

📁 Topic 2
  ├─ 📂 Group A
  │   ├─ 📄 File4.java:15
  │   └─ 📄 File5.java:30
  └─ 📂 Ungrouped Lines
      └─ 📄 File6.java:50
```

### 详情面板（右侧）
```
Topic: test
━━━━━━━━━━━━━━━━━━━━━━━━

📄 T.java:29                  ← 右键这里：Fix Line Offset
   test content...

📄 Another.java:45
   another content...
```

---

## 💡 最佳实践

### 场景 1: 日常使用
- 发现某行错位 → 右键该行 → Fix Line Offset

### 场景 2: 切换分支后
- 工具栏 → Fix All → 一次性修复所有错位

### 场景 3: Pull 代码后
- 右键特定 Topic → Fix Topic Offset → 只修复相关的 Topic

### 场景 4: 错位很少时
- 手动右键逐个修复，更有控制感

---

## 📝 技巧提示

### 提示 1: 批量操作
如果有多个 Topic 都错位了，最快的方式是：
```
工具栏 Fix All > 右键 Topic > 右键 TopicLine
```

### 提示 2: 查看修复详情
修复后的对话框会显示：
- 修复了多少行
- 失败了多少行
- 具体的错误信息
- 耗时

### 提示 3: 预防错位
每次切换分支或 Pull 代码后，养成习惯：
1. 点击 "Fix All"
2. 查看修复结果
3. 继续工作

---

## 🔧 调试信息

如果遇到问题，可以查看：

1. **IDEA 日志**: `Help > Show Log in Explorer/Finder`
2. **插件日志**: 搜索 "CodeReadingNote" 或 "AutoFix"
3. **Bookmark 信息**: 右键 → "Show BookmarkUid"

---

## ❓ 还有问题？

如果修复功能不起作用，请检查：

1. ✅ 是否已经升级到 v3.3.0
2. ✅ 是否有编译错误（查看 IDEA 底部的 Problems 面板）
3. ✅ TopicLine 是否有 bookmarkUid（右键 Show BookmarkUid 查看）
4. ✅ Bookmark 是否还存在（Tools > Bookmarks）

---

**文档版本**: v3.3.0  
**最后更新**: 2025-10-25

