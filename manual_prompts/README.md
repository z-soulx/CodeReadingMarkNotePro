# 手动操作提示词

这个文件夹包含Code Reading Mark Note Pro项目的两类手动操作提示词。

## 📁 文件说明

### [version_release.md](version_release.md) - 版本发布准备
**使用场景**: 当功能开发完成、测试通过后，准备发布新版本时

**主要操作**:
- 版本号升级 (build.gradle + plugin.xml)
- 变更日志整理 (changeNotes.html)
- 描述文档更新 (description.html + README)

### [context_optimization.md](context_optimization.md) - 功能验收与上下文优化
**使用场景**: 当计划功能完成后，分析开发文档，评估是否需要更新项目上下文

**主要操作**:
- tmpmd文档分析
- PROJECT_CONTEXT.md对比分析
- 更新决策判断
- 上下文文档优化

## 🚀 使用流程

### 版本发布流程
1. 功能开发完成，tmpmd/ 已清空
2. 打开 `version_release.md`
3. 复制版本升级命令，替换新版本号
4. 执行命令完成版本更新
5. 如有必要，更新变更日志和描述文档

### 上下文优化流程
1. 功能验收完成，准备开始新功能
2. 打开 `context_optimization.md`
3. 分析tmpmd内容与PROJECT_CONTEXT.md的差异
4. 根据决策标准判断是否需要更新
5. 如需要，按规范更新PROJECT_CONTEXT.md


## 📝 注意事项

- **版本升级**: 确保测试充分后再发布
- **变更日志**: 基于tmpmd批次去重，避免重复记录
- **上下文更新**: 保持精简，只记录核心稳定的信息
- **文档风格**: 保持中英文双语描述一致性

## 🔗 相关文档

- `../PROJECT_CONTEXT.md` - 项目上下文和规则
- `../tmpmd/` - 临时开发文档存放地
- `../build.gradle` - 构建配置
- `../src/main/resources/META-INF/plugin.xml` - 插件配置

---

*最后更新: 2025-11-24*
*版本: 3.6.0*
