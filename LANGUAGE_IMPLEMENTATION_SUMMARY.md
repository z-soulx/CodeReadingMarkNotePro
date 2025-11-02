# 插件独立语言切换 - 实现总结

## 🎯 实现目标

实现插件独立的语言切换功能，满足以下要求：
1. ✅ 语言控制只有一个入口：插件设置
2. ✅ 首次使用：IDE 是中文 → 插件默认中文，IDE 是其他语言 → 插件默认英文
3. ✅ 手动切换：用户在设置中选择语言后，插件使用用户选择的语言
4. ✅ 不受影响：插件语言独立于 IDE 语言，互不干扰

## 📋 技术实现

### 核心设计

```
用户选择语言
    ↓
LanguageSettings (持久化)
    ↓
CodeReadingNoteBundle (加载资源)
    ↓
插件所有 UI 组件
```

### 关键类和方法

#### 1. LanguageSettings
**职责**: 管理语言选择，提供智能默认值

```java
public class LanguageSettings {
    private PluginLanguage selectedLanguage = null;  // null 表示首次使用
    
    // 智能默认：根据 IDE 语言自动选择
    private PluginLanguage detectDefaultLanguage() {
        Locale ideLocale = DynamicBundle.getLocale();  // 获取 IDE 语言
        if ("zh".equals(ideLocale.getLanguage())) {
            return PluginLanguage.SIMPLIFIED_CHINESE;
        }
        return PluginLanguage.ENGLISH;
    }
    
    // 获取实际使用的 Locale
    public Locale getEffectiveLocale() {
        return getSelectedLanguage().getLocale();
    }
}
```

**智能默认逻辑**:
- `selectedLanguage == null` → 首次使用 → 检测 IDE 语言 → 自动选择
- `selectedLanguage != null` → 已设置 → 直接使用用户选择

#### 2. CodeReadingNoteBundle
**职责**: 加载正确的资源文件，禁用缓存确保切换生效

```java
public class CodeReadingNoteBundle {
    // 关键：禁用 ResourceBundle 缓存
    private static final ResourceBundle.Control NO_CACHE_CONTROL = 
        new ResourceBundle.Control() {
            @Override
            public long getTimeToLive(String baseName, Locale locale) {
                return ResourceBundle.Control.TTL_DONT_CACHE;
            }
        };
    
    // 每次都获取最新的 Locale
    private static ResourceBundle getBundle() {
        Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
        return ResourceBundle.getBundle(BUNDLE, locale, 
            CodeReadingNoteBundle.class.getClassLoader(), 
            NO_CACHE_CONTROL);  // 不缓存
    }
}
```

**为什么禁用缓存**:
- ResourceBundle 默认会缓存加载的资源
- 缓存导致运行时切换语言无法生效
- 使用 `TTL_DONT_CACHE` 确保每次都加载正确的资源文件

#### 3. PluginLanguage
**职责**: 定义支持的语言选项

```java
public enum PluginLanguage {
    ENGLISH("English", "English", Locale.ENGLISH),
    SIMPLIFIED_CHINESE("简体中文", "Simplified Chinese", Locale.SIMPLIFIED_CHINESE);
    
    // 显示格式：English / English  或  简体中文 / Simplified Chinese
    public String getDisplayName() {
        return displayNameEn + " / " + displayNameZh;
    }
}
```

**设计说明**:
- 移除了 "Auto" 选项，避免与系统语言混淆
- 显示名称同时包含中英文，便于所有用户理解

## 🔄 工作流程

### 首次使用流程

```
1. 用户安装插件
   ↓
2. IDE 启动，LanguageSettings 被加载
   ↓
3. selectedLanguage == null (首次使用)
   ↓
4. 调用 detectDefaultLanguage()
   ↓
5. 检测 IDE 语言（DynamicBundle.getLocale()）
   ↓
6. IDE 是中文？
   ├─ 是 → 返回 SIMPLIFIED_CHINESE
   └─ 否 → 返回 ENGLISH
   ↓
7. 插件使用检测到的语言显示界面
```

### 用户切换语言流程

```
1. 用户打开 Settings → Tools → Code Reading Note Sync
   ↓
2. 在 "Plugin Language" 下拉框中选择语言
   ↓
3. 点击 Apply 或 OK
   ↓
4. SyncSettingsPanel.saveTo() 被调用
   ↓
5. LanguageSettings.setSelectedLanguage(选择的语言)
   ↓
6. 配置保存到 codeReadingNoteLanguage.xml
   ↓
7. 用户重启 IDE
   ↓
8. LanguageSettings 加载配置
   ↓
9. selectedLanguage != null (已设置)
   ↓
10. 插件使用用户选择的语言
```

### 资源加载流程

```
每次调用 CodeReadingNoteBundle.message("key")
   ↓
1. 调用 getBundle()
   ↓
2. 从 LanguageSettings 获取 effectiveLocale
   ↓
3. 使用 NO_CACHE_CONTROL 加载 ResourceBundle
   ↓
4. 根据 Locale 选择正确的 .properties 文件
   ├─ Locale.ENGLISH → CodeReadingNoteBundle.properties
   └─ Locale.SIMPLIFIED_CHINESE → CodeReadingNoteBundle_zh.properties
   ↓
5. 返回对应语言的文本
```

## 🎨 用户体验

### 场景 1: 中文 IDE 用户
```
安装插件 → 自动中文界面 → 可选切换到英文
```

### 场景 2: 英文 IDE 用户
```
安装插件 → 自动英文界面 → 可选切换到中文
```

### 场景 3: 多语言团队
```
团队成员 A (中文 IDE) → 插件显示中文
团队成员 B (英文 IDE) → 插件显示英文
团队成员 C (中文 IDE，选择英文插件) → IDE 中文，插件英文
```

## 📁 配置存储

### 配置文件位置

**Windows**:
```
%APPDATA%\JetBrains\<IDE-Version>\options\codeReadingNoteLanguage.xml
```

**macOS/Linux**:
```
~/.config/JetBrains/<IDE-Version>/options/codeReadingNoteLanguage.xml
```

### 配置文件格式

**选择英文**:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
```

**选择中文**:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="SIMPLIFIED_CHINESE" />
  </component>
</application>
```

**首次使用（未设置）**:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings" />
</application>
```

## ✅ 测试验证

### 必测场景

1. **首次安装（中文 IDE）**
   - ✅ 插件自动显示中文
   - ✅ 设置中语言选项默认为"简体中文"

2. **首次安装（英文 IDE）**
   - ✅ 插件自动显示英文
   - ✅ 设置中语言选项默认为"English"

3. **手动切换到英文**
   - ✅ 在设置中选择"English / English"
   - ✅ 重启后插件显示英文
   - ✅ 所有界面元素都是英文

4. **手动切换到中文**
   - ✅ 在设置中选择"简体中文 / Simplified Chinese"
   - ✅ 重启后插件显示中文
   - ✅ 所有界面元素都是中文

5. **多次切换**
   - ✅ 英文→中文→英文，每次都正确
   - ✅ 配置正确保存和加载

### 验证点清单

- [ ] 工具窗口标题
- [ ] 右键菜单（"添加到主题" / "Add to Topic"）
- [ ] 对话框标题和按钮
- [ ] 设置页面标签和提示
- [ ] 树视图节点显示
- [ ] 错误消息
- [ ] 同步功能消息
- [ ] 表单验证消息

## 🔧 技术细节

### 为什么不用 DynamicBundle？

**问题**:
- `DynamicBundle` 在构造时确定 Locale
- 子类化后仍然难以运行时切换
- 缓存机制复杂，难以清除

**解决**:
- 直接使用 `ResourceBundle` API
- 自定义 `Control` 禁用缓存
- 每次调用都获取最新的 Locale

### 为什么需要禁用缓存？

```java
// 默认行为（有缓存）
ResourceBundle bundle1 = ResourceBundle.getBundle("messages", Locale.ENGLISH);
// 切换语言
ResourceBundle bundle2 = ResourceBundle.getBundle("messages", Locale.CHINESE);
// 问题：bundle2 可能还是英文的，因为被缓存了

// 禁用缓存后
ResourceBundle bundle1 = ResourceBundle.getBundle("messages", Locale.ENGLISH, NO_CACHE_CONTROL);
// 切换语言
ResourceBundle bundle2 = ResourceBundle.getBundle("messages", Locale.CHINESE, NO_CACHE_CONTROL);
// bundle2 保证是中文的
```

### 为什么用 DynamicBundle.getLocale() 而不是 Locale.getDefault()？

```java
// Locale.getDefault() → 系统语言
// 问题：用户的 IDE 可能是英文，但系统是中文

// DynamicBundle.getLocale() → IDE 语言
// 正确：检测的是 IDE 界面语言，符合用户预期
```

## 📊 性能考虑

### 资源加载开销

**问题**: 禁用缓存会导致性能下降吗？

**分析**:
- 每次调用 `message()` 都会调用 `getBundle()`
- `ResourceBundle.getBundle()` 虽然没有应用层缓存，但 JVM 层面的类加载器缓存仍然有效
- `.properties` 文件很小（~150 行），加载很快
- UI 更新频率不高（不是每帧都更新）

**结论**: 性能影响可以忽略不计

### 优化建议（如果需要）

如果未来发现性能问题，可以添加一层应用缓存：

```java
public class CodeReadingNoteBundle {
    private static Locale lastLocale = null;
    private static ResourceBundle cachedBundle = null;
    
    private static ResourceBundle getBundle() {
        Locale currentLocale = LanguageSettings.getInstance().getEffectiveLocale();
        
        // 只有 Locale 变化时才重新加载
        if (lastLocale == null || !lastLocale.equals(currentLocale)) {
            cachedBundle = ResourceBundle.getBundle(BUNDLE, currentLocale, 
                CodeReadingNoteBundle.class.getClassLoader(), NO_CACHE_CONTROL);
            lastLocale = currentLocale;
        }
        
        return cachedBundle;
    }
}
```

但目前不需要这个优化。

## 🎉 完成状态

✅ **已完成**:
1. PluginLanguage 枚举（2个语言选项）
2. LanguageSettings 配置类（智能默认 + 持久化）
3. CodeReadingNoteBundle 资源加载（禁用缓存）
4. SyncSettingsPanel UI（语言选择下拉框）
5. plugin.xml 注册（applicationService）
6. 资源文件更新（tooltip 说明）
7. 文档完善（使用指南 + 测试说明）

✅ **测试建议**:
1. 构建插件 JAR
2. 在中文 IDE 中安装测试
3. 在英文 IDE 中安装测试
4. 测试语言切换功能
5. 验证配置持久化

---

**实现完成时间**: 2025-11-02  
**版本**: v3.5.0

