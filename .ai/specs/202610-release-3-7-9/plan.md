# 方案

按 docs/runbooks/release.md 升补丁版本并更新双语变更说明。
运行 test/build，检查 ZIP 内插件描述符；按用户明确要求从当前分支创建 3.7.9 版本分支，常规提交并推送，不强推。
通过 Gradle publishPlugin 使用已有用户级凭据上传默认通道，不读取或输出凭据。
执行证据追加到 .ai/runs/202610-workspace-refresh/progress.md；Marketplace 审核与已上传状态分别报告。
