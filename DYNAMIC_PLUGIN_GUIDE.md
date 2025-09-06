# 动态插件支持指南

## 📋 关于提示信息

**提示内容**：
> IDE restart may be unnecessary to enable or disable Code Reading mark Note pro 2.0.0 because the plugin satisfies the requirements for dynamic plugins. Yet the unloading may be restricted if the plugin's class loader cannot be unloaded dynamically or if the plugin has complex dependencies.

**含义**：
- ✅ **好消息**: 插件满足动态插件要求，理论上支持无重启安装/卸载
- ⚠️ **注意**: 可能因类加载器或复杂依赖导致无法完全动态卸载
- 📝 **建议**: 手动测试确认是否真正支持无重启操作

## 🔍 当前插件的动态加载兼容性分析

### ✅ 支持动态加载的特征
1. **标准服务注册**: 使用`<projectService>`标准方式
2. **标准扩展点**: 使用`<toolWindow>`, `<editorFactoryDocumentListener>`等标准扩展
3. **标准监听器**: 使用`<projectListeners>`标准配置
4. **无全局状态**: 没有使用全局静态变量存储状态
5. **标准Action**: 使用标准的Action系统

### ⚠️ 可能影响动态卸载的因素
1. **Document监听器**: 在UI组件中添加了文档监听器
2. **MessageBus连接**: 使用了消息总线连接
3. **BookmarkManager集成**: 与原生书签系统集成

## 🛠️ 优化建议

### 1. 确保监听器正确清理

检查你的监听器是否在插件卸载时正确清理。当前代码中的监听器：

```java
// TopicDetailPanel.java 和 TopicLineDetailPanel.java
noteArea.getDocument().addDocumentListener(new NoteAreaListener(this));
```

**建议**: 确保这些监听器在组件销毁时被移除。

### 2. MessageBus连接管理

当前使用：
```java
// CodeReadingNoteService.java
MessageBus messageBus = project.getMessageBus();
messageBus.connect().subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
    // ...
});
```

**建议**: 使用Disposable模式确保连接被正确关闭：
```java
MessageBusConnection connection = messageBus.connect(this);
connection.subscribe(TopicNotifier.TOPIC_NOTIFIER_TOPIC, new TopicNotifier() {
    // ...
});
```

### 3. 资源清理

确保所有资源在插件卸载时被正确清理：
- 文档监听器
- 消息总线连接
- 文件系统监听器
- 创建的书签引用

## 🧪 测试动态加载

### 手动测试步骤
1. **安装测试**:
   - 在IDEA中打开 Settings → Plugins
   - 安装你的插件
   - 观察是否提示需要重启

2. **功能测试**:
   - 测试插件的所有核心功能
   - 确认工具窗口、菜单、服务都正常工作

3. **卸载测试**:
   - 在Plugins页面禁用插件
   - 观察是否提示需要重启
   - 检查插件功能是否完全停止

4. **重新启用测试**:
   - 重新启用插件
   - 确认功能恢复正常

### 自动化测试
```bash
# 使用插件验证器测试
./gradlew runPluginVerifier

# 构建插件进行测试
./gradlew buildPlugin
```

## 📊 动态插件最佳实践

### ✅ 推荐做法
1. **使用Disposable**: 实现Disposable接口确保资源清理
2. **避免静态状态**: 不在静态字段中存储状态
3. **标准扩展点**: 只使用标准的IntelliJ扩展点
4. **正确的监听器管理**: 确保监听器被正确添加和移除
5. **服务生命周期**: 正确实现服务的创建和销毁

### ❌ 避免的做法
1. **全局静态变量**: 避免在静态字段中存储状态
2. **未清理的监听器**: 添加监听器后不清理
3. **直接操作IDE内部**: 避免使用非公开API
4. **复杂的外部依赖**: 避免依赖难以卸载的外部库

## 🎯 当前状态评估

**你的插件动态加载兼容性**: ⭐⭐⭐⭐☆ (4/5)

**优势**:
- 使用标准的IntelliJ扩展点
- 没有复杂的外部依赖
- 服务和监听器配置标准

**改进空间**:
- 确保文档监听器正确清理
- 优化MessageBus连接管理

## 📝 结论

你的插件已经很好地支持动态加载！这个提示是**信息性**的，不是错误。它表明：

1. ✅ 插件满足动态插件的基本要求
2. ✅ 大部分情况下可以无重启安装/卸载
3. ⚠️ 某些边界情况可能需要重启（这是正常的）

**建议**: 保持当前的良好架构，不需要特别的修改。这个提示可以忽略，或者按照上述建议进行小幅优化。
