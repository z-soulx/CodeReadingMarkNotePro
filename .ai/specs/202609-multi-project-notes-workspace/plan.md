# 实施设计

- NoteProjectContext 为运行时归属，TopicList/Topic 持有；TopicLine 使用归属根，保留默认根导入接口。
- WorkspaceNotesService 后台发现和监听，聚合数据；根 getTopicList/getState 仅根，子项目独立存储适配器。
- 列表事件携带归属，统一修改信号驱动保存；装载不发布修改。
- 项目树按选择路由操作，聚合编辑器查询，禁用跨项目移动。
- 保存前磁盘指纹校验、原子替换、保留 XML 未修改内容；冲突保留本地并提供恢复动作。
- 应用级协调独立打开 Project 服务，避免竞争写入。
- 纯文件系统测试核心边界，Gradle 集成校验。

风险：旧根默认调用点、UID/主题隔离、平台生命周期和存储竞争、未知 XML 保留。
