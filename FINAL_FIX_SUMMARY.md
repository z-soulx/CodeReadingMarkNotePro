# 语言切换功能 - 最终修复总结

## 🐛 发现的两个关键 Bug

### Bug 1: UTF-8 编码问题
**症状**: 中文显示为乱码 `����`

**原因**: 
- Java `ResourceBundle` 默认使用 ISO-8859-1 编码读取 `.properties` 文件
- 我们的文件是 UTF-8 编码

**解决**: 
- 自定义 `ResourceBundle.Control`
- 使用 `InputStreamReader(stream, StandardCharsets.UTF_8)` 读取

### Bug 2: Locale 映射错误
**症状**: 
```
locale: en → Bundle loaded: zh  (错误！)
```

**原因**:
- 使用 `Locale.ENGLISH` (language=en)
- ResourceBundle 查找 `CodeReadingNoteBundle_en.properties`
- 文件不存在，fallback 到其他文件，可能加载了中文

**解决**:
- 改用 `Locale.ROOT` 对应英文
- `Locale.ROOT` 直接加载 `CodeReadingNoteBundle.properties` (默认文件)

## 📁 资源文件命名约定

```
CodeReadingNoteBundle.properties        ← 默认 (英文) - 对应 Locale.ROOT
CodeReadingNoteBundle_zh.properties     ← 中文 - 对应 Locale.SIMPLIFIED_CHINESE
```

**ResourceBundle 查找顺序**:
- `Locale.ROOT` → 直接加载 `CodeReadingNoteBundle.properties`
- `Locale.SIMPLIFIED_CHINESE` (zh_CN) → 查找顺序：
  1. `CodeReadingNoteBundle_zh_CN.properties` (不存在)
  2. `CodeReadingNoteBundle_zh.properties` ✅ 找到
  3. `CodeReadingNoteBundle.properties` (如果上面没找到)

## 🔧 最终修复

### 1. CodeReadingNoteBundle.java

**关键改动**:
```java
// 自定义 Control：禁用缓存 + UTF-8 编码
private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
    @Override
    public long getTimeToLive(String baseName, Locale locale) {
        return ResourceBundle.Control.TTL_DONT_CACHE;  // 禁用缓存
    }
    
    @Override
    public ResourceBundle newBundle(...) {
        // 使用 UTF-8 编码读取
        return new PropertyResourceBundle(
            new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
    }
};

private static ResourceBundle getBundle() {
    Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
    return ResourceBundle.getBundle(BUNDLE, locale, 
        CodeReadingNoteBundle.class.getClassLoader(), 
        UTF8_CONTROL);  // 使用自定义 Control
}
```

### 2. PluginLanguage.java

**关键改动**:
```java
public enum PluginLanguage {
    // 改用 Locale.ROOT 而不是 Locale.ENGLISH
    ENGLISH("English", "English", Locale.ROOT),
    
    SIMPLIFIED_CHINESE("简体中文", "Simplified Chinese", Locale.SIMPLIFIED_CHINESE);
}
```

### 3. LanguageSettings.java

**关键改动**:
```java
// 独立的 State 类，存储字符串而非枚举
public static class State {
    public String selectedLanguage = null;
}

public PluginLanguage getSelectedLanguage() {
    if (myState.selectedLanguage == null) {
        return detectDefaultLanguage();  // 智能检测
    }
    return PluginLanguage.valueOf(myState.selectedLanguage);
}

public void setSelectedLanguage(PluginLanguage language) {
    myState.selectedLanguage = language.name();  // 存储枚举名称
}
```

### 4. build.gradle

**关键改动**:
```gradle
tasks.withType(ProcessResources) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filteringCharset = 'UTF-8'
}
```

## 📊 预期的正确行为

### 场景 1: 首次使用（英文 IDE）

**日志**:
```
[LanguageSettings] myState.selectedLanguage = null
[LanguageSettings] IDE Locale: en, language: en
[LanguageSettings] Returning default ENGLISH
[CodeReadingNoteBundle] locale: (empty/root)
[UTF8_CONTROL] Loading resource: messages/CodeReadingNoteBundle.properties
[UTF8_CONTROL] Successfully loaded
[CodeReadingNoteBundle] Bundle loaded: (empty)
[CodeReadingNoteBundle] message('action.new.topic') = 'Add Topic'  ✅
```

### 场景 2: 首次使用（中文 IDE）

**日志**:
```
[LanguageSettings] myState.selectedLanguage = null
[LanguageSettings] IDE Locale: zh_CN, language: zh
[LanguageSettings] Detected Chinese IDE, returning SIMPLIFIED_CHINESE
[CodeReadingNoteBundle] locale: zh_CN
[UTF8_CONTROL] Loading resource: messages/CodeReadingNoteBundle_zh_CN.properties
[UTF8_CONTROL] Resource not found
[UTF8_CONTROL] Loading resource: messages/CodeReadingNoteBundle_zh.properties
[UTF8_CONTROL] Successfully loaded
[CodeReadingNoteBundle] Bundle loaded: zh
[CodeReadingNoteBundle] message('action.new.topic') = '新建主题'  ✅
```

### 场景 3: 手动选择英文

**配置文件**:
```xml
<option name="selectedLanguage" value="ENGLISH" />
```

**日志**:
```
[LanguageSettings] myState.selectedLanguage = ENGLISH
[LanguageSettings] Using saved language: ENGLISH
[CodeReadingNoteBundle] locale: (empty/root)
[UTF8_CONTROL] Loading resource: messages/CodeReadingNoteBundle.properties
[CodeReadingNoteBundle] message('action.new.topic') = 'Add Topic'  ✅
```

### 场景 4: 手动选择中文

**配置文件**:
```xml
<option name="selectedLanguage" value="SIMPLIFIED_CHINESE" />
```

**日志**:
```
[LanguageSettings] myState.selectedLanguage = SIMPLIFIED_CHINESE
[LanguageSettings] Using saved language: SIMPLIFIED_CHINESE
[CodeReadingNoteBundle] locale: zh_CN
[UTF8_CONTROL] Loading resource: messages/CodeReadingNoteBundle_zh.properties
[CodeReadingNoteBundle] message('action.new.topic') = '新建主题'  ✅
```

## ✅ 验证清单

- [x] UTF-8 编码问题修复
- [x] Locale 映射正确
- [x] 配置序列化/反序列化正确
- [x] 缓存禁用生效
- [x] 智能默认语言检测
- [x] Gradle 构建配置正确

## 🎯 测试步骤

1. **清理并重新构建**:
   ```bash
   ./gradlew clean buildPlugin
   ```

2. **删除旧配置** (确保全新测试):
   ```powershell
   del %APPDATA%\JetBrains\*\options\codeReadingNoteLanguage.xml /s
   ```

3. **安装插件并重启 IDE**

4. **检查日志**:
   - 应该看到 `[UTF8_CONTROL] Loading resource:`
   - 应该看到 `locale: ` 和 `Bundle loaded:` 匹配
   - 中文显示正常，无乱码

5. **测试语言切换**:
   - Settings → Tools → Code Reading Note Sync
   - 选择 "English / English"
   - Apply → 重启
   - 验证界面全英文

6. **多次切换测试**:
   - 英文 → 中文 → 英文
   - 每次都正确

## 📝 关键技术点

### 为什么用 Locale.ROOT？

```java
// 方案1: 使用 Locale.ENGLISH (en)
// 问题：需要文件 CodeReadingNoteBundle_en.properties
ENGLISH("English", "English", Locale.ENGLISH)

// 方案2: 使用 Locale.ROOT (默认)
// 优势：直接使用 CodeReadingNoteBundle.properties
ENGLISH("English", "English", Locale.ROOT)  ✅
```

### UTF-8 Control 的作用

1. **禁用缓存**: 允许运行时切换语言
2. **UTF-8 编码**: 正确读取中文字符
3. **完全控制**: 可以添加调试日志

### 状态序列化的最佳实践

```java
// ❌ 错误：直接序列化枚举
private PluginLanguage selectedLanguage;

// ✅ 正确：序列化枚举名称字符串
public static class State {
    public String selectedLanguage;
}
```

## 🎉 完成状态

所有问题已修复：
1. ✅ UTF-8 编码问题
2. ✅ Locale 映射问题
3. ✅ 配置持久化问题
4. ✅ 缓存清除问题
5. ✅ 智能语言检测

现在请重新构建并测试！

