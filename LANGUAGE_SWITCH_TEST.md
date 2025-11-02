# 语言切换功能测试说明

## 核心改进

### 问题诊断
之前的实现依赖 `DynamicBundle`，但该类在首次加载时就缓存了 Locale，导致运行时修改设置无法生效（即使重启 IDE 也可能因为缓存问题无法正确切换）。

### 解决方案
1. **放弃 DynamicBundle**：直接使用 `ResourceBundle` API
2. **禁用缓存**：使用自定义的 `ResourceBundle.Control`，设置 `TTL_DONT_CACHE`
3. **动态加载**：每次调用 `message()` 时，都从 `LanguageSettings` 获取最新的 Locale

## 测试步骤

### 测试场景 1: 首次安装（中文 IDE）

**前提条件**: 
- IDE 语言设置为中文
- 首次安装插件

**预期结果**:
```
1. 安装插件后重启 IDE
2. 打开工具窗口 "Code Reading Mark Note Pro"
3. 界面应显示中文（因为检测到 IDE 是中文）
```

**验证点**:
- 工具窗口标题
- 右键菜单 "添加到主题"
- 按钮文字（添加主题、重命名、删除等）

---

### 测试场景 2: 首次安装（英文 IDE）

**前提条件**: 
- IDE 语言设置为英文
- 首次安装插件

**预期结果**:
```
1. 安装插件后重启 IDE
2. 打开工具窗口 "Code Reading Mark Note Pro"
3. 界面应显示英文（因为检测到 IDE 是英文）
```

---

### 测试场景 3: 中文 IDE + 切换到英文插件

**操作步骤**:
```
1. IDE 保持中文设置
2. 打开 Settings → Tools → Code Reading Note Sync
3. 在 "插件语言" 中选择 "English / English"
4. 点击 Apply 或 OK
5. 重启 IDE
```

**预期结果**:
- 插件所有界面显示英文
- IDE 其他部分仍然是中文
- 设置页面显示 "Plugin Language: English / English"

**验证点**:
- 工具窗口标题
- 右键菜单文字
- 对话框标题和按钮
- 错误提示信息
- 设置页面文字

---

### 测试场景 4: 英文 IDE + 切换到中文插件

**操作步骤**:
```
1. IDE 保持英文设置
2. 打开 Settings → Tools → Code Reading Note Sync
3. 在 "Plugin Language" 中选择 "简体中文 / Simplified Chinese"
4. 点击 Apply 或 OK
5. 重启 IDE
```

**预期结果**:
- 插件所有界面显示中文
- IDE 其他部分仍然是英文
- 设置页面显示 "插件语言：简体中文 / Simplified Chinese"

---

### 测试场景 5: 多次切换

**操作步骤**:
```
1. 从中文切换到英文 → 重启 → 验证
2. 从英文切换到中文 → 重启 → 验证
3. 重复 2-3 次
```

**预期结果**:
- 每次切换后重启，语言都能正确生效
- 不会出现混合显示（一部分中文一部分英文）
- 配置正确保存，下次打开 IDE 仍然保持选择的语言

---

## 调试方法

### 1. 检查配置文件

配置文件位置（Windows）:
```
%APPDATA%\JetBrains\<IDE-Version>\options\codeReadingNoteLanguage.xml
```

配置文件位置（macOS/Linux）:
```
~/.config/JetBrains/<IDE-Version>/options/codeReadingNoteLanguage.xml
```

文件内容示例（选择英文）:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
```

文件内容示例（选择中文）:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="SIMPLIFIED_CHINESE" />
  </component>
</application>
```

文件内容示例（未设置）:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings" />
</application>
```

### 2. 验证 Locale 检测

在 `LanguageSettings.detectDefaultLanguage()` 方法中添加日志：

```java
private PluginLanguage detectDefaultLanguage() {
    try {
        Locale ideLocale = DynamicBundle.getLocale();
        String language = ideLocale.getLanguage();
        System.out.println("IDE Locale detected: " + ideLocale + ", language: " + language);
        
        if ("zh".equals(language)) {
            System.out.println("Defaulting to SIMPLIFIED_CHINESE");
            return PluginLanguage.SIMPLIFIED_CHINESE;
        }
    } catch (Exception e) {
        System.out.println("Failed to detect IDE locale: " + e.getMessage());
        String sysLanguage = Locale.getDefault().getLanguage();
        System.out.println("Fallback to system locale: " + sysLanguage);
        if ("zh".equals(sysLanguage)) {
            return PluginLanguage.SIMPLIFIED_CHINESE;
        }
    }
    
    System.out.println("Defaulting to ENGLISH");
    return PluginLanguage.ENGLISH;
}
```

### 3. 验证资源加载

在 `CodeReadingNoteBundle.message()` 方法中添加调试：

```java
public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    try {
        Locale currentLocale = LanguageSettings.getInstance().getEffectiveLocale();
        System.out.println("Loading message '" + key + "' with locale: " + currentLocale);
        
        ResourceBundle bundle = getBundle();
        String value = bundle.getString(key);
        
        if (params.length > 0) {
            return MessageFormat.format(value, params);
        }
        return value;
    } catch (MissingResourceException e) {
        System.err.println("Missing resource key: " + key);
        return "!" + key + "!";
    }
}
```

---

## 常见问题排查

### Q: 修改设置后重启，语言没有变化

**可能原因**:
1. 配置文件没有正确保存
2. IDE 缓存问题

**解决方法**:
```
1. 检查配置文件是否存在并包含正确的值
2. 尝试 File → Invalidate Caches / Restart → Invalidate and Restart
3. 如果还不行，手动删除配置文件后重启 IDE
```

---

### Q: 显示的是系统语言，不是我选择的语言

**可能原因**:
- `getEffectiveLocale()` 返回了错误的 Locale

**解决方法**:
```java
// 在 LanguageSettings 中添加日志
public Locale getEffectiveLocale() {
    PluginLanguage lang = getSelectedLanguage();
    Locale locale = lang.getLocale();
    System.out.println("getEffectiveLocale: " + lang + " -> " + locale);
    return locale;
}
```

---

### Q: 部分界面是英文，部分是中文

**可能原因**:
- 某些地方没有使用 `CodeReadingNoteBundle.message()`
- 硬编码的字符串

**解决方法**:
```bash
# 在项目中搜索硬编码的中文字符串
grep -r "添加\|删除\|重命名\|主题" src/main/java/

# 在项目中搜索硬编码的英文字符串（常见的）
grep -r '"Add\|"Remove\|"Rename\|"Topic' src/main/java/
```

---

## 关键代码位置

### 配置加载
- `LanguageSettings.getSelectedLanguage()` - 获取用户选择的语言
- `LanguageSettings.detectDefaultLanguage()` - 首次使用时的智能检测

### 资源加载
- `CodeReadingNoteBundle.getBundle()` - 加载正确的资源文件
- `CodeReadingNoteBundle.message()` - 获取国际化文本

### UI 设置
- `SyncSettingsPanel` - 语言选择下拉框
- `PluginLanguage.getDisplayName()` - 下拉框显示的文本

---

## 成功标志

✅ **功能正常的标志**:
1. 设置页面可以看到两个语言选项
2. 选择语言后点击 Apply，配置文件被更新
3. 重启 IDE 后，插件界面使用选择的语言
4. 多次切换都能正常生效
5. IDE 语言和插件语言可以不同

✅ **关键验证点**:
- [ ] 工具窗口标题正确
- [ ] 右键菜单文字正确
- [ ] 对话框标题和按钮正确
- [ ] 设置页面文字正确
- [ ] 树视图节点文字正确
- [ ] 错误/提示消息正确
- [ ] 同步功能消息正确

