# tmpmd - 临时开发文档文件夹

## 用途

此文件夹用于存放开发过程中的临时文档，包括：
- 新功能开发文档
- Bug修复记录
- 技术调研笔记
- 设计讨论记录

## 命名规范

### 新功能文档
```
feature_{功能名}.md
```
示例：
- `feature_bookmark_sync.md`
- `feature_multi_language.md`

### Bug修复文档
```
bugfix_{问题简述}.md
```
示例：
- `bugfix_npe_exception.md`
- `bugfix_language_switch.md`

### 优化文档
```
optimize_{优化内容}.md
```
示例：
- `optimize_performance.md`
- `optimize_ui_responsive.md`

## 文档生命周期

1. **开发阶段**: 创建相关文档，记录分析、设计、实现过程
2. **验收阶段**: 功能完成后，整理tmpmd/内容，去重汇总
3. **归档阶段**: 检查是否需要更新PROJECT_CONTEXT.md，然后清理tmpmd/

## 注意事项

- 文档应使用Markdown格式
- 包含必要的代码示例和截图
- 记录重要决策和原因
- 功能完成后及时清理，避免文档堆积

## 相关链接

- [项目上下文](../PROJECT_CONTEXT.md) - 项目整体规范和规则
- [README](../README.md) - 项目说明文档
