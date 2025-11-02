# 语言切换 Bug 修复总结

## 🐛 Bug 描述

**用户反馈**: 不管选择哪个语言，插件都显示中文。

## 🔍 问题分析

### 第一个问题：ResourceBundle 缓存

**症状**: 使用 `DynamicBundle` 继承，但运行时无法切换语言。

**原因**: 
- `DynamicBundle` 在构造时就确定了 Locale
- `ResourceBundle` 有内置缓存机制

**解决方案**: 
- 放弃继承 `DynamicBundle`，直接使用 `ResourceBundle` API
- 使用自定义 `Control` 禁用缓存（`TTL_DONT_CACHE`）

### 第二个问题：枚举序列化失败

**症状**: 设置保存后，重启 IDE 配置丢失。

**原因**: 
- 直接序列化枚举对象可能失败
- `XmlSerializerUtil.copyBean()` 对枚举类型的支持不完善

**解决方案**: 
- 创建独立的 `State` 类
- 将枚举存储为字符串（`language.name()`）
- 加载时使用 `PluginLanguage.valueOf()` 转换回枚举

## 🔧 关键修复

### 1. CodeReadingNoteBundle.java

**修改前**:
```java
public class CodeReadingNoteBundle extends DynamicBundle {
    private CodeReadingNoteBundle() {
        super(BUNDLE);
    }
    
    @Override
    protected ResourceBundle findBundle(...) {
        Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
        return ResourceBundle.getBundle(...);
    }
}
```

**修改后**:
```java
public final class CodeReadingNoteBundle {
    // 禁用缓存
    private static final ResourceBundle.Control NO_CACHE_CONTROL = 
        new ResourceBundle.Control() {
            @Override
            public long getTimeToLive(String baseName, Locale locale) {
                return ResourceBundle.Control.TTL_DONT_CACHE;
            }
        };
    
    private static ResourceBundle getBundle() {
        Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
        return ResourceBundle.getBundle(BUNDLE, locale, 
            CodeReadingNoteBundle.class.getClassLoader(), 
            NO_CACHE_CONTROL);  // 使用不缓存的 Control
    }
}
```

### 2. LanguageSettings.java

**修改前**:
```java
public class LanguageSettings implements PersistentStateComponent<LanguageSettings> {
    private PluginLanguage selectedLanguage = null;
    
    @Override
    public LanguageSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull LanguageSettings state) {
        XmlSerializerUtil.copyBean(state, this);  // 可能失败
    }
}
```

**修改后**:
```java
public class LanguageSettings implements PersistentStateComponent<LanguageSettings.State> {
    // 独立的状态类，存储字符串而非枚举
    public static class State {
        public String selectedLanguage = null;
    }
    
    private State myState = new State();
    
    @Override
    public State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
    
    public PluginLanguage getSelectedLanguage() {
        if (myState.selectedLanguage == null) {
            return detectDefaultLanguage();
        }
        return PluginLanguage.valueOf(myState.selectedLanguage);
    }
    
    public void setSelectedLanguage(PluginLanguage language) {
        if (language != null) {
            myState.selectedLanguage = language.name();  // 存储字符串
        }
    }
}
```

## 📋 配置文件格式

### 正确的格式

```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
```

或

```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="SIMPLIFIED_CHINESE" />
  </component>
</application>
```

### 错误的格式（如果直接存枚举）

```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <selectedLanguage>ENGLISH</selectedLanguage>  <!-- 错误 -->
  </component>
</application>
```

## 🧪 调试功能

为了帮助诊断问题，添加了详细的调试日志：

### LanguageSettings 日志

```java
public PluginLanguage getSelectedLanguage() {
    System.out.println("[LanguageSettings] getSelectedLanguage called");
    System.out.println("[LanguageSettings] myState.selectedLanguage = " + myState.selectedLanguage);
    
    if (myState.selectedLanguage == null) {
        PluginLanguage detected = detectDefaultLanguage();
        System.out.println("[LanguageSettings] First use, detected language: " + detected);
        return detected;
    }
    
    PluginLanguage lang = PluginLanguage.valueOf(myState.selectedLanguage);
    System.out.println("[LanguageSettings] Using saved language: " + lang);
    return lang;
}
```

### CodeReadingNoteBundle 日志

```java
private static ResourceBundle getBundle() {
    Locale locale = LanguageSettings.getInstance().getEffectiveLocale();
    System.out.println("[CodeReadingNoteBundle] getBundle() called, locale: " + locale);
    ResourceBundle bundle = ResourceBundle.getBundle(...);
    System.out.println("[CodeReadingNoteBundle] Bundle loaded: " + bundle.getLocale());
    return bundle;
}

public static String message(String key, Object... params) {
    ResourceBundle bundle = getBundle();
    String value = bundle.getString(key);
    System.out.println("[CodeReadingNoteBundle] message('" + key + "') = '" + value + "'");
    return value;
}
```

## 📊 测试场景

### 场景 1: 首次安装（中文 IDE）

**预期行为**:
1. 检测到 IDE 是中文
2. 自动选择中文
3. 插件显示中文

**调试日志**:
```
[LanguageSettings] myState.selectedLanguage = null
[LanguageSettings] IDE Locale: zh_CN, language: zh
[LanguageSettings] Detected Chinese IDE, returning SIMPLIFIED_CHINESE
[CodeReadingNoteBundle] locale: zh_CN
[CodeReadingNoteBundle] message('action.topic.add') = '添加主题'
```

### 场景 2: 中文 IDE + 选择英文

**操作**:
1. 打开设置
2. 选择 "English / English"
3. 点击 Apply
4. 重启 IDE

**预期行为**:
1. 配置保存为 "ENGLISH"
2. 重启后加载配置
3. 插件显示英文

**调试日志**:
```
# 保存时
[LanguageSettings] setSelectedLanguage called with: ENGLISH
[LanguageSettings] myState.selectedLanguage set to: ENGLISH

# 重启后
[LanguageSettings] myState.selectedLanguage = ENGLISH
[LanguageSettings] Using saved language: ENGLISH
[CodeReadingNoteBundle] locale: en
[CodeReadingNoteBundle] message('action.topic.add') = 'Add Topic'
```

## 🎯 验证清单

请按照以下清单验证修复是否成功：

### 构建验证
- [ ] 代码编译无错误
- [ ] 资源文件包含在 JAR 中
  ```bash
  jar tf build/distributions/Code*.zip | grep properties
  # 应该看到:
  # messages/CodeReadingNoteBundle.properties
  # messages/CodeReadingNoteBundle_zh.properties
  ```

### 功能验证
- [ ] 首次安装：中文 IDE → 插件自动中文
- [ ] 首次安装：英文 IDE → 插件自动英文
- [ ] 切换到英文：设置生效，界面全英文
- [ ] 切换到中文：设置生效，界面全中文
- [ ] 多次切换：每次都正确

### 配置验证
- [ ] 配置文件正确生成
- [ ] 配置内容格式正确
- [ ] 重启后配置正确加载

### 日志验证
- [ ] 能看到 [LanguageSettings] 日志
- [ ] 能看到 [CodeReadingNoteBundle] 日志
- [ ] 日志显示的 locale 和 message 值正确

## 🔍 如果仍然有问题

### 检查点 1: 配置是否保存

**检查**: 选择语言后，查看配置文件是否生成

**位置**:
- Windows: `%APPDATA%\JetBrains\<IDE-Version>\options\codeReadingNoteLanguage.xml`
- macOS/Linux: `~/.config/JetBrains/<IDE-Version>/options/codeReadingNoteLanguage.xml`

**如果文件不存在**: `saveTo()` 方法可能没有被调用

**如果文件存在但内容错误**: 序列化有问题

### 检查点 2: 配置是否加载

**检查**: 重启后查看日志

**正常日志**:
```
[LanguageSettings] myState.selectedLanguage = ENGLISH
```

**异常日志**:
```
[LanguageSettings] myState.selectedLanguage = null
```

**如果是 null**: `loadState()` 可能没有被调用，或 XML 反序列化失败

### 检查点 3: Locale 是否正确

**检查**: 查看 Bundle 日志

**正常日志**:
```
[CodeReadingNoteBundle] locale: en
[CodeReadingNoteBundle] Bundle loaded: en
```

**异常日志**:
```
[CodeReadingNoteBundle] locale: en
[CodeReadingNoteBundle] Bundle loaded: zh_CN  <-- 不匹配
```

**如果不匹配**: ResourceBundle fallback 机制触发，可能是资源文件问题

### 检查点 4: 消息是否正确

**检查**: 查看 message 日志

**正常日志（选择英文时）**:
```
[CodeReadingNoteBundle] message('action.topic.add') = 'Add Topic'
```

**异常日志（选择英文但显示中文）**:
```
[CodeReadingNoteBundle] message('action.topic.add') = '添加主题'
```

**如果不正确**: 资源文件内容有问题

## 📞 反馈信息

如果问题仍然存在，请提供：

1. **完整的日志输出** （所有包含 [LanguageSettings] 和 [CodeReadingNoteBundle] 的行）

2. **配置文件内容**
   ```bash
   cat <path>/codeReadingNoteLanguage.xml
   ```

3. **IDE 信息**
   - IDE 版本
   - IDE 语言设置
   - 操作系统

4. **操作步骤**
   - 你做了什么
   - 预期结果
   - 实际结果

5. **截图**
   - 设置页面
   - 插件界面

---

**相关文档**:
- [DEBUG_INSTRUCTIONS.md](DEBUG_INSTRUCTIONS.md) - 详细调试步骤
- [LANGUAGE_SWITCH_TEST.md](LANGUAGE_SWITCH_TEST.md) - 测试场景
- [LANGUAGE_IMPLEMENTATION_SUMMARY.md](LANGUAGE_IMPLEMENTATION_SUMMARY.md) - 技术实现

