# 插件名称修复说明

## 问题
插件在IDE中显示的工具窗口标题为 "Code Reading mark Note pro"（mark是小写的），与期望的 "Code Reading Mark Note Pro" 不一致。

## 修复内容

### 1. 修改 `plugin.xml` 中的工具窗口ID
**文件**: `src/main/resources/META-INF/plugin.xml`

**修改前**:
```xml
<toolWindow icon="MyIcons.PLUGIN" id="Code Reading mark Note pro" anchor="bottom" ... />
```

**修改后**:
```xml
<toolWindow icon="MyIcons.PLUGIN" id="Code Reading Mark Note Pro" anchor="bottom" ... />
```

### 2. 统一版本号
同时更新了 `plugin.xml` 中的版本号，使其与 `build.gradle` 保持一致：
- `build.gradle`: version '3.2.0'
- `plugin.xml`: <version>3.2.0</version>

## 当前配置

### 插件名称（plugin.xml）
```xml
<name>Code Reading Mark Note Pro</name>
```

### 工具窗口标题（plugin.xml）
```xml
<toolWindow id="Code Reading Mark Note Pro" ... />
```

### 版本号
- `build.gradle`: `3.2.0`
- `plugin.xml`: `3.2.0`

## 验证
修改后，插件在IntelliJ IDEA中的显示应该为：
- 插件市场名称: **Code Reading Mark Note Pro**
- 工具窗口标题: **Code Reading Mark Note Pro**
- 右键菜单项: **Add to Topic**

## 注意事项
修改工具窗口ID后：
1. 需要重新构建插件：`gradlew buildPlugin`
2. 用户如果已经安装了旧版本，可能需要重启IDE才能看到新的工具窗口标题
3. 现有用户的工具窗口位置设置可能会重置（因为ID改变了）

## 发布清单
在发布新版本前，请确认：
- [x] `plugin.xml` 中的 `<name>` 正确：Code Reading Mark Note Pro
- [x] `plugin.xml` 中的 `<toolWindow id>` 正确：Code Reading Mark Note Pro
- [x] `build.gradle` 和 `plugin.xml` 版本号一致：3.2.0
- [ ] 更新 `changeNotes.html`，说明此次版本的更新内容
- [ ] 构建插件：`gradlew buildPlugin`
- [ ] 测试安装和功能
- [ ] 发布到 JetBrains Marketplace

