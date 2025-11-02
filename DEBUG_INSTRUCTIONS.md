# 语言切换调试说明

## 🐛 问题症状

用户报告：不管选择哪个语言，插件都显示中文。

## 🔍 已添加的调试日志

我已经在关键位置添加了详细的调试日志，帮助诊断问题。

### 1. LanguageSettings 日志

**位置**: `src/main/java/.../settings/LanguageSettings.java`

**输出的日志**:
```
[LanguageSettings] getSelectedLanguage called
[LanguageSettings] myState.selectedLanguage = <值>
[LanguageSettings] First use, detected language: <语言>
[LanguageSettings] Using saved language: <语言>
[LanguageSettings] setSelectedLanguage called with: <语言>
[LanguageSettings] myState.selectedLanguage set to: <值>
[LanguageSettings] IDE Locale: <locale>, language: <lang>
[LanguageSettings] Detected Chinese IDE, returning SIMPLIFIED_CHINESE
[LanguageSettings] Returning default ENGLISH
```

### 2. CodeReadingNoteBundle 日志

**位置**: `src/main/java/.../CodeReadingNoteBundle.java`

**输出的日志**:
```
[CodeReadingNoteBundle] getBundle() called, locale: <locale>
[CodeReadingNoteBundle] Bundle loaded: <locale>
[CodeReadingNoteBundle] message('<key>') = '<value>'
```

## 📝 测试步骤

### 步骤 1: 清理旧配置

**Windows**:
```powershell
# 找到配置文件
cd %APPDATA%\JetBrains
dir /s codeReadingNoteLanguage.xml

# 删除旧配置（如果存在）
del <找到的路径>\codeReadingNoteLanguage.xml
```

**macOS/Linux**:
```bash
# 找到配置文件
find ~/.config/JetBrains -name "codeReadingNoteLanguage.xml"

# 删除旧配置（如果存在）
rm <找到的路径>/codeReadingNoteLanguage.xml
```

### 步骤 2: 重新构建插件

```bash
# 清理并重新构建
./gradlew clean buildPlugin

# 或者在 Windows PowerShell
.\gradlew.bat clean buildPlugin
```

### 步骤 3: 安装插件

1. 打开 IDE
2. File → Settings → Plugins
3. 点击齿轮图标 → Install Plugin from Disk
4. 选择 `build/distributions/Code Reading Mark Note Pro-3.5.0.zip`
5. 重启 IDE

### 步骤 4: 查看首次启动日志

**打开 IDE 日志窗口**:
- Help → Show Log in Explorer (Windows)
- Help → Show Log in Finder (macOS)
- 或者直接在 IDE 中: Help → Diagnostic Tools → Debug Log Settings

**查找关键日志**:
在 `idea.log` 文件中搜索：
```
[LanguageSettings]
[CodeReadingNoteBundle]
```

**预期日志（首次启动）**:
```
[LanguageSettings] getSelectedLanguage called
[LanguageSettings] myState.selectedLanguage = null
[LanguageSettings] IDE Locale: zh_CN, language: zh
[LanguageSettings] Detected Chinese IDE, returning SIMPLIFIED_CHINESE
[LanguageSettings] First use, detected language: SIMPLIFIED_CHINESE
```

或者（如果是英文 IDE）:
```
[LanguageSettings] getSelectedLanguage called
[LanguageSettings] myState.selectedLanguage = null
[LanguageSettings] IDE Locale: en_US, language: en
[LanguageSettings] Returning default ENGLISH
[LanguageSettings] First use, detected language: ENGLISH
```

### 步骤 5: 打开设置并选择语言

1. 打开 Settings → Tools → Code Reading Note Sync
2. 查看 "Plugin Language" 当前选择的是什么
3. 选择 "English / English"
4. 点击 Apply

**预期日志**:
```
[LanguageSettings] setSelectedLanguage called with: ENGLISH
[LanguageSettings] myState.selectedLanguage set to: ENGLISH
```

### 步骤 6: 检查配置文件

**Windows**:
```powershell
type %APPDATA%\JetBrains\<IDE-Version>\options\codeReadingNoteLanguage.xml
```

**macOS/Linux**:
```bash
cat ~/.config/JetBrains/<IDE-Version>/options/codeReadingNoteLanguage.xml
```

**预期内容**:
```xml
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
```

### 步骤 7: 重启 IDE 并验证

1. 重启 IDE
2. 打开工具窗口 "Code Reading Mark Note Pro"

**预期日志（重启后）**:
```
[LanguageSettings] getSelectedLanguage called
[LanguageSettings] myState.selectedLanguage = ENGLISH
[LanguageSettings] Using saved language: ENGLISH
[CodeReadingNoteBundle] getBundle() called, locale: en
[CodeReadingNoteBundle] Bundle loaded: en
[CodeReadingNoteBundle] message('action.topic.add') = 'Add Topic'
```

如果仍然显示中文，日志应该是：
```
[LanguageSettings] getSelectedLanguage called
[LanguageSettings] myState.selectedLanguage = ENGLISH
[LanguageSettings] Using saved language: ENGLISH
[CodeReadingNoteBundle] getBundle() called, locale: en
[CodeReadingNoteBundle] Bundle loaded: zh_CN    <-- 问题！应该是 en
[CodeReadingNoteBundle] message('action.topic.add') = '添加主题'  <-- 问题！应该是英文
```

## 🔬 诊断问题

根据日志输出，可以判断问题所在：

### 情况 1: selectedLanguage 一直是 null

**日志特征**:
```
[LanguageSettings] myState.selectedLanguage = null
```
即使在设置中选择了语言并点击了 Apply。

**可能原因**:
- `setSelectedLanguage()` 没有被调用
- 配置没有正确保存

**解决方法**:
检查 `SyncSettingsPanel.saveTo()` 是否被正确调用。

### 情况 2: selectedLanguage 保存了，但加载时是 null

**日志特征**:
```
# 保存时
[LanguageSettings] myState.selectedLanguage set to: ENGLISH

# 重启后
[LanguageSettings] myState.selectedLanguage = null
```

**可能原因**:
- XML 序列化/反序列化有问题
- 配置文件没有正确生成

**解决方法**:
检查配置文件是否存在，内容是否正确。

### 情况 3: selectedLanguage 正确，但 Bundle 加载了错误的 Locale

**日志特征**:
```
[LanguageSettings] Using saved language: ENGLISH
[CodeReadingNoteBundle] getBundle() called, locale: en
[CodeReadingNoteBundle] Bundle loaded: zh_CN   <-- 不匹配！
```

**可能原因**:
- ResourceBundle fallback 机制（找不到 en 文件，回退到默认）
- 资源文件路径问题

**解决方法**:
检查 `CodeReadingNoteBundle.properties` 文件是否存在于正确的位置。

### 情况 4: Bundle 加载正确，但返回的是中文

**日志特征**:
```
[CodeReadingNoteBundle] Bundle loaded: en
[CodeReadingNoteBundle] message('action.topic.add') = '添加主题'  <-- 错误！
```

**可能原因**:
- 资源文件内容错误
- 加载了错误的 properties 文件

**解决方法**:
检查 `CodeReadingNoteBundle.properties` 文件内容。

## 📊 收集信息

如果问题仍然存在，请提供以下信息：

1. **IDE 版本和语言设置**
   - IDE: IntelliJ IDEA 202X.X
   - IDE 语言: 中文 / 英文

2. **配置文件内容**
   ```bash
   # 找到并复制配置文件内容
   ```

3. **关键日志片段**
   ```
   # 复制所有包含 [LanguageSettings] 和 [CodeReadingNoteBundle] 的日志
   ```

4. **资源文件检查**
   ```bash
   # 在构建的 JAR 中检查资源文件
   jar tf build/distributions/Code*.zip | grep properties
   ```
   
   应该能看到：
   ```
   messages/CodeReadingNoteBundle.properties
   messages/CodeReadingNoteBundle_zh.properties
   ```

5. **实际显示的文字**
   - 哪些地方显示中文？
   - 哪些地方显示英文？
   - 截图

## 🎯 快速测试方法

如果想快速测试，可以手动创建配置文件：

**Windows**:
```powershell
# 创建配置目录（如果不存在）
mkdir "%APPDATA%\JetBrains\IntelliJIdea2024.3\options" -Force

# 创建配置文件
@"
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
"@ | Out-File -Encoding UTF8 "%APPDATA%\JetBrains\IntelliJIdea2024.3\options\codeReadingNoteLanguage.xml"
```

**macOS/Linux**:
```bash
# 创建配置目录（如果不存在）
mkdir -p ~/.config/JetBrains/IntelliJIdea2024.3/options

# 创建配置文件
cat > ~/.config/JetBrains/IntelliJIdea2024.3/options/codeReadingNoteLanguage.xml << 'EOF'
<application>
  <component name="CodeReadingNoteLanguageSettings">
    <option name="selectedLanguage" value="ENGLISH" />
  </component>
</application>
EOF
```

然后重启 IDE，看是否生效。

## ⚡ 临时解决方案

如果调试发现是 `detectDefaultLanguage()` 的问题，可以临时强制返回英文：

```java
private PluginLanguage detectDefaultLanguage() {
    // 临时：总是返回英文
    return PluginLanguage.ENGLISH;
}
```

这样可以排除检测逻辑的问题。

---

**下一步**: 请按照上述步骤测试，并提供日志输出，我们可以根据日志定位具体问题。

