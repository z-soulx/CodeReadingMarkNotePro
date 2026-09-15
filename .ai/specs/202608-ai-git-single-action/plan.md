# Plan: Merge .ai Git toolbar into one stateful action

- Unit: `202608-ai-git-single-action`

## Design

One `AnAction` on the AI Workspace toolbar. `update()` sets text/description from init vs open bundle keys. `actionPerformed` always calls `openWorkspaceGitUi()`. Delete `initializeWorkspaceGit()`.

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility
- [x] Resource cleanup
