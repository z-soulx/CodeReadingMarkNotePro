# Plan: Remove workspace version bump and shortcut commit buttons

- Unit: `202608-ai-git-manual-commit`

## Design

Delete the two `AnAction` entries and `bumpWorkspaceVersion` / `commitWorkspaceVersion` from `AIWorkspacePanel`. Keep `AIWorkspaceVersionService` and `GitService.commit`. Remove bundle keys used only by those actions.

## Impact

| Area | Files | Risk |
|------|-------|------|
| UI | `AIWorkspacePanel` | Low |
| i18n / docs | bundles, help, domain README, changeNotes | Low |

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility (new fields have safe defaults)
- [x] Resource cleanup (listeners, schedulers)
