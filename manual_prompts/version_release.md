# 场景1: 版本发布准备

**目的**: 当功能开发完成、测试通过后，准备发布新版本

## 操作步骤

### 1.1 版本号升级

**执行步骤**:
1. 检查当前版本号在 `build.gradle` 中的 `version` 字段
2. 确定新版本号（遵循语义化版本：主版本.次版本.修订号）
3. 同步更新 `src/main/resources/META-INF/plugin.xml` 中的 `<version>` 标签
4. 验证两个文件中的版本号完全一致

**注意事项**:
- 版本号格式：`x.y.z` (例如：3.6.0 → 3.7.0 或 3.6.0 → 3.6.1)
- 主版本：破坏性变更
- 次版本：新增功能
- 修订号：bug修复

### 1.2 变更日志整理

**执行步骤**:
1. 分析 `tmpmd/` 文件夹中的文档，识别本次发布包含的功能
2. 按照功能类型分类整理变更内容：
   - 新功能 (Features)
   - 改进 (Improvements)
   - 修复 (Bug Fixes)
   - 国际化 (Internationalization)

3. 更新 `src/main/resources/META-INF/changeNotes.html`

**变更内容识别规则**:
- **批次判断**: 按照 `tmpmd/` 文件夹的文件批次划分
- **去重原则**: 同批次内的修复属于同一功能，不单独列出
- **内容分类**:
  - 功能实现文档 → 新功能或改进
  - Bug修复文档 → Bug Fixes
  - 国际化文档 → Internationalization

**changeNotes.html 格式要求**:
```html
<h1>版本号</h1>
<ul>
  <li><b>英文标题:</b> 英文描述（<b>中文标题：</b>中文描述）</li>
  <li>其他变更...
</ul>
```

**示例** (基于现有 changeNotes.html 风格):
```html
<h1>3.6.3</h1>
<ul>
  <li><b>New Feature:</b> Added advanced search functionality（<b>新功能：</b>新增高级搜索功能）</li>
  <li><b>UI Improvement:</b> Enhanced dialog layout for better user experience（<b>UI改进：</b>优化对话框布局，提升用户体验）</li>
  <li><b>Bug Fix:</b> Fixed crash when opening empty topics（<b>Bug修复：</b>修复打开空主题时的崩溃问题）</li>
</ul>
```

### 1.3 描述文档更新（可选）

**触发条件**: 当有重大功能新增或架构变更时

**执行步骤**:
1. 更新 `src/main/resources/META-INF/description.html`
2. 更新项目根目录的 `README.md` (中英双语格式)

**描述更新原则**:
- 突出新增的核心功能
- 保持中英双语同时显示格式：英文（中文）
- 更新功能列表和特性说明
- 适当调整使用场景描述

**注意事项**:
- 描述文件应简洁明了，突出卖点
- 保持与 changeNotes 的内容协调
- README 采用中英双语格式，方便不同语言用户阅读

---

*最后更新: 2025-11-24*
*版本: 3.6.0*
