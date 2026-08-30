# AGENTS.md

Code Reading Mark Note Pro - IntelliJ IDEA plugin for code reading notes and bookmarks
(Java 17 + Gradle + IntelliJ Platform 2024.3+). Plugin ID: `soulx.CodeReadingMarkNotePro`.
Pro adds group management, GitHub sync, multi-language UI, AI config workspace.

## Before doing anything in this repo

1. Load and follow `.ai/constitution.md` - non-negotiable red lines (i18n, data
   compatibility, error handling, versioning)
2. Load `.ai/context/INDEX.md` - routes you to exactly the docs your task needs
3. Quick facts (components, storage, events): `.ai/context/system-overview.md`

## Where things live (intent -> location)

| Intent | Location |
|--------|----------|
| Change the system (feature/bugfix/refactor) | create a unit in `.ai/specs/` (see `.ai/specs/README.md`) |
| Describe the current system | `docs/` (Chinese knowledge base) |
| Record why a decision was made | `.ai/adr/` (append-only) |
| Execution evidence | `.ai/runs/` (gitignored, pure record) |
| End-user plugin docs | `help/` (do not mix with `docs/`) |

## Key code paths

| What | Where |
|------|-------|
| Build | `build.gradle` |
| Plugin descriptor | `src/main/resources/META-INF/plugin.xml` |
| Change notes | `src/main/resources/META-INF/changeNotes.html` |
| Source root | `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/` |
| Core service | `...codereadingnote/CodeReadingNoteService.java` |
| Sync layer | `...codereadingnote/sync/` (SyncProvider, GitHubSyncProvider) |
| AI config layer | `...codereadingnote/aiconfig/` (AIConfigService, AIConfigRegistry, AIConfigSyncAdapter) |
| UI panels | `...codereadingnote/ui/` (ManagementPanel, AIWorkspacePanel, AIConfigTreePanel) |
| Gutter/Popup | `...remark/NoteGutterIconRenderer.java`, `NotePopupHelper.java` |
| i18n bundles | `src/main/resources/messages/CodeReadingNoteBundle*.properties` |

Never describe a live system as a spec, and never let a spec describe what already
exists - the routing rules in `INDEX.md` decide.
