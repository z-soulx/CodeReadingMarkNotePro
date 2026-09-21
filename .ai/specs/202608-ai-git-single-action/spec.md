# Spec: Merge .ai Git toolbar into one stateful action

- Unit: `202608-ai-git-single-action`
- Status: frozen
- Type: refactor

## Intent (WHY)

Initialize and Open are one workflow. Two buttons confuse users. One control should initialize when `.ai/.git` is missing and open IDEA Commit when it exists. After the first mapping, the project Commit window already includes `.ai`.

## Background

`openWorkspaceGitUi()` already initializes then opens. The separate Initialize action only inits and shows a dialog. See `docs/domain/ai-config/README.md`.

## Scope

In scope: one toolbar action with stateful label; remove Initialize-only action and its success dialog; document that mapping persists. Out of scope: removing the action entirely after init.

## Acceptance Criteria

- [x] Toolbar has a single Git action: label Initialize when not inited, Open when inited; click always ends in native Commit UI (init first if needed).
- [x] Help/docs state that after the first successful init, IDEA Commit keeps the `.ai` root (Directory Mapping + startup restore).
- [x] `./gradlew test` passes.
