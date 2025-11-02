# 国际化支持 / Internationalization Support

## 概述 / Overview

Code Reading Mark Note Pro 插件现已支持国际化（i18n），可以根据用户的IDE语言设置自动切换界面语言。

The Code Reading Mark Note Pro plugin now supports internationalization (i18n) and automatically switches interface language based on the user's IDE language settings.

## 支持的语言 / Supported Languages

- **English** (默认 / Default)
- **简体中文** (Simplified Chinese)

## 如何切换语言 / How to Switch Languages

### 方式 1: 插件独立语言设置（推荐）/ Method 1: Plugin Independent Language (Recommended)

**v3.5.0+ 新功能！** 插件现在支持独立的语言设置，无需更改 IDE 语言：

**New in v3.5.0+!** The plugin now supports independent language settings without changing IDE language:

1. 打开设置 / Open Settings: `File` → `Settings` (Windows/Linux) 或 `IntelliJ IDEA` → `Preferences` (macOS)
2. 导航到 / Navigate to: `Tools` → `Code Reading Note Sync`
3. 在顶部找到 **"Plugin Language"** 选项 / Find **"Plugin Language"** option at the top
4. 选择语言 / Select language: `English` 或 `简体中文`
5. 点击 `Apply` 或 `OK` / Click `Apply` or `OK`
6. 重启 IDE / Restart the IDE

**智能默认** / **Smart Default**: 首次使用时，插件会根据 IDE 语言自动选择（中文 IDE → 中文，其他 → 英文）
First time use: plugin auto-selects based on IDE language (Chinese IDE → Chinese, others → English)

详细说明请查看：[语言设置指南](LANGUAGE_SETTINGS_GUIDE.md)

For detailed instructions, see: [Language Settings Guide](LANGUAGE_SETTINGS_GUIDE.md)

### 方式 2: 切换 IDE 语言 / Method 2: Change IDE Language

切换 IDE 语言后，插件语言保持您在设置中选择的语言不变：

After changing IDE language, plugin language remains what you selected in settings:

**提示** / **Tip**: 如果想让插件跟随 IDE 语言，只需在每次更换 IDE 语言后，在插件设置中手动调整一次即可。

If you want plugin to follow IDE language, simply adjust it once in plugin settings after changing IDE language.

## 技术实现 / Technical Implementation

### 资源文件 / Resource Files

国际化文本存储在以下文件中：

Internationalized text is stored in the following files:

```
src/main/resources/messages/
├── CodeReadingNoteBundle.properties       # 英文 (默认) / English (default)
└── CodeReadingNoteBundle_zh.properties    # 简体中文 / Simplified Chinese
```

### 使用方法 / Usage

在代码中使用 `CodeReadingNoteBundle` 类来获取国际化文本：

Use the `CodeReadingNoteBundle` class in code to retrieve internationalized text:

```java
// 简单文本 / Simple text
String text = CodeReadingNoteBundle.message("action.new.topic");

// 带参数的文本 / Text with parameters
String text = CodeReadingNoteBundle.message("renderer.lines.count", lineCount);
```

### 国际化覆盖范围 / Internationalization Coverage

所有用户界面文本已国际化，包括：

All user interface text has been internationalized, including:

- ✅ Action 按钮和菜单项 / Action buttons and menu items
- ✅ 对话框标题和消息 / Dialog titles and messages
- ✅ UI 面板标签 / UI panel labels
- ✅ 树视图显示文本 / Tree view display text
- ✅ 错误和提示信息 / Error and notification messages
- ✅ 同步功能消息 / Sync feature messages

## 添加新语言 / Adding New Languages

要添加新的语言支持：

To add support for a new language:

1. 创建新的资源文件 / Create a new resource file:
   ```
   src/main/resources/messages/CodeReadingNoteBundle_[语言代码].properties
   ```
   例如 / For example:
   - 日语 / Japanese: `CodeReadingNoteBundle_ja.properties`
   - 韩语 / Korean: `CodeReadingNoteBundle_ko.properties`
   - 法语 / French: `CodeReadingNoteBundle_fr.properties`

2. 复制 `CodeReadingNoteBundle.properties` 的所有键值对到新文件中

   Copy all key-value pairs from `CodeReadingNoteBundle.properties` to the new file

3. 翻译所有值为目标语言

   Translate all values to the target language

4. 测试新语言支持

   Test the new language support

## 资源文件结构 / Resource File Structure

资源文件按功能分组：

Resource files are organized by functionality:

```properties
# Actions - 操作按钮和菜单
action.new.topic=New Topic
action.rename.topic=Rename Topic
...

# Dialogs - 对话框
dialog.create.topic.title=Create New Topic
dialog.create.topic.message=Enter Topic name
...

# Panel Labels - 面板标签
panel.select.topic=Select Topic
panel.select.group=Select Group (Optional)
...

# Tree View - 树视图
tree.view.tab=Tree View
tree.ungrouped.lines=Ungrouped Lines
...

# Messages - 消息和提示
message.export.failed=Fail to save. Please try again.
message.push.successful.title=Push Successful
...

# Progress - 进度提示
progress.pushing=Pushing Notes
progress.pulling=Pulling Notes
...

# Buttons - 按钮
button.add.to.topic=Add to Topic
button.cancel=Cancel
...

# List Renderer - 列表渲染
renderer.lines.count={0} lines
renderer.date.format=yyyy/MM/dd HH:mm
...
```

## 参数化消息 / Parameterized Messages

支持使用 `{0}`, `{1}` 等占位符：

Placeholders like `{0}`, `{1}` are supported:

```properties
# 英文 / English
renderer.lines.count={0} lines
message.sync.config.error=Sync configuration incomplete: {0}

# 中文 / Chinese
renderer.lines.count={0} 行
message.sync.config.error=同步配置不完整：{0}
```

使用示例 / Usage example:

```java
String message = CodeReadingNoteBundle.message("renderer.lines.count", 5);
// 英文输出 / English output: "5 lines"
// 中文输出 / Chinese output: "5 行"
```

## 注意事项 / Notes

1. 所有资源文件必须使用 UTF-8 编码

   All resource files must use UTF-8 encoding

2. 键名使用小写字母和点号分隔

   Key names use lowercase letters and dots as separators

3. 保持所有语言文件的键保持一致

   Keep keys consistent across all language files

4. 特殊字符需要转义（例如 `:`、`=`、`\`）

   Special characters need to be escaped (e.g., `:`, `=`, `\`)

5. 换行符使用 `\n`

   Use `\n` for line breaks

## 贡献 / Contributing

欢迎贡献新的语言翻译！请提交 Pull Request 并包含：

Contributions for new language translations are welcome! Please submit a Pull Request including:

1. 新的资源文件 / New resource file
2. 更新后的文档 / Updated documentation
3. 测试截图（可选）/ Test screenshots (optional)

## 反馈 / Feedback

如果发现翻译问题或有改进建议，请在 GitHub 上提交 Issue。

If you find translation issues or have suggestions for improvement, please submit an Issue on GitHub.

