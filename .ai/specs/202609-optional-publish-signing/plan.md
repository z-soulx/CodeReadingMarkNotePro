# 实施计划

检查 Gradle IntelliJ 1.17.0 的签名依赖；只在未提供签名材料时移除 signPlugin 的 ZIP Signer 下载依赖。保留独立签名验证任务的工具下载能力。为部分证书配置提供明确失败，避免误发未签名版本。更新发布指南中的 JDK 17 会话命令。以无上传的 Gradle 任务图与 signPlugin 执行验证。
