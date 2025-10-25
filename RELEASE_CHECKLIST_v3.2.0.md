# Release Checklist for v3.2.0

## 版本信息
- **版本号**: 3.2.0
- **发布日期**: [待定]
- **主要功能**: 智能搜索功能

## 预发布检查清单

### 1. 配置文件检查
- [x] `build.gradle` 版本号: 3.2.0
- [x] `plugin.xml` 版本号: 3.2.0
- [x] `plugin.xml` 插件名称: "Code Reading Mark Note Pro"
- [x] `plugin.xml` 工具窗口ID: "Code Reading Mark Note Pro"
- [x] `changeNotes.html` 已更新3.2.0版本说明

### 2. 新功能验证

#### 搜索功能
- [ ] 搜索框正常显示，提示文本为英文
- [ ] 实时搜索功能正常工作
- [ ] 拼音搜索（首字母和完整拼音）正常工作
- [ ] 模糊搜索正常工作
- [ ] 搜索结果按相似度排序
- [ ] 搜索结果显示格式正确（Topic > Group > Note）
- [ ] 双击搜索结果能跳转到代码
- [ ] 右键菜单显示正常
- [ ] "Navigate to Code" 菜单项功能正常
- [ ] "Locate in Tree View (Switch to Tree)" 菜单项功能正常
- [ ] 状态栏显示搜索结果数量
- [ ] 清除按钮功能正常

#### UI英文化
- [ ] 标签页显示为 "Tree View" 和 "Search"
- [ ] 搜索框提示: "Search TopicLine notes... (Supports Pinyin/Fuzzy search)"
- [ ] 清除按钮提示: "Clear search"
- [ ] 状态栏消息为英文
- [ ] 搜索结果中的"未分组"显示为 "Ungrouped"

### 3. 现有功能回归测试
- [ ] Topic创建/重命名/删除功能正常
- [ ] Group创建/重命名/删除功能正常
- [ ] TopicLine添加/删除功能正常
- [ ] 树视图展开/折叠功能正常
- [ ] 一键折叠/展开按钮功能正常
- [ ] 在编辑器中"Add to Topic"右键菜单正常
- [ ] TopicLine跳转到代码功能正常
- [ ] 导入/导出功能正常
- [ ] Bookmark集成功能正常
- [ ] **Bug修复测试1**: 删除TopicLine引用的文件后，列表不再抛出NPE
- [ ] **Bug修复测试2**: 跨分支场景（切换到文件不存在的分支），打开文件不再抛出NPE

### 4. 兼容性测试
- [ ] IntelliJ IDEA 2024.3 测试通过
- [ ] IntelliJ IDEA 2025.1 测试通过
- [ ] 其他JetBrains IDE（如PyCharm、WebStorm）测试通过
- [ ] Windows系统测试通过
- [ ] macOS系统测试通过（如果可能）
- [ ] Linux系统测试通过（如果可能）

### 5. 性能测试
- [ ] 大量TopicLine（100+）时搜索性能正常
- [ ] UI响应流畅，无卡顿
- [ ] 内存占用正常

### 6. 构建和打包
- [ ] 清理旧的构建文件: `gradlew clean`
- [ ] 编译成功: `gradlew build`
- [ ] 打包插件: `gradlew buildPlugin`
- [ ] 生成的插件文件位置: `build/distributions/CodeReadingMarkNotePro-3.2.0.zip`
- [ ] 插件文件大小合理（< 5MB）

### 7. 安装测试
- [ ] 从本地zip文件安装插件成功
- [ ] 插件在IDE中显示名称正确
- [ ] 工具窗口标题显示正确
- [ ] 所有功能在全新安装中正常工作

### 8. 文档更新
- [x] `SEARCH_FEATURE.md` - 中文搜索功能说明
- [x] `SEARCH_FEATURE_ENGLISH.md` - 英文搜索功能说明
- [x] `PLUGIN_NAME_FIX.md` - 插件名称修复说明
- [x] `RELEASE_CHECKLIST_v3.2.0.md` - 发布检查清单（本文件）
- [ ] `README.md` - 更新主要功能列表（如需要）

### 9. 发布到JetBrains Marketplace
- [ ] 登录 https://plugins.jetbrains.com/
- [ ] 上传插件zip文件
- [ ] 填写版本说明（从changeNotes.html复制）
- [ ] 设置兼容IDE版本: 2024.3 - 2025.3
- [ ] 提交审核

### 10. 发布后验证
- [ ] 插件在Marketplace中可见
- [ ] 版本号显示正确: 3.2.0
- [ ] 插件名称显示正确: Code Reading Mark Note Pro
- [ ] 下载链接正常
- [ ] 用户可以通过IDE插件市场搜索到

## 主要新增文件
```
src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/
├── search/
│   ├── PinyinUtils.java          # 拼音工具类
│   ├── SearchService.java         # 搜索服务
│   └── (相关类)
└── ui/
    ├── SearchPanel.java           # 搜索UI面板
    └── (已修改的UI类)
```

## 主要修改文件
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/Topic.java`
  - 新增 `getGroups()` 和 `getUngroupedLines()` 方法
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/ManagementPanel.java`
  - 集成搜索面板
  - 添加标签页（Tree View / Search）
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/TopicTreePanel.java`
  - 新增 `selectGroupLine()` 和 `selectUngroupedLine()` 方法
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/ui/TopicDetailPanel.java`
  - 修复 NullPointerException：先检查 null 再获取图标
  - 修复 lineAdded 回调中的潜在 NPE
- `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/CodeReadingNoteService.java`
  - 修复 list() 和 listSource() 方法的跨分支 NPE 问题
- `src/main/resources/META-INF/plugin.xml`
  - 修复工具窗口ID大小写
  - 更新版本号
- `src/main/resources/META-INF/changeNotes.html`
  - 添加3.2.0版本更新说明

## 已知问题
- Gradle编译时可能遇到Java版本兼容性问题，建议在IntelliJ IDEA中直接构建

## 备注
此版本主要新增了智能搜索功能，支持拼音搜索、模糊搜索和向量式搜索，大大提升了用户查找TopicLine的效率。

