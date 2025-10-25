# NullPointerException 修复说明

## 问题描述

在某些项目启动时，插件会抛出 `NullPointerException` 异常：

```
java.lang.NullPointerException: Cannot invoke "com.intellij.psi.PsiElement.getIcon(int)" because "fileOrDir" is null
	at jp.kitabatakep.intellij.plugins.codereadingnote.ui.TopicDetailPanel$TopicLineListCellRenderer.lambda$getListCellRendererComponent$1(TopicDetailPanel.java:264)
```

## 问题原因

在 `TopicDetailPanel.java` 的 `TopicLineListCellRenderer.getListCellRendererComponent()` 方法中，代码逻辑存在错误：

**错误的代码**（第261-271行）：
```java
ApplicationManager.getApplication().runReadAction(() -> {
    // 在read-action中执行读取PSI树的操作
    PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
    Icon icon = fileOrDir.getIcon(0);  // ❌ 先获取图标
    if (fileOrDir != null) {           // ❌ 后检查null
        // 确保 setIcon 操作在 EDT 上执行
        SwingUtilities.invokeLater(() -> {
            setIcon(icon);
        });
    }
});
```

**问题分析**：
1. 当文件被删除、移动或路径无效时，`PsiUtilCore.findFileSystemItem()` 返回 `null`
2. 代码在第264行直接调用 `fileOrDir.getIcon(0)`，没有先检查 `fileOrDir` 是否为 `null`
3. 第265行的 `if (fileOrDir != null)` 检查来得太晚，异常已经在上一行抛出

## 修复方案

将 null 检查移到获取图标之前：

**修复后的代码**：
```java
ApplicationManager.getApplication().runReadAction(() -> {
    // 在read-action中执行读取PSI树的操作
    PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
    if (fileOrDir != null) {           // ✅ 先检查null
        Icon icon = fileOrDir.getIcon(0);  // ✅ 后获取图标
        // 确保 setIcon 操作在 EDT 上执行
        SwingUtilities.invokeLater(() -> {
            setIcon(icon);
        });
    }
});
```

## 修改文件

- **文件**: `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/TopicDetailPanel.java`
- **方法**: `TopicLineListCellRenderer.getListCellRendererComponent()`
- **行号**: 第261-271行

## 影响范围

此修复影响以下场景：
1. ✅ 打开包含已删除文件的 TopicLine 的项目
2. ✅ 文件路径无效或文件已移动
3. ✅ PSI 树尚未构建完成时显示列表
4. ✅ 项目加载过程中工具窗口被激活

## 测试建议

修复后应测试以下场景：
1. 创建 TopicLine 后删除对应的源文件
2. 移动或重命名 TopicLine 引用的文件
3. 在大型项目中快速打开工具窗口（PSI树可能未完全加载）
4. 导入包含无效文件路径的配置

## 其他相关检查

已经检查了代码库中所有使用 `PsiUtilCore.findFileSystemItem()` 的地方：
- ✅ `TopicDetailPanel.java` - 已修复
- ✅ 其他UI组件 - 未发现类似问题

## 技术细节

### IntelliJ PSI (Program Structure Interface)
- `PsiUtilCore.findFileSystemItem()` 可能返回 `null`，必须进行检查
- PSI 操作必须在 `ReadAction` 中执行
- UI 更新（如 `setIcon()`）必须在 EDT (Event Dispatch Thread) 上执行

### 最佳实践
```java
// ✅ 正确的模式
ApplicationManager.getApplication().runReadAction(() -> {
    PsiElement element = PsiUtilCore.findFileSystemItem(project, file);
    if (element != null) {
        // 使用 element
        Icon icon = element.getIcon(0);
        SwingUtilities.invokeLater(() -> {
            // UI 更新
            setIcon(icon);
        });
    }
});

// ❌ 错误的模式
ApplicationManager.getApplication().runReadAction(() -> {
    PsiElement element = PsiUtilCore.findFileSystemItem(project, file);
    Icon icon = element.getIcon(0); // 可能 NPE
    if (element != null) {
        // ...
    }
});
```

## 版本信息

- **修复版本**: 3.2.0
- **影响版本**: 3.1.0 及更早版本
- **优先级**: 高（影响用户体验，可能导致工具窗口无法显示）

## 相关链接

- 异常堆栈跟踪位置: `TopicDetailPanel.java:264`
- IntelliJ Platform API: [PsiUtilCore Documentation](https://plugins.jetbrains.com/docs/intellij/psi.html)

