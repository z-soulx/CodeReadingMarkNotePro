# Plan

修正共享 GitHubSyncProvider 的分支读取、SHA 错误传播及 JSON 错误解析，覆盖仍使用该 Provider 的入口；工作空间 GitHubNotesRemote 保持条件写入并细分 422 提示。使用可注入 HTTP endpoint 与本地服务器验证已有 XML 更新、连续更新及失败不发 PUT。错误正文先结构化解码，展示前清除凭据。

将未发布的 3.8.0/3.9.0 功能说明合并为 3.7.7；既往构建证据保持原值。本次不修改或回复远端 issue，不执行用户真实笔记推拉。
