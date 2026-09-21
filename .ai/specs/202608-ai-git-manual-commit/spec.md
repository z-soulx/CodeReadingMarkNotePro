# Spec: Remove workspace version bump and shortcut commit buttons

- Unit: `202608-ai-git-manual-commit`
- Status: frozen
- Type: refactor

## Intent (WHY)

With IDEA's native Commit UI for `.ai`, users want to write their own messages and choose files. The toolbar Bump Version and Commit Workspace Version actions force a patch bump and `chore: <version>` commit, which fights that workflow.

## Background

3.7.5 added Open .ai Git plus parent isolation. Bump/Commit were leftover from 3.7.4 CLI shortcuts. See `docs/domain/ai-config/README.md`.

## Scope

In scope: remove the two toolbar actions and their panel methods; drop unused bundle keys; update help/docs/changeNotes. Out of scope: deleting `AIWorkspaceVersionService` or `GitService.commit` (VERSION file and CLI git remain for manual use).

## Acceptance Criteria

- [x] AI Workspace toolbar has Initialize .ai Git and Open .ai Git, not Bump Version or Commit Workspace Version.
- [x] Users commit `.ai` through IDEA Commit UI or by editing `.ai/VERSION` themselves.
- [x] Help/docs/changeNotes match. `./gradlew test` passes.
