# Runbook：版本发布

迁自旧 `.ai/WORKFLOW.md`，分析对象由 `tmpmd/` 改为新体系的 `.ai/specs/`。

## 发布步骤

1. **版本号**两处同步改（必须一致）：
   - `build.gradle` -> `version`
   - `src/main/resources/META-INF/plugin.xml` -> `<version>`

2. **更新变更说明** `src/main/resources/META-INF/changeNotes.html`：
   ```html
   <h1>x.y.z</h1>
   <ul>
     <li><b>Feature:</b> English desc（<b>新功能：</b>中文描述）</li>
     <li><b>Bug Fix:</b> English desc（<b>Bug修复：</b>中文描述）</li>
   </ul>
   ```
   - 素材来源：`.ai/` 的分析对象改为自上次发布以来**冻结的变更单元**
     （`.ai/specs/*/tasks.md` 的完成情况 + `spec.md` 的用户可见意图）
   - 按 Features / Improvements / Bug Fixes / i18n 分类
   - 用**产品视角**写（用户得到什么），不用开发者视角（改了哪些类）
   - 去重：同一功能的同批修复合并为一条

3. **可选**：重大功能同步更新 `description.html` 与根 `README.md`

4. 构建验证后按提交规范入库（`chore: x.y.z` 或直接 `x.y.z`，沿用现有习惯）

5. **一键发布到 JetBrains Marketplace**（无需打开网页上传 zip/gzip）：

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
   .\gradlew.bat --version
   .\gradlew.bat publishPlugin
   ```

   `--version` 中 JVM 应为 17。Gradle 启动先读取 `JAVA_HOME`，构建脚本中的 Java 17
   toolchain 不能修正启动阶段的 Java 7；上述设置仅影响当前 PowerShell 会话。
   IDEA 内构建则需将 Settings → Build Tools → Gradle → Gradle JVM 设为 JDK 17。

   一次性准备（只需做一次）：

   1. 打开 [My Tokens](https://plugins.jetbrains.com/author/me/tokens)，生成 Personal Access Token。
   2. 把 token 写到 **Gradle user home** 下的 `gradle.properties`（不要提交进仓库）。
      本机 Gradle user home 是 `E:\work\PG\Gradlerepo`（Settings → Gradle → Gradle user home /
      环境变量 `GRADLE_USER_HOME`），因此文件是：

      `E:\work\PG\Gradlerepo\gradle.properties`

      ```properties
      intellijPublishToken=perm:你的token
      ```

      若该目录还没有这个文件，新建即可。不要写到 `C:\Users\<你>\.gradle\`——本机未使用默认路径。

      或者当前终端临时设置：

      ```powershell
      $env:PUBLISH_TOKEN = "perm:你的token"
      ```

   3. 本插件已在 Marketplace 上（id `24163` / `soulx.CodeReadingMarkNotePro`），Gradle 上传只用于后续版本。
      Marketplace 不会接受重复版本号，发布前确认 `build.gradle` 与 `plugin.xml` 已升版。

   可选签名：未签名也能发布，但安装时 IDE 可能提示未签名。若要签名，在同一份
   Gradle user home 的 `gradle.properties` 里加上证书路径（文件放仓库外）：

   ```properties
   certificateChainFile=C:/path/to/chain.crt
   privateKeyFile=C:/path/to/private.pem
   privateKeyPassword=你的私钥密码
   ```

   证书生成见 [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)。
   配置后 `publishPlugin` 会先跑 `signPlugin` 再上传。

   未配置签名材料时，发布流程跳过签名，也不下载 ZIP Signer；这避免了可选签名步骤
   因 `Cannot resolve the latest Marketplace ZIP Signer CLI version` 阻塞未签名发布。
   仅配置证书或私钥会明确报错，不会静默按未签名发布。若已配置完整签名材料而遇到
   此错误，需检查 ZIP Signer 下载所需的网络/代理访问；不要通过跳过签名任务发布。

   发布前可运行 `.\gradlew.bat publishPlugin --dry-run` 检查任务图，不会上传。

   发到非默认通道（如 EAP）时加参数：`.\gradlew.bat publishPlugin -PpublishChannel=eap`

## 发布后检查

- 冷启动安装新版本，验证：旧数据能读（向后兼容红线）、双语言切换、
  两条同步通道手动推/拉各一次
- 发现问题 -> 开新的变更单元（bugfix），不回头改已冻结单元
# 3.7.5 .ai Git UI 发布检查

- 确认 `build.gradle` 与 `plugin.xml` 版本均为 `3.7.5`。
- 初始化 `.ai` Git 后项目根 `.gitignore` 含 `/.ai/`，父仓库 Commit 不再把 `.ai/**` 列为未跟踪文件。
- Directory Mappings 含 `.ai` → Git；重新打开项目后 mapping 仍在。
- 工具栏只有一个 Git 按钮：未初始化则 init 并打开 Commit，已初始化则打开。第一次之后 IDEA 自己的 Commit 窗口也会一直有 `.ai` mapping。
- 发布前确认 `changeNotes.html` 含 3.7.5，并运行 `./gradlew test`。

# 3.7.4 AI Workspace 发布检查

- 确认 `build.gradle` 与 `plugin.xml` 版本均为 `3.7.4`。
- 验证 AI Workspace 默认 `.ai/docs`，且不会移动根目录 `docs/`。
- 仅在用户点击后初始化 `.ai/.git`，并确认 `.ai/.gitignore` 排除敏感文件。
- 验证 `.ai/VERSION` 初始化、补丁升级和 `chore: <version>` 提交。
- 发布前确认 `META-INF/description.html` 与 `changeNotes.html` 已包含 3.7.5 的 `.ai` Git UI 与首次内置命令说明，并运行 `./gradlew test`。
- 验证新 `.ai/` 在缺少 `workspace-commands.json` 时会写入 Cursor、Typora 两条默认可删命令；已有该文件（含空列表）不再注入。确认 `$FilePath$` 默认可来自项目工具窗口选中（不必打开，无选中则回退编辑器），也可改为仅用编辑器文件；静默模式通过 IDEA Terminal 执行但不抢焦点，两种模式都可执行 `cd ai3`。
