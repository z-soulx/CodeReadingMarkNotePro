# Implementation Plan

1. Add `AIWorkspaceService`, `AIWorkspaceGitService`, `AIWorkspaceVersionService`, and `AIWorkspaceCommandService`.
2. Connect Git initialization, version bump, commit, and the single command-management action to `AIWorkspacePanel`; Add/Edit share one form, Save confirms once, and Run launches directly from the manager.
3. Keep `.ai/docs` as the runtime docs location; users may move it manually.
4. Preserve existing AI config scan/sync behavior, including legacy metadata compatibility, while ignoring `.git` directories.
5. Add i18n resources, bilingual skeleton references, help documentation, and Gradle verification.

Threading: Git operations run in IntelliJ background tasks. Custom commands use either `TerminalToolWindowManager`/IDEA Terminal (PowerShell on Windows) or a background `ProcessBuilder` when Silent mode is selected. Persistence uses project-level workspace state and JSON schema version 1 for commands.
