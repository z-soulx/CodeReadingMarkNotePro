# Plan: First-run built-in workspace commands

- Unit: `202608-ai-builtin-commands`

## Design

`AIWorkspaceCommandService` owns a static factory `builtInCommands(cursorName, typoraName)` that builds the two records. `ensureSeeded()` writes them only when `.ai/` is a directory and `workspace-commands.json` is missing. `load()` calls `ensureSeeded()` first. `AIWorkspaceVcsStartupActivity` also calls `ensureSeeded()` on a pooled thread so the file appears without opening the manager.

Existing files are never merged. `delete()` already saves the remaining list, so removing both commands leaves `[]` and later loads stay empty.

Display names are resolved from the bundle at seed time and stored like any other command.

## Impact

| Area | Files / modules | Risk |
|------|----------------|------|
| Command service | `AIWorkspaceCommandService.java` | Accidental re-seed if existence check is wrong |
| Startup | `AIWorkspaceVcsStartupActivity.java` | Extra file I/O when `.ai/` exists |
| Tests | `AIWorkspaceCommandServiceTest.java` | Factory/validate only (no Project) |
| i18n | `CodeReadingNoteBundle*.properties` | Missing ZH key |
| Docs | help, domain README, changeNotes 3.7.5 | Help Typora id must match built-in |

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility (new fields have safe defaults)
- [x] Resource cleanup (listeners, schedulers)

## Alternatives Considered

Re-insert missing ids on every load: rejected; the user asked for one-time defaults they can delete.

Keep names as i18n keys in JSON and translate at display time: rejected; commands are user-owned after seed, and the manager already shows stored `displayName`.
