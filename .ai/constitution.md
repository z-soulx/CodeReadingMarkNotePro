# Constitution - Project Red Lines

Non-negotiable constraints. A change that violates any of these is wrong regardless of its feature value.

This file is the canonical and only source of the red lines; `AGENTS.md` is the root entry
point that directs loading it. When rules change, update this file and any doc that quotes
them.

## Internationalization

- 100% UI text coverage, **no hardcoded strings**
- `CodeReadingNoteBundle.message("key.name")` with params `CodeReadingNoteBundle.message("key", p1, p2)`
- EN: `src/main/resources/messages/CodeReadingNoteBundle.properties`
- ZH: `src/main/resources/messages/CodeReadingNoteBundle_zh.properties`
- Key naming: dot-separated (`action.new.topic`, `dialog.create.topic.title`)
- Runtime language switch, no restart needed

## Data & Compatibility

- Backward compatibility for persisted data formats. Older plugin versions must still be able
  to read data written by newer versions:
  - `CodeReadingNote.xml` (local + remote synced notes)
  - `aiConfigRegistry.xml` (project-level AI config state)
  - `ai-config-registry.json` (remote workspace metadata)
- New persisted fields must have a migration-safe default (missing/empty = legacy state)
- Proper cleanup of listeners and resources (MessageBus subscriptions, VFS listeners,
  scheduled executors)

## Error Handling

- All exceptions must have i18n user-facing error messages
- Never swallow exceptions silently - surface them or wrap with an i18n message
- GitHub token and other credentials must never appear in logs, exception messages, or
  pushed content. `.ai/token.txt` is local-only (gitignored).

## Versioning & Commits

- Semver `x.y.z` in `build.gradle` and `plugin.xml` (must match)
- Commit: `type: description` (feat / fix / docs / style / refactor / test / chore)

## Testing & Verification

- Current reality: focused unit tests exist under `src/test` for workspace Semver and
  command construction. Broader verification still relies on `gradlew build` plus the
  acceptance criteria of the driving spec in `.ai/specs/`.
- Sync/aiconfig logic that is pure computation (e.g. `AIConfigMergeAnalyzer`) is written
  UI/IDE-free on purpose - keep it that way so it stays testable when a suite is added.
- "Completed" means verified, not merely compiled. Fail loud: report skipped steps.

## IntelliJ Platform Taboos

- No blocking network I/O on the EDT - push/pull and GitHub API calls run on pooled threads
- No business logic inside UI listeners; route through services and the MessageBus
