# 插件独立语言切换功能 / Plugin Independent Language Switch

## 功能概述 / Feature Overview

**Code Reading Mark Note Pro** 现在支持独立的语言切换功能！您可以为插件设置不同于 IDE 的显示语言，无需修改整个 IDE 的语言设置。

**Code Reading Mark Note Pro** now supports independent language switching! You can set a different display language for the plugin without changing the entire IDE's language settings.

## 使用方法 / How to Use

### 访问语言设置 / Access Language Settings

1. 打开 IDE 设置 / Open IDE Settings:
   - Windows/Linux: `File` → `Settings`
   - macOS: `IntelliJ IDEA` → `Preferences`

2. 导航到插件设置 / Navigate to plugin settings:
   ```
   Tools → Code Reading Note Sync
   ```

3. 在设置页面顶部找到 **"Plugin Language"** 选项 / Find the **"Plugin Language"** option at the top of the settings page

### 语言选项 / Language Options

插件提供两种语言选项：

The plugin provides two language options:

| 选项 / Option | 说明 / Description |
|--------------|-------------------|
| **English** | 使用英文界面<br>Use English interface |
| **简体中文** / **Simplified Chinese** | 使用简体中文界面<br>Use Simplified Chinese interface |

**智能默认** / **Smart Default**: 
- 首次使用时，如果 IDE 是中文，默认选择中文
- 如果 IDE 是其他语言，默认选择英文
- First time use: if IDE is Chinese, default to Chinese
- If IDE is other language, default to English

### 应用更改 / Apply Changes

1. 选择您想要的语言 / Select your preferred language
2. 点击 `Apply` 或 `OK` 按钮 / Click the `Apply` or `OK` button
3. **重启 IDE** 使更改生效 / **Restart the IDE** for changes to take effect

⚠️ **注意 / Note**: 语言更改需要重启 IDE 才能完全生效 / Language changes require an IDE restart to take full effect

## 使用场景 / Use Cases

### 场景 1: 中文 IDE + 英文插件
**Scenario 1: Chinese IDE + English Plugin**

如果您使用中文版 IDE，但希望练习英语或更熟悉英文技术术语：

If you use Chinese IDE but want to practice English or prefer English technical terms:

1. IDE 保持中文设置 / Keep IDE in Chinese
2. 插件设置为 `English`
3. 重启后，IDE 界面是中文，但插件是英文 / After restart, IDE is in Chinese but plugin is in English

### 场景 2: 英文 IDE + 中文插件
**Scenario 2: English IDE + Chinese Plugin**

如果您的 IDE 是英文，但更习惯用中文阅读笔记工具：

If your IDE is in English but you prefer Chinese for note-taking tools:

1. IDE 保持英文设置 / Keep IDE in English
2. 插件设置为 `简体中文` / Set plugin to `Simplified Chinese`
3. 重启后，IDE 界面是英文，但插件是中文 / After restart, IDE is in English but plugin is in Chinese

### 场景 3: 团队协作
**Scenario 3: Team Collaboration**

在多语言团队中，每个成员可以根据自己的语言偏好设置插件：

In multilingual teams, each member can set the plugin according to their language preference:

- 不影响项目配置 / Does not affect project configuration
- 个人设置独立保存 / Personal settings saved independently
- 切换灵活，不干扰他人 / Flexible switching without affecting others

## 技术细节 / Technical Details

### 配置存储 / Configuration Storage

语言设置保存在应用级别（Application Level），位置：

Language settings are saved at the application level, location:

```
~/.config/JetBrains/[IDE-Version]/options/codeReadingNoteLanguage.xml
```

### 默认行为 / Default Behavior

- **首次安装** / **First Installation**: 默认跟随 IDE 语言 / Defaults to follow IDE language
- **升级插件** / **Plugin Upgrade**: 保留之前的语言设置 / Retains previous language settings

### 支持的组件 / Supported Components

独立语言设置适用于插件的所有组件：

Independent language settings apply to all plugin components:

- ✅ Actions（操作菜单）
- ✅ Dialogs（对话框）
- ✅ Settings（设置界面）
- ✅ Tool Window（工具窗口）
- ✅ Notifications（通知）
- ✅ Error Messages（错误消息）
- ✅ Context Menus（上下文菜单）
- ✅ Tree Views（树视图）

## 常见问题 / FAQ

### Q1: 更改语言后为什么没有生效？
**Q1: Why didn't the language change take effect?**

**A**: 语言更改需要重启 IDE。请完全关闭 IDE 并重新打开。

**A**: Language changes require an IDE restart. Please completely close the IDE and reopen it.

### Q2: 可以在不重启的情况下切换语言吗？
**Q2: Can I switch languages without restarting?**

**A**: 目前不支持热切换。这是 IntelliJ Platform 资源加载机制的限制，需要重启才能重新加载资源文件。

**A**: Hot-swapping is not currently supported. This is a limitation of IntelliJ Platform's resource loading mechanism, which requires a restart to reload resource files.

### Q3: 首次安装时插件如何决定使用哪种语言？
**Q3: How does the plugin decide which language to use on first installation?**

**A**: 插件会检测 IDE 的语言设置：
- 如果 IDE 语言是中文（zh），默认使用简体中文
- 其他情况默认使用英文
- 之后可以随时在设置中手动修改

**A**: The plugin detects the IDE's language settings:
- If IDE language is Chinese (zh), default to Simplified Chinese
- Otherwise, default to English
- You can manually change it anytime in settings

### Q4: 这会影响我的项目配置吗？
**Q4: Will this affect my project configuration?**

**A**: 不会。语言设置是个人偏好，存储在应用级别，不会影响项目配置或团队成员的设置。

**A**: No. Language settings are personal preferences stored at the application level and do not affect project configuration or team members' settings.

### Q5: 支持其他语言吗？
**Q5: Are other languages supported?**

**A**: 当前支持英文和简体中文。未来可以根据需求添加更多语言支持（如日语、韩语等）。

**A**: Currently supports English and Simplified Chinese. More language support (such as Japanese, Korean, etc.) can be added based on demand in the future.

## 设置位置截图参考 / Settings Location Reference

```
Settings / Preferences
└── Tools
    └── Code Reading Note Sync
        ├── Plugin Language:  [Dropdown Menu]     <-- 语言选择在这里
        │   ├── English / English
        │   └── 简体中文 / Simplified Chinese
        ├── Note: Language changes will take effect after restarting the IDE.
        │
        ├── ─────────────────────────────
        │
        ├── Enable Sync
        ├── Sync Provider: [Dropdown]
        └── ...
```

## 反馈 / Feedback

如果您在使用语言切换功能时遇到问题，或有改进建议，请在 GitHub 上提交 Issue。

If you encounter issues using the language switching feature or have suggestions for improvement, please submit an Issue on GitHub.

---

**版本要求 / Version Requirements**: Code Reading Mark Note Pro v3.5.0+

**兼容性 / Compatibility**: IntelliJ IDEA 2024.3+

