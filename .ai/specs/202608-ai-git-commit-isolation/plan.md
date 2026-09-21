# Plan: Keep project commits from including .ai files

- Unit: `202608-ai-git-commit-isolation`

## Design

`AIWorkspaceGitService.ensureParentIsolation()` always writes `/.ai/` and, when the parent is a Git repo with tracked `.ai` paths, runs `git rm -r --cached --ignore-unmatch -- .ai` in the project root (never `git commit`).

Startup runs isolation whenever `.ai/` exists, not only when `.ai/.git` exists. If untrack changed the index, show an i18n balloon. Nested mapping and `AIWorkspaceChangeListService` still run only when nested Git is initialized.

`AIWorkspaceChangeListService` (project service + `Disposable`) moves changes under `.ai` into a changelist named `.ai` and keeps Default as the active list.

## Impact

| Area | Files / modules | Risk |
|------|----------------|------|
| Git | `AIWorkspaceGitService` | Staged parent deletions until the user commits once |
| Startup | `AIWorkspaceVcsStartupActivity` | Isolation without nested Git |
| Changelist | `AIWorkspaceChangeListService` | Listener re-entry; dispose on project close |
| Docs / i18n | help, domain README, changeNotes, bundles | — |

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility (new fields have safe defaults)
- [x] Resource cleanup (listeners, schedulers)

## Alternatives Considered

- Force-grouping the Commit tree: no stable public API.
- Auto-commit the parent untrack: would write to the user's project Git without asking.
