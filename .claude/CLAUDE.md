# Code Reading Mark Note Pro

An IntelliJ IDEA plugin that helps developers take notes and create bookmarks while reading source code. Based on the open-source [CodeReadingNote](https://github.com/kitabatake/CodeReadingNote) with full backward compatibility, this Pro version adds group management, GitHub auto-sync, multi-language UI, and more.

- Plugin ID: `soulx.CodeReadingMarkNotePro`
- Tech: Java 17 + Gradle + IntelliJ Platform 2024.3+

## Hard Rules

### Internationalization
- 100% UI text coverage, **no hardcoded strings**
- Use `CodeReadingNoteBundle.message("key.name")` with params `CodeReadingNoteBundle.message("key", p1, p2)`
- EN: `src/main/resources/messages/CodeReadingNoteBundle.properties`
- ZH: `src/main/resources/messages/CodeReadingNoteBundle_zh.properties`
- Key naming: dot-separated (`action.new.topic`, `dialog.create.topic.title`)
- Runtime language switch, independent of IDE language, no restart needed

### Code Quality
- Backward compatibility for data formats
- Proper cleanup of listeners and resources
- All exceptions must have i18n user-facing error messages

### Versioning & Commits
- Semver `x.y.z` in `build.gradle` and `plugin.xml` (must match)
- Commit: `type: description` (feat / fix / docs / style / refactor / test / chore)

## Development Workflow

1. New feature: create `tmpmd/feature_<name>.md` -> develop -> clean up
2. Bug fix: create `tmpmd/bugfix_<desc>.md` -> fix -> clean up
3. Long-term specs go in `.ai/` directory

## Key Paths

| What | Where |
|------|-------|
| Build | `build.gradle` |
| Plugin descriptor | `src/main/resources/META-INF/plugin.xml` |
| Change notes | `src/main/resources/META-INF/changeNotes.html` |
| Source | `src/main/java/jp/kitabatakep/intellij/plugins/codereadingnote/` |
| i18n bundles | `src/main/resources/messages/CodeReadingNoteBundle*.properties` |

## Progressive Loading

Architecture, workflows, and feature specs are in `.ai/` -- load on demand:
- `.ai/ARCHITECTURE.md` - domain model, components, data storage, design patterns
- `.ai/WORKFLOW.md` - version release procedure, post-feature review process
- `.ai/README_SYNC.md` - sync feature spec (GitHub sync, conflict detection, auto-sync)
