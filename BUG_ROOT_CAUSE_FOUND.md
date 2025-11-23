# Bug 根本原因：双重 UUID 生成

## 问题描述
添加 TopicLine 后，TopicLine 存储的 UUID 和 bookmark 的 UUID 不一致，导致修改行号时找不到旧 bookmark，无法删除。

## 根本原因

### 重复的 lineAdded 监听器

有**两个地方**都在监听 `lineAdded` 事件，并且都在生成 UUID 和创建 bookmark：

#### 1. CodeReadingNoteService.java (第 66-76 行)
```java
@Override
public void lineAdded(Topic _topic, TopicLine _topicLine) {
    String uid = UUID.randomUUID().toString();  // ← 生成 UUID-A
    Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
    if (bookmark != null) {
        _topicLine.setBookmarkUid(uid);  // ← 设置 UUID-A 到 TopicLine
    }
    EditorUtils.addLineCodeRemark(project, _topicLine);
    scheduleAutoSync();
}
```

#### 2. TopicDetailPanel.java (第 105-121 行) ❌ **问题代码**
```java
@Override
public void lineAdded(Topic _topic, TopicLine _topicLine) {
    if (_topic == topic) {
        if (_topicLine.file() != null) {
            String uid = UUID.randomUUID().toString();  // ← 生成 UUID-B (不同!)
            Bookmark bookmark = BookmarkUtils.addBookmark(project, _topicLine.file(), _topicLine.line(), _topicLine.note(), uid);
            if (bookmark != null) {
                _topicLine.setBookmarkUid(uid);  // ← 设置 UUID-B，覆盖了 UUID-A!
            }
        }
        topicLineListModel.addElement(_topicLine);
    }
}
```

### 执行流程

当用户添加一个 TopicLine 时：

```
1. Topic.addLine(topicLine) 被调用
2. 触发 lineAdded 事件
3. CodeReadingNoteService 监听器执行：
   - 生成 UUID-A: "abc-123"
   - 创建 bookmark-A 用 UUID-A
   - TopicLine.setBookmarkUid("abc-123")
   
4. TopicDetailPanel 监听器执行：
   - 生成 UUID-B: "xyz-789" (新的，不同的!)
   - 创建 bookmark-B 用 UUID-B
   - TopicLine.setBookmarkUid("xyz-789") ← 覆盖了之前的 UUID-A!

5. 最终状态：
   - TopicLine 的 UUID = "xyz-789" (UUID-B)
   - bookmark-A 的 UUID = "abc-123"
   - bookmark-B 的 UUID = "xyz-789"
   
6. 问题：
   - 有两个 bookmark（重复）
   - 当修改行号时，尝试用 TopicLine 的 UUID ("xyz-789") 查找
   - 如果 bookmark-A 先创建，可能找到的是 bookmark-A
   - 但 bookmark-A 的 UUID 是 "abc-123"，不匹配！
   - 删除失败，旧 bookmark 残留
```

### 为什么会有两个监听器？

这可能是历史遗留问题：

1. **最初设计**：`TopicDetailPanel` 负责创建 bookmark
2. **后来重构**：将 bookmark 创建移到了 `CodeReadingNoteService`（更合理，集中管理）
3. **遗漏**：忘记删除 `TopicDetailPanel` 中的旧代码

## 修复方案

### 删除 TopicDetailPanel 中的重复逻辑

**文件:** `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/TopicDetailPanel.java`

**修改前:**
```java
@Override
public void lineAdded(Topic _topic, TopicLine _topicLine) {
    if (_topic == topic) {
        // 只有当文件存在时才添加书签
        if (_topicLine.file() != null) {
            String uid = UUID.randomUUID().toString();  // ❌ 重复生成 UUID
            Bookmark bookmark = BookmarkUtils.addBookmark(...);  // ❌ 重复创建 bookmark
            if (bookmark != null) {
                _topicLine.setBookmarkUid(uid);  // ❌ 覆盖 UUID
            }
        }
        topicLineListModel.addElement(_topicLine);
    }
}
```

**修改后:**
```java
@Override
public void lineAdded(Topic _topic, TopicLine _topicLine) {
    if (_topic == topic) {
        // Bookmark creation is now handled by CodeReadingNoteService.lineAdded()
        // Don't create duplicate bookmarks here!
        // Just update the UI
        topicLineListModel.addElement(_topicLine);
    }
}
```

### 职责分离

修复后，职责更清晰：

| 组件 | 职责 |
|------|------|
| **CodeReadingNoteService** | 业务逻辑：创建 bookmark、设置 UUID、添加 remark、触发同步 |
| **TopicDetailPanel** | UI 更新：将 TopicLine 添加到列表显示 |

## 修复效果

### 修复前
```
添加 TopicLine:
  → CodeReadingNoteService: UUID-A, bookmark-A
  → TopicDetailPanel: UUID-B, bookmark-B
  → TopicLine 最终 UUID: UUID-B
  → 有两个 bookmark，UUID 不一致 ❌
```

### 修复后
```
添加 TopicLine:
  → CodeReadingNoteService: UUID-A, bookmark-A
  → TopicDetailPanel: 只更新 UI
  → TopicLine 最终 UUID: UUID-A
  → 只有一个 bookmark，UUID 一致 ✅
```

### 修改行号时
```
修复前:
  1. 用 TopicLine UUID (UUID-B) 查找 bookmark
  2. 找到 bookmark-A (UUID-A)
  3. UUID 不匹配，删除失败 ❌
  
修复后:
  1. 用 TopicLine UUID (UUID-A) 查找 bookmark
  2. 找到 bookmark-A (UUID-A)
  3. UUID 匹配，成功删除 ✅
  4. 创建新 bookmark 用相同 UUID (UUID-A)
  5. 完美！✨
```

## 其他相关修复

为了完整解决问题，还包含了之前的修复：

### 1. 确保完整删除 bookmark
**文件:** `BookmarkUtils.java`
```java
public static boolean removeMachBookmark(TopicLine _topicLine, Project project) {
    ...
    if (bookmark != null) {
        // 从两个地方删除
        group.remove(bookmark);      // 从分组删除
        manager.remove(bookmark);    // 从管理器删除（真正删除）✅
        return true;
    }
    return false;
}
```

### 2. 详细日志
添加了详细的调试日志，帮助追踪：
- UUID 生成和匹配过程
- Bookmark 创建和删除过程
- Description 格式是否正确

## 测试验证

### 测试步骤
1. 编译运行: `./gradlew runIde`
2. 创建一个 Topic
3. 添加一个 TopicLine (例如在第 58 行)
4. 查看日志，应该只看到**一次** bookmark 创建
5. 检查编辑器：只有**一个** bookmark
6. 修改行号到 60
7. 查看日志，应该看到成功找到并删除旧 bookmark
8. 检查编辑器：
   - ✅ 第 58 行没有 bookmark
   - ✅ 第 60 行有 bookmark
   - ✅ 只有一个 bookmark

### 预期日志输出

**添加 TopicLine 时（应该只出现一次）：**
```
========================================
Creating bookmark at line 58
  File: /path/to/file.java
  Note: your note
  UUID: abc-123-def-456
  Description to create: 'your note$abc-123-def-456'
  ...
========================================
```

**修改行号时（应该能找到 bookmark）：**
```
========================================
Searching for bookmark with UUID: abc-123-def-456
TopicLine: /path/to/file.java:58
Total bookmarks in group 'Code Reading Mark Note Pro': 1
  [0] description='your note$abc-123-def-456'
      extracted UUID='abc-123-def-456'
      matches target? true
  ✓✓✓ Found matching bookmark at index 0!
========================================
```

## 总结

这是一个经典的**事件监听器重复订阅**问题：

1. ✅ **根本原因**：两个监听器都在处理同一个事件，导致重复操作
2. ✅ **修复方法**：删除 UI 层的业务逻辑，保持职责分离
3. ✅ **附加修复**：确保 bookmark 从两个地方都删除
4. ✅ **调试支持**：添加详细日志，便于追踪问题

**这次修复应该彻底解决了 UUID 不匹配的问题！** 🎉

## 变更文件清单

1. ✅ `TopicDetailPanel.java` - 删除重复的 bookmark 创建逻辑（核心修复）
2. ✅ `BookmarkUtils.java` - 确保完整删除 + 详细日志
3. ✅ `LineNumberUpdateService.java` - 详细日志
4. ✅ `CodeReadingNoteService.java` - 保持原有逻辑（正确的）

**没有改动原来的设计，只是删除了重复代码！**

