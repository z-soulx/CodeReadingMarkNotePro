# Bookmark 搜索功能扩展 - v3.3.0

## 🎯 功能概述

扩展了搜索功能，支持搜索 IDEA 原生 Bookmark，并添加了搜索范围选择功能。

### ✨ 新特性

1. **搜索范围选择**
   - Topics Only（默认）- 只搜索 Topics 内容
   - Bookmarks Only - 只搜索 IDEA 原生 Bookmarks
   - All - 同时搜索 Topics 和 Bookmarks

2. **Bookmark 搜索**
   - 搜索所有 bookmark 组中的 bookmarks
   - 搜索 bookmark 的描述（description）
   - 支持拼音搜索、模糊匹配等所有搜索算法
   - 显示 bookmark 所在的文件名和行号
   - 双击跳转到 bookmark 位置

3. **统一的搜索体验**
   - Topics 和 Bookmarks 使用相同的搜索算法
   - 统一的结果显示格式（带颜色区分）
   - 统一的相似度评分系统
   - 统一的交互方式（双击跳转、右键菜单）

## 📁 新增文件

### 1. `SearchScope.java`
搜索范围枚举类，定义三种搜索范围。

```java
public enum SearchScope {
    TOPICS_ONLY("Topics Only", "只搜索 Topics"),
    BOOKMARKS_ONLY("Bookmarks Only", "只搜索 Bookmarks"),
    ALL("All", "搜索全部");
}
```

**位置**: `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/search/SearchScope.java`

### 2. `BookmarkSearchResult.java`
Bookmark 搜索结果包装类，封装 bookmark 搜索结果信息。

**位置**: `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/search/BookmarkSearchResult.java`

**包含信息**:
- Bookmark 对象
- 描述文本
- 所属组名
- 文件引用
- 行号
- 相似度评分

## 🔄 修改文件

### 1. `SearchService.java`
扩展搜索服务，添加 Bookmark 搜索功能。

**新增方法**:
- `searchBookmarks(Project, String)` - 搜索所有 bookmarks
- `searchWithScope(Project, List<Topic>, String, SearchScope)` - 根据范围搜索
- `UnifiedSearchResults` - 统一搜索结果容器类

**实现细节**:
```java
public static List<BookmarkSearchResult> searchBookmarks(Project project, String query) {
    // 1. 获取 BookmarksManager
    // 2. 遍历所有 bookmark 组
    // 3. 对每个 bookmark 的 description 计算相似度
    // 4. 返回排序后的结果
}
```

### 2. `SearchPanel.java`
完全重写搜索面板，支持范围选择和统一结果处理。

**UI 改进**:
```
┌────────────────────────────────────────────────┐
│ [Topics Only ▼] [搜索框................] [×]  │
├────────────────────────────────────────────────┤
│ [Topic: XX] ▸ [Group] ▸ 注释内容... 85%       │
│ [Bookmark: YY] ▸ 描述内容... 72%              │
├────────────────────────────────────────────────┤
│ Found 2 result(s) - Topics: 1, Bookmarks: 1   │
└────────────────────────────────────────────────┘
```

**新增接口和类**:
- `UnifiedSearchResultItem` - 统一搜索结果项接口
- `TopicSearchResultItem` - Topic 结果实现
- `BookmarkSearchResultItem` - Bookmark 结果实现

**功能改进**:
- 搜索范围下拉框（左上角）
- 实时切换搜索范围
- 统一的双击跳转
- 智能的右键菜单（根据结果类型显示不同选项）
- 详细的状态栏（显示 Topics 和 Bookmarks 各自的结果数量）

## 🎨 UI 设计

### 搜索结果显示格式

#### Topic 结果
```html
<html>
  <span style='color:#6897BB;'>[Topic: 用户管理]</span>
  <span style='color:#9876AA;'>▸ [登录模块]</span>
  ▸ <b>验证用户密码</b>
  <span style='color:#808080;'>(UserService.java:123)</span>
  <span style='color:#50FA7B;'>85%</span>
</html>
```

#### Bookmark 结果
```html
<html>
  <span style='color:#FF6B6B;'>[Bookmark: MyBookmarks]</span>
  ▸ <b>重要的登录逻辑</b>
  <span style='color:#808080;'>(AuthService.java:45)</span>
  <span style='color:#FFB86C;'>72%</span>
</html>
```

### 颜色方案

| 元素 | 颜色 | 说明 |
|------|------|------|
| Topic 标签 | #6897BB (蓝色) | 与 IntelliJ 关键字颜色一致 |
| Group 标签 | #9876AA (紫色) | 区分层级 |
| Bookmark 标签 | #FF6B6B (红色) | 突出 bookmark 类型 |
| 文件路径 | #808080 (灰色) | 次要信息 |
| 评分 > 70% | #50FA7B (绿色) | 高相关度 |
| 评分 40-70% | #FFB86C (橙色) | 中等相关度 |
| 评分 < 40% | #FF5555 (红色) | 低相关度 |

## 🔍 搜索算法

### Bookmark 搜索
使用与 Topic 搜索相同的算法：

1. **完全匹配** (100%) - description 完全相同
2. **包含匹配** (60-80%) - description 包含查询词
3. **拼音首字母** (50%) - 中文拼音首字母匹配
4. **完整拼音** (55%) - 中文完整拼音匹配
5. **编辑距离** (0-40%) - Levenshtein 距离算法
6. **字符重叠** (0-30%) - 字符级相似度

### 搜索范围逻辑

```java
switch (scope) {
    case TOPICS_ONLY:
        // 只搜索 Topics
        results.topicResults = search(topics, query);
        break;
        
    case BOOKMARKS_ONLY:
        // 只搜索 Bookmarks
        results.bookmarkResults = searchBookmarks(project, query);
        break;
        
    case ALL:
        // 搜索全部
        results.topicResults = search(topics, query);
        results.bookmarkResults = searchBookmarks(project, query);
        break;
}
```

## 💡 使用方式

### 1. 选择搜索范围
点击左上角的下拉框，选择：
- **Topics Only** - 默认选项，只搜索插件管理的 Topics
- **Bookmarks Only** - 只搜索 IDEA 的原生 Bookmarks
- **All** - 同时搜索两者

### 2. 输入搜索关键词
支持：
- 中文/英文文本
- 拼音首字母（如：yhgl → 用户管理）
- 完整拼音（如：yonghuchakan → 用户查看）
- 模糊匹配（允许拼写错误）

### 3. 查看搜索结果
- Topic 结果显示：[Topic: XX] ▸ [Group] ▸ 注释
- Bookmark 结果显示：[Bookmark: XX] ▸ 描述
- 相似度评分：颜色编码（绿/橙/红）

### 4. 跳转到代码
- **双击**结果项：直接跳转到代码位置
- **回车键**：跳转到选中的结果
- **右键菜单**：
  - "Navigate to Code" - 跳转到代码
  - "Locate in Tree View" - 在树视图中定位（仅 Topic 结果）

## 🔧 技术实现

### Bookmark API 使用

```java
// 获取 BookmarksManager
BookmarksManager bookmarksManager = BookmarksManager.getInstance(project);

// 遍历所有组
for (BookmarkGroup group : bookmarksManager.getGroups()) {
    String groupName = group.getName();
    
    // 遍历组中的 bookmarks
    for (Bookmark bookmark : group.getBookmarks()) {
        String description = group.getDescription(bookmark);
        
        // 获取文件和行号（如果是 LineBookmark）
        if (bookmark instanceof LineBookmark) {
            LineBookmark lineBookmark = (LineBookmark) bookmark;
            VirtualFile file = lineBookmark.getFile();
            int line = lineBookmark.getLine();
        }
        
        // 跳转
        if (bookmark.canNavigate()) {
            bookmark.navigate(true);
        }
    }
}
```

### 统一结果处理

使用接口模式实现多态：

```java
interface UnifiedSearchResultItem {
    String getDisplayText();  // 显示文本
    void navigate();          // 跳转行为
    Object getUnderlyingObject(); // 底层对象
}

// Topic 实现
class TopicSearchResultItem implements UnifiedSearchResultItem {
    // 实现跳转到 TopicLine
}

// Bookmark 实现
class BookmarkSearchResultItem implements UnifiedSearchResultItem {
    // 实现跳转到 Bookmark
}
```

## 📊 性能考虑

### 搜索性能
- Bookmark 搜索与 Topic 搜索复杂度相同：O(n × m)
  - n: bookmark 数量
  - m: 查询词长度
- 使用相同的相似度算法，性能一致

### 内存占用
- `UnifiedSearchResults` 只保存必要的引用
- 搜索结果按需创建，不缓存
- 清除搜索时立即释放结果对象

## 🧪 测试建议

### 测试场景

1. **基本功能测试**
   - [ ] 默认搜索范围是 "Topics Only"
   - [ ] 切换到 "Bookmarks Only" 可以搜索 bookmarks
   - [ ] 切换到 "All" 可以同时搜索
   - [ ] 双击 Topic 结果可以跳转
   - [ ] 双击 Bookmark 结果可以跳转

2. **搜索算法测试**
   - [ ] 中文关键词可以匹配 bookmark 描述
   - [ ] 拼音首字母可以搜索中文 bookmark
   - [ ] 完整拼音可以搜索中文 bookmark
   - [ ] 模糊匹配对 bookmark 有效

3. **UI 测试**
   - [ ] Topic 和 Bookmark 结果颜色不同
   - [ ] 状态栏正确显示两种结果数量
   - [ ] 右键菜单对 Bookmark 不显示 "Locate in Tree"
   - [ ] 搜索范围切换立即生效

4. **边界情况测试**
   - [ ] 没有 bookmarks 时搜索不报错
   - [ ] Bookmark 没有 description 时被跳过
   - [ ] 文件不存在的 bookmark 不导致崩溃
   - [ ] 空查询时清除结果

## 🚀 未来改进建议

### 功能增强
1. **搜索历史** - 记住最近的搜索词
2. **高级过滤** - 按文件类型、bookmark 组过滤
3. **搜索预览** - 显示代码片段预览
4. **批量操作** - 批量导出/删除搜索结果
5. **搜索快捷键** - 自定义快捷键切换搜索范围

### 性能优化
1. **索引缓存** - 缓存 bookmark 索引
2. **增量搜索** - 只搜索变化的部分
3. **异步搜索** - 真正的后台线程搜索
4. **结果分页** - 大量结果时分页显示

### UI 改进
1. **搜索建议** - 输入时显示建议词
2. **结果分组** - 按类型/文件/组分组显示
3. **自定义颜色** - 允许用户配置颜色方案
4. **导出结果** - 导出搜索结果为文件

## 📝 版本历史

### v3.3.0 (待发布)
- ✅ 新增 Bookmark 搜索功能
- ✅ 新增搜索范围选择（Topics/Bookmarks/All）
- ✅ 统一搜索结果显示
- ✅ 支持双击跳转到 bookmark
- ✅ 智能右键菜单

### v3.2.0
- ✅ 新增智能搜索功能（拼音、模糊匹配）
- ✅ 搜索支持双击跳转和右键菜单

## 🔗 相关文件

- `SearchScope.java` - 搜索范围枚举
- `BookmarkSearchResult.java` - Bookmark 结果类
- `SearchService.java` - 搜索服务（扩展）
- `SearchPanel.java` - 搜索面板（重写）

## 📧 反馈

如有问题或建议，请通过以下方式反馈：
- GitHub Issues
- JetBrains Plugin 页面评论
- Email: 170918810@qq.com

---

**注意**: 此功能扩展完全向后兼容，不影响现有的 Topic 搜索功能。默认搜索范围为 "Topics Only"，与之前版本行为一致。

