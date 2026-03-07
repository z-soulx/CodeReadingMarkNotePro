# Code Reading Mark Note Pro

IntelliJ IDEA plugin for code reading notes and bookmarks. Pro version adds group management, GitHub sync, multi-language UI, AI config workspace.

- Plugin ID: `soulx.CodeReadingMarkNotePro`
- Tech: Java 17 + Gradle + IntelliJ Platform 2024.3+

## Hard Rules

### Internationalization
- 100% UI text coverage, **no hardcoded strings**
- `CodeReadingNoteBundle.message("key.name")` with params `CodeReadingNoteBundle.message("key", p1, p2)`
- EN: `src/main/resources/messages/CodeReadingNoteBundle.properties`
- ZH: `src/main/resources/messages/CodeReadingNoteBundle_zh.properties`
- Key naming: dot-separated (`action.new.topic`, `dialog.create.topic.title`)
- Runtime language switch, no restart needed

### Code Quality
- Backward compatibility for data formats
- Proper cleanup of listeners and resources
- All exceptions must have i18n user-facing error messages

### Versioning & Commits
- Semver `x.y.z` in `build.gradle` and `plugin.xml` (must match)
- Commit: `type: description` (feat / fix / docs / style / refactor / test / chore)

## Development Workflow

1. New feature: create `tmpmd/feature_<name>.md` → develop → clean up
2. Bug fix: create `tmpmd/bugfix_<desc>.md` → fix → clean up
3. Long-term specs go in `.ai/` directory

## Key Paths

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
| User help docs | `help/` (README.md, SYNC_GUIDE.md, AI_WORKSPACE_GUIDE.md) |

## Progressive Loading

Architecture and specs are in `.ai/` — load on demand:
- `.ai/ARCHITECTURE.md` — domain model, components, data storage, sync design, AI config layer (ignore system, empty dir support, remote delete)
- `.ai/WORKFLOW.md` — version release procedure, post-feature review process
