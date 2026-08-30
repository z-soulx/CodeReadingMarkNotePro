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

## 发布后检查

- 冷启动安装新版本，验证：旧数据能读（向后兼容红线）、双语言切换、
  两条同步通道手动推/拉各一次
- 发现问题 -> 开新的变更单元（bugfix），不回头改已冻结单元
# 3.7.4 AI Workspace 发布检查

- 确认 `build.gradle` 与 `plugin.xml` 版本均为 `3.7.4`。
- 验证 AI Workspace 默认 `.ai/docs`，且不会移动根目录 `docs/`。
- 仅在用户点击后初始化 `.ai/.git`，并确认 `.ai/.gitignore` 排除敏感文件。
- 验证 `.ai/VERSION` 初始化、补丁升级和 `chore: <version>` 提交。
- 发布前确认 `META-INF/description.html` 与 `changeNotes.html` 已包含 3.7.4 的 AI Workspace、Skeleton 和 Custom Commands 说明，并运行 `./gradlew test`。
- 验证 `.ai/workspace-commands.json` 中的 Cursor、Typora 示例命令；确认 `$FilePath$` 默认可来自项目工具窗口选中（不必打开，无选中则回退编辑器），也可改为仅用编辑器文件；静默模式通过 IDEA Terminal 执行但不抢焦点，两种模式都可执行 `cd ai3`。
