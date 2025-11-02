# 同步配置保存问题修复

## 问题描述

用户反馈：配置了同步设置后，点击 Apply 保存，但是关闭 Settings 后再打开发现配置并没有保存下来，配置丢失。

## 问题原因

发现了三个配置保存相关的 bug：

### Bug 1: `getProperties()` 返回副本

```java
// SyncConfig.java
@NotNull
public Map<String, String> getProperties() {
    return new HashMap<>(properties);  // ❌ 返回副本，不是原始引用
}
```

**问题**：当在 `SyncSettings.getSyncConfig()` 中尝试通过 `config.getProperties().putAll()` 加载配置时，实际上是在操作一个临时副本，并没有影响到 `config` 内部的 `properties`。

### Bug 2: 缺少 `providerType` 设置

```java
// SyncSettings.java - 旧版本
public SyncConfig getSyncConfig() {
    SyncConfig config = createConfigByType(state.providerType);
    config.setEnabled(state.enabled);
    config.setAutoSync(state.autoSync);
    // ❌ 缺少这行：config.setProviderType(state.providerType);
    config.getProperties().putAll(state.properties);
    return config;
}
```

**问题**：虽然根据 `providerType` 创建了对应的配置对象，但没有设置配置对象的 `providerType` 字段，导致配置不完整。

### Bug 3: 初始加载时机不对

```java
// SyncConfigurable.java - 旧版本
public JComponent createComponent() {
    if (settingsPanel == null) {
        settingsPanel = new SyncSettingsPanel();
        // ❌ 创建面板后没有立即加载配置
    }
    return settingsPanel.getPanel();
}
```

**问题**：创建UI面板后没有立即加载现有配置，导致第一次打开 Settings 时显示的是空白配置。

## 修复方案

### 修复 1: 使用 `setProperty` 方法逐个设置

```java
// SyncSettings.java - 修复后
public SyncConfig getSyncConfig() {
    SyncConfig config = createConfigByType(state.providerType);
    
    config.setEnabled(state.enabled);
    config.setAutoSync(state.autoSync);
    config.setProviderType(state.providerType);  // ✅ 添加
    
    // ✅ 使用setProperty逐个设置，而不是操作getProperties()的返回值
    for (Map.Entry<String, String> entry : state.properties.entrySet()) {
        config.setProperty(entry.getKey(), entry.getValue());
    }
    
    return config;
}
```

### 修复 2: 创建面板时立即加载配置

```java
// SyncConfigurable.java - 修复后
public JComponent createComponent() {
    if (settingsPanel == null) {
        settingsPanel = new SyncSettingsPanel();
        // ✅ 创建面板后立即加载配置
        workingConfig = SyncSettings.getInstance().getSyncConfig();
        settingsPanel.loadFrom(workingConfig);
    }
    return settingsPanel.getPanel();
}
```

### 修复 3: Apply 后重新加载配置

```java
// SyncConfigurable.java - 修复后
public void apply() throws ConfigurationException {
    // ... 保存逻辑 ...
    
    SyncSettings.getInstance().setSyncConfig(workingConfig);
    
    // ✅ 重新加载workingConfig，确保下次打开时显示正确
    workingConfig = SyncSettings.getInstance().getSyncConfig();
}
```

## 测试验证

### 测试步骤

1. **打开设置**
   ```
   File → Settings → Tools → Code Reading Note Sync
   ```

2. **配置同步**
   ```
   ☑ 启用同步
   同步方式: GitHub
   仓库地址: username/repo
   访问令牌: ghp_xxxxx
   分支: main
   基础路径: code-reading-notes
   ```

3. **点击 Apply**
   - 应该没有错误提示
   - 配置应该被保存

4. **点击 OK 关闭设置**

5. **重新打开设置**
   ```
   File → Settings → Tools → Code Reading Note Sync
   ```

6. **验证配置**
   - ✅ 应该看到刚才配置的所有内容
   - ✅ 启用同步应该是勾选状态
   - ✅ 所有字段应该显示之前输入的值

### 预期结果

- ✅ 配置能够正确保存
- ✅ 重新打开 Settings 能看到之前的配置
- ✅ IDE 重启后配置仍然存在
- ✅ Push/Pull 操作能正常使用保存的配置

## 配置文件位置

配置保存在：
```
Windows: %USERPROFILE%\.IntelliJIdea2024.x\config\options\codeReadingNoteSync.xml
Linux:   ~/.IntelliJIdea2024.x/config/options/codeReadingNoteSync.xml
Mac:     ~/Library/Application Support/JetBrains/IntelliJIdea2024.x/options/codeReadingNoteSync.xml
```

### 配置文件示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<application>
  <component name="CodeReadingNoteSyncSettings">
    <State>
      <option name="enabled" value="true" />
      <option name="autoSync" value="false" />
      <option name="providerType" value="GITHUB" />
      <option name="properties">
        <map>
          <entry key="repository" value="username/my-notes" />
          <entry key="token" value="ghp_xxxxxxxxxxxxx" />
          <entry key="branch" value="main" />
          <entry key="basePath" value="code-reading-notes" />
        </map>
      </option>
    </State>
  </component>
</application>
```

## 故障排查

### 配置仍然无法保存

1. **检查文件权限**
   ```
   确保配置目录有写入权限
   ```

2. **检查磁盘空间**
   ```
   确保有足够的磁盘空间
   ```

3. **查看日志**
   ```
   Help → Show Log in Explorer/Finder
   搜索 "CodeReadingNoteSyncSettings" 相关错误
   ```

4. **手动创建配置文件**
   ```
   可以手动创建 codeReadingNoteSync.xml 文件
   按照上面的示例格式填写
   重启 IDE
   ```

### Token 没有保存

如果发现其他配置都保存了，但 Token 丢失：

1. **检查 Token 格式**
   - Token 应该以 `ghp_` 开头
   - 不要有多余的空格

2. **检查密码字段**
   - `JBPasswordField` 使用 `getPassword()` 获取
   - 转换为 String: `new String(tokenField.getPassword())`

3. **检查日志**
   - 看是否有异常信息

## 相关文件

- `SyncSettings.java` - 配置持久化
- `SyncConfigurable.java` - Settings UI 集成
- `SyncSettingsPanel.java` - 配置面板
- `SyncConfig.java` - 配置基类
- `GitHubSyncConfig.java` - GitHub 配置

## 总结

这次修复解决了三个关键问题：
1. ✅ 使用正确的方法设置 properties
2. ✅ 完整设置所有配置字段
3. ✅ 正确的初始化和保存时机

现在配置能够正确保存和恢复，用户体验得到了改善。

---

**修复版本**: v3.4.0-fix1
**修复日期**: 2024-11-01

