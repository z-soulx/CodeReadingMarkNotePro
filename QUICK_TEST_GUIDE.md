# 快速测试指南

## 测试 1: Edit Line Number - 旧 Bookmark 删除

### 准备
1. 在项目中打开一个文件
2. 选择第 53 行代码
3. 右键 → "Add to Topic"
4. 选择一个 Topic，输入 note: "测试行号修改"
5. 添加成功后，第 53 行应该有：
   - IDE 原生 bookmark（左侧装订线有标记）
   - CodeRemark 注释（行尾显示 "测试行号修改"）

### 执行
1. 在 Code Reading Note 工具窗口中找到这个 TopicLine
2. 右键 → "Edit Line Number"
3. 将行号从 53 改为 55
4. 点击 OK

### 验证
✅ **立即观察**（无需关闭文件）：
- [ ] 第 53 行的 bookmark 标记消失
- [ ] 第 53 行的 CodeRemark 注释消失
- [ ] 第 55 行出现 bookmark 标记
- [ ] 第 55 行出现 CodeRemark 注释 "测试行号修改"

✅ **持久化验证**：
- [ ] 关闭 IDE
- [ ] 重新打开项目
- [ ] 第 55 行仍有 bookmark 和 remark
- [ ] 第 53 行干净（无 bookmark 和 remark）

---

## 测试 2: TreeView 拖拽 - Ungrouped → Group

### 准备
1. 创建一个新 Topic，名为 "拖拽测试"
2. 右键 Topic → "Add Group"，创建 Group "目标组"
3. 添加一个 TopicLine（会默认在 Ungrouped Lines）

### 执行
1. 切换到 TreeView 标签（左侧树形视图）
2. 展开 "拖拽测试" Topic
3. 展开 "Ungrouped Lines" 文件夹，看到刚添加的 TopicLine
4. **拖拽** TopicLine 到 "目标组"

### 验证
✅ **立即观察**：
- [ ] TopicLine 从 "Ungrouped Lines" 消失
- [ ] TopicLine 出现在 "目标组" 下
- [ ] "Ungrouped Lines" 旁边的计数减 1
- [ ] "目标组" 旁边的计数加 1

✅ **切换标签验证**：
- [ ] 切换到右侧详细面板
- [ ] 选择 "目标组"
- [ ] TopicLine 在列表中显示

✅ **持久化验证**：
- [ ] 关闭 IDE
- [ ] 重新打开项目
- [ ] TopicLine 仍在 "目标组" 下

---

## 测试 3: TreeView 拖拽 - Group → Group

### 准备
1. 在 "拖拽测试" Topic 下创建两个 Groups:
   - "组A"
   - "组B"
2. 在 "组A" 下添加一个 TopicLine

### 执行
1. 在 TreeView 中，**拖拽** TopicLine 从 "组A" 到 "组B"

### 验证
✅ **立即观察**：
- [ ] TopicLine 从 "组A" 消失
- [ ] TopicLine 出现在 "组B" 下
- [ ] "组A" 计数减 1
- [ ] "组B" 计数加 1

---

## 测试 4: TreeView 拖拽 - Group → Ungrouped

### 准备
1. "组A" 下有一个 TopicLine

### 执行
1. **拖拽** TopicLine 从 "组A" 到 "Ungrouped Lines" 文件夹

### 验证
✅ **立即观察**：
- [ ] TopicLine 从 "组A" 消失
- [ ] TopicLine 出现在 "Ungrouped Lines" 下
- [ ] 计数正确更新

---

## 测试 5: TreeView 拖拽 - 批量拖拽

### 准备
1. 在 "Ungrouped Lines" 下有至少 3 个 TopicLines

### 执行
1. 按住 **Ctrl** 键
2. 点击选择多个 TopicLine（应该都被高亮）
3. 拖拽选中的 TopicLines 到 "目标组"

### 验证
✅ **立即观察**：
- [ ] 所有选中的 TopicLines 都从 "Ungrouped Lines" 消失
- [ ] 所有选中的 TopicLines 都出现在 "目标组" 下
- [ ] 计数正确更新

---

## 测试 6: 负面测试 - 不能拖拽到非法目标

### 执行
1. 尝试拖拽 TopicLine 到 Topic 节点（不是 Group）

### 验证
✅ **预期行为**：
- [ ] 拖拽光标显示"禁止"图标（🚫）
- [ ] 无法放置

---

## 启用调试日志（如果测试失败）

如果任何测试失败，请启用日志：

1. **Help** → **Diagnostic Tools** → **Debug Log Settings**
2. 添加以下内容：
   ```
   #jp.kitabatakep.intellij.plugins.codereadingnote.ui.dnd.TopicTreeTransferHandler
   #jp.kitabatakep.intellij.plugins.codereadingnote.operations.TopicLineOperationService
   #jp.kitabatakep.intellij.plugins.codereadingnote.operations.LineNumberUpdateService
   ```
3. 点击 **OK**
4. 重现失败的测试
5. **Help** → **Show Log in Explorer**
6. 打开 `idea.log`，搜索相关类名
7. 截图日志并提供

---

## 预期日志示例

### Edit Line Number 成功日志
```
LineNumberUpdateService: Updating line number from 53 to 55
LineNumberUpdateService: Removed old bookmark at line 53: true
LineNumberUpdateService: Created new bookmark at line: 55
```

### 拖拽成功日志
```
TopicTreeTransferHandler: Creating transferable for 1 TopicLine(s)
TopicTreeTransferHandler: Dropping 1 line(s) onto 目标组
TopicLineOperationService: Moving 1 lines to group '目标组'
TopicLineOperationService: Removed line from ungrouped
TopicLineOperationService: Added line to target group: 目标组
TopicTreeTransferHandler: Tree model reloaded
```

---

## 所有测试通过 ✅

如果所有测试都通过，说明修复成功！

## 如果有测试失败 ❌

请提供：
1. 失败的测试编号
2. 实际行为（截图）
3. 日志文件（按上述步骤启用并提取）

---

**Happy Testing! 🎉**
