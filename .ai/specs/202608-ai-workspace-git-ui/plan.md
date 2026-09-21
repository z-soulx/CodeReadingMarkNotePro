# Plan: .ai independent Git and IDEA Commit UI

- Unit: `202608-ai-workspace-git-ui`

## Design

Keep CLI Git in `AIWorkspaceGitService` (cwd = `.ai`). Add parent-isolation helpers that append a marked `/.ai/` block to the project `.gitignore` when that exact rule is missing.

Add `AIWorkspaceVcsSupport` using platform VCS APIs only (`ProjectLevelVcsManager.setDirectoryMapping(path, "Git")`), no Git4Idea compile dependency. Skip when a mapping for the same path already exists or when the Git VCS plugin is absent.

`AIWorkspaceVcsStartupActivity` (`ProjectActivity`): if `.ai/.git` exists, ensure parent isolation and register the mapping on the EDT.

AI Workspace toolbar action Open .ai Git: initialize when needed (existing background task), then `CheckinFiles` with a DataContext whose `VIRTUAL_FILE` / `VIRTUAL_FILE_ARRAY` is the `.ai` root; fall back to activating the `Commit` (then `Vcs`) tool window.

This plugin repository's `.gitignore` uses `/.ai/` instead of the previous three `.ai` exceptions.

## Impact

| Area | Files / modules | Risk |
|------|----------------|------|
| Git CLI + ignore | `AIWorkspaceGitService` | Writing the user's project `.gitignore` — append-only, skip if `/.ai/` already present |
| VCS mapping | `AIWorkspaceVcsSupport`, startup activity, `plugin.xml` | Invalid mapping if Git plugin missing — guard with `findVcsByName` |
| UI | `AIWorkspacePanel` | CheckinFiles may still show other roots (IDEA limitation) |
| i18n / docs / version | bundles, help, docs, `changeNotes.html`, `3.7.5` | Version mismatch if one file is skipped |

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility (new fields have safe defaults)
- [x] Resource cleanup (listeners, schedulers)

## Alternatives Considered

- Git submodule: changes parent-repo collaboration; rejected for a local workspace.
- Open `.ai` as a second IDEA project window: exclusive UI, but heavy and confusing.
- Compile against Git4Idea: unnecessary if Directory Mapping and `CheckinFiles` stay on platform APIs.
