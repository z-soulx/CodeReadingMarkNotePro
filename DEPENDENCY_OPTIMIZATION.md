# 插件依赖优化说明

## 优化内容

### 1. 版本号同步 ✅
- **plugin.xml**: `<version>2.0.0</version>` 
- **build.gradle**: `version '2.0.0'`
- 确保两处版本号一致，消除版本不匹配警告

### 2. 明确依赖声明 ✅
```xml
<!-- plugin.xml -->
<depends>com.intellij.modules.platform</depends>
<depends>com.intellij.modules.lang</depends>
```
- 明确声明需要的IntelliJ模块
- 提供更清晰的依赖关系

### 3. 编译配置优化 ✅
```gradle
// build.gradle
intellij {
    version.set('2024.3')  // 编译版本
    type.set('IC')         // 使用Community版本
    plugins.set([])        // 明确声明无插件依赖
}
```

### 4. 插件元数据完善 ✅
```xml
<vendor email="170918810@qq.com" url="https://plugins.jetbrains.com/plugin/24163-code-reading-mark-note-pro">soulx</vendor>
<category>Code tools</category>
```

### 5. 验证配置优化 ✅
```gradle
runPluginVerifier {
    ideVersions.set(['2024.3', '2025.1', '2025.2'])
    failureLevel.set(['COMPATIBILITY_PROBLEMS'])
    downloadDir.set(file("${project.buildDir}/pluginVerifier/ides"))
}
```

## 警告消除效果

### 之前的警告
```
soulx.CodeReadingMarkNotePro unknown version
\--- com.intellij.modules.platform unknown version [declaring module com.intellij.modules.platform]
```

### 优化后的效果
- ✅ 版本号明确：`soulx.CodeReadingMarkNotePro 2.0.0`
- ✅ 依赖关系清晰：明确声明platform和lang模块依赖
- ✅ 编译配置标准化：使用IC版本，无额外插件依赖
- ✅ 验证范围明确：测试具体的版本而非快照版本

## 依赖模块说明

### com.intellij.modules.platform
- **作用**: IntelliJ平台的核心API
- **包含**: 基础的项目管理、文件系统、UI框架等
- **必需**: 所有IntelliJ插件的基础依赖

### com.intellij.modules.lang  
- **作用**: 语言支持相关的API
- **包含**: 编辑器、语法高亮、代码导航等
- **适用**: 需要编辑器功能的插件（如你的代码标记功能）

## 最佳实践

### ✅ 推荐做法
1. 明确声明所有需要的模块依赖
2. 使用IC（Community）版本进行编译以确保兼容性
3. 保持plugin.xml和build.gradle版本号同步
4. 提供完整的插件元数据

### ❌ 避免的做法
1. 只依赖platform模块但使用lang模块的功能
2. 版本号不一致
3. 使用LATEST-EAP-SNAPSHOT进行发布验证
4. 缺少插件分类和描述信息

## 验证命令

```bash
# 验证插件在多个版本的兼容性
./gradlew runPluginVerifier

# 构建插件包
./gradlew buildPlugin

# 检查插件描述符
./gradlew verifyPlugin
```

这些优化应该能消除大部分依赖相关的警告，并提供更清晰的插件配置。
