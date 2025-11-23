# Linter 错误修复指南

## 🎯 问题根源

代码中使用了错误的 TopicLine 方法名。TopicLine 类使用的是简短的方法名风格（如 `line()`, `url()`, `note()`），而不是传统的 JavaBean getter 风格（如 `getLineNum()`, `getFilePath()`, `getNote()`）。

## 📋 正确的方法映射

| 错误的方法调用 | 正确的方法调用 | 说明 |
|---------------|---------------|------|
| `topicLine.getLineNum()` | `topicLine.line()` | 获取行号 |
| `topicLine.getFilePath()` | `topicLine.url()` | 获取文件路径 |
| `topicLine.getTopic()` | `topicLine.topic()` | 获取所属 Topic |
| `topicLine.getNote()` | `topicLine.note()` | 获取备注 |
| `topicLine.getBookmarkUuid()` | `topicLine.getBookmarkUid()` | 获取 Bookmark UUID |
| `topicLine.setBookmarkUuid(x)` | `topicLine.setBookmarkUid(x)` | 设置 Bookmark UUID |
| `group.getName()` | `group.name()` | 获取分组名称 |

## 🔧 需要修复的文件和位置

### 1. TopicLineOperationService.java (6处)
```java
// 第 50 行
Topic sourceTopic = lines.get(0).topic(); // 改为 topic()

// 第 93 行  
String targetGroupName = targetGroup != null ? targetGroup.name() : "Ungrouped"; // 改为 name()

// 第 139 行
Topic topic = line.topic(); // 改为 topic()

// 第 158 行
String uuid = line.getBookmarkUid(); // 改为 getBookmarkUid()

// 第 164 行
line.note() // 改为 note()

// 第 199 行
Topic topic = line.topic(); // 改为 topic()
```

### 2. BookmarkRepairService.java (13处)
所有 `getBookmarkUuid()` → `getBookmarkUid()`
所有 `setBookmarkUuid()` → `setBookmarkUid()`
所有 `getFilePath()` → `url()`
所有 `getLineNum()` → `line()`
所有 `getNote()` → `note()`

### 3. LineNumberUpdateService.java (9处)
所有 `getLineNum()` → `line()`
所有 `getFilePath()` → `url()`
所有 `getTopic()` → `topic()`
所有 `getBookmarkUuid()` → `getBookmarkUid()`

### 4. EditLineNumberDialog.java (6处)
所有 `getLineNum()` → `line()`
所有 `getFilePath()` → `url()`

### 5. BatchLineNumberAdjustDialog.java (3处)
所有 `getLineNum()` → `line()`
所有 `getFilePath()` → `url()`

### 6. EditLineNumberAction.java (2处)
所有 `getLineNum()` → `line()`
所有 `getFilePath()` → `url()`

## 🤖 批量修复命令

可以使用 search_replace 工具批量修复每个文件。修复顺序建议：

1. 先修复简单的替换（如 `getLineNum()` → `line()`）
2. 再修复需要上下文的替换
3. 最后检查 BookmarkUtils 相关的方法

## ⚠️ 注意事项

1. `setLineNum(int)` 方法不存在，TopicLine 使用 `modifyLine(int)` 方法
2. BookmarkUtils 可能需要添加或修复以下方法：
   - `findBookmarkByUuid(Project, String)`
   - `getBookmarkUuid(Bookmark)`
   - `setBookmarkUuid(Bookmark, String)` (已有，但可能叫 `setBookmarkUid`)

## 📊 修复进度

- [ ] TopicLineOperationService.java
- [ ] BookmarkRepairService.java
- [ ] LineNumberUpdateService.java  
- [ ] EditLineNumberDialog.java
- [ ] BatchLineNumberAdjustDialog.java
- [ ] EditLineNumberAction.java
- [ ] 检查 BookmarkUtils 方法
- [ ] 测试编译

---

**预计修复时间**: 15 分钟  
**当前状态**: 待修复

