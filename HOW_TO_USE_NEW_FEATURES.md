# 如何使用新功能

## 🎯 问题："为什么我运行后没有看到那些功能？"

### ✅ 已修复的问题

1. ✅ **plugin.xml 注册** - 已添加 RepairBookmarksAction
2. ✅ **UI 集成** - TopicDetailPanel 已增强
3. ✅ **创建缺失文件** - RepairTopicBookmarksAction.java 已创建
4. ✅ **编译错误** - 仅剩无害的警告

## 📦 如何看到新功能

### 1. 重新构建插件

```bash
# Windows PowerShell
cd D:\worker\pg\CodeReadingNote
.\gradlew clean buildPlugin

# 或者在 IntelliJ IDEA 中
# Build > Build Project
```

### 2. 重启 IDE

**重要！** 即使使用 `runIde`，也需要重启测试 IDE 实例才能看到：
- 新的 Actions
- 新的工具栏按钮
- 新的右键菜单项

### 3. 查看新功能的位置

#### 🔧 Bookmark 修复功能

**位置 1: Tools 菜单**
```
Tools > Repair Code Reading Bookmarks
```
- 扫描所有 Topics 的 bookmarks
- 修复缺失的 UUID

**位置 2: Topic 工具栏**
```
打开 Code Reading Mark Note Pro 工具窗口
选择任意 Topic
在下方的 TopicLine 列表上方看到工具栏
点击刷新图标 (Repair Bookmarks)
```
- 仅修复当前 Topic 的 bookmarks

#### 📝 行号编辑功能

**单个行号编辑**
```
1. 右键点击任意 TopicLine
2. 选择 "Edit Line Number"
3. 在对话框中输入新行号
4. 可选：同时更新关联的 bookmark
```

**批量调整行号**
```
1. 按住 Ctrl 选择多个 TopicLine
2. 右键点击选中的行
3. 选择 "Batch Adjust Line Numbers"
4. 选择调整模式：
   - Add offset: 加上偏移量
   - Subtract offset: 减去偏移量
   - Set to specific: 设置为特定值
5. 预览结果
6. 点击 OK 应用
```

#### 🖱️ 拖拽功能

**多选支持**
```
1. 在 TopicLine 列表中按住 Ctrl 点击多个项目
2. 拖拽选中的项目到新位置
3. 自动重新排序
```

**说明**: 
- 当前使用的是 IntelliJ 内置的 `RowsDnDSupport`
- 支持在同一列表内重新排序
- 跨 Topic 移动需要使用"Move to Topic"功能

## 🐛 故障排除

### 问题：重启后仍然看不到新功能

**解决方案**:
1. 检查插件是否正确加载
   ```
   Settings > Plugins > Installed
   查找 "Code Reading Mark Note Pro"
   ```

2. 检查版本号
   - 应该显示 `3.6.0` 或更高

3. 查看日志
   ```
   Help > Show Log in Explorer
   打开 idea.log
   搜索 "CodeReadingNote" 或错误信息
   ```

### 问题：右键菜单没有新选项

**原因**: 需要选中 TopicLine 才会显示相关菜单项

**验证**:
1. 在 TopicLine 列表中**单击**选中一行（会高亮显示）
2. 再右键点击
3. 应该看到：
   - Remove from Topic
   - Show Bookmark UID
   - Fix Line Remark
   - Move to Group
   - **Edit Line Number** ← 新功能！

4. 选中**多行**（Ctrl+Click）再右键
5. 应该看到：
   - **Batch Adjust Line Numbers** ← 新功能！

### 问题：工具栏按钮不显示

**检查**:
1. 确保你在 "Code Reading Mark Note Pro" 工具窗口中
2. 确保你选择了一个 Topic（左侧列表）
3. 查看 TopicLine 列表上方是否有工具栏
4. 应该看到一个刷新图标 🔄

**如果还是看不到**:
- 尝试调整窗口大小
- 尝试重新打开工具窗口
- 检查 IDE 日志是否有错误

## 📸 预期效果

### Topic 工具栏
```
┌─────────────────────────────────────┐
│ 🔄 Repair Bookmarks                 │  ← 新增工具栏
├─────────────────────────────────────┤
│ ▶ Topic Line 1 (file.java:10)      │
│   Topic Line 2 (file.java:20)      │
│   Topic Line 3 (file.java:30)      │
└─────────────────────────────────────┘
```

### 右键菜单（单选）
```
Remove from Topic
Show Bookmark UID
Fix Line Remark
Move to Group
──────────────────
Edit Line Number      ← 新增！
```

### 右键菜单（多选）
```
Remove from Topic
Show Bookmark UID
Fix Line Remark
Move to Group
──────────────────
Batch Adjust Line Numbers  ← 新增！
```

### Tools 菜单
```
Tools
├── ...
├── Code Reading Note Sync
├── Repair Code Reading Bookmarks  ← 新增！
└── ...
```

## ✅ 功能验证清单

请按以下步骤验证功能：

- [ ] 1. 重新构建插件 (`gradlew buildPlugin`)
- [ ] 2. 重启 IDE
- [ ] 3. 打开 Code Reading Mark Note Pro 工具窗口
- [ ] 4. 选择一个 Topic
- [ ] 5. 看到 TopicLine 列表上方的工具栏（带刷新图标）
- [ ] 6. 单击选中一个 TopicLine
- [ ] 7. 右键查看菜单，应该有 "Edit Line Number"
- [ ] 8. Ctrl+Click 选中多个 TopicLine
- [ ] 9. 右键查看菜单，应该有 "Batch Adjust Line Numbers"
- [ ] 10. 在 Tools 菜单中找到 "Repair Code Reading Bookmarks"

## 📚 更多信息

- **完整实现文档**: `IMPLEMENTATION_COMPLETE.md`
- **设计文档**: `DRAG_DROP_DESIGN.md`（已删除，功能已集成）
- **API 文档**: 查看各个 Action 和 Service 类的 JavaDoc

---

**更新时间**: 2025-11-22  
**当前状态**: ✅ 所有功能已实现并集成到 UI

如果按照上述步骤操作后仍有问题，请检查：
1. IDE 日志文件
2. 编译是否成功（无错误）
3. plugin.xml 是否正确加载

