# System Overview - Current State Facts

What exists right now, as a compact fact sheet. Narrative explanations live in `docs/`;
decisions and their rationale live in `.ai/adr/`.

## Identity

- IntelliJ IDEA plugin `soulx.CodeReadingMarkNotePro`: code reading notes and bookmarks.
  Pro adds group management, GitHub sync, multi-language UI, AI config workspace.
- Java 17 + Gradle + IntelliJ Platform 2024.3+. Focused unit tests live under `src/test` (workspace Semver and command construction). UI/sync verification still uses `gradlew build` plus spec acceptance.

## Domain Model

```
CodeReadingNoteService (project-level singleton)
└── TopicList
    └── Topic (name, datetime)
        └── TopicLine (url, line, description)
            └── Group (tag-based grouping, custom naming)
```

Core entities: `Topic` is a reading theme, `TopicLine` is an annotated code line within a
topic, `Group` provides tag-based organization within topics, `TrashedLine` wraps deleted
notes in a recoverable trash bin.

## Components

| Layer | Key Classes | Responsibility |
|-------|------------|----------------|
| Service | `CodeReadingNoteService` | State management, persistence via `PersistentStateComponent` |
| Domain | `Topic`, `TopicLine`, `TopicList`, `TrashedLine` | Business entities and operations |
| Sync | `SyncProvider`, `SyncService`, `GitHubSyncProvider`, `AutoSyncScheduler`, `SyncConflictDetector` | Remote sync with conflict detection, debounced auto push |
| AI Config | `AIConfigService`, `AIConfigRegistry`, `AIConfigSyncAdapter`, `AIConfigAutoSyncScheduler`, `AIConfigMergeAnalyzer` | AI config discovery, tracking, independent sync, three-way merge |
| AI Workspace | `AIWorkspaceService`, `AIWorkspaceGitService`, `AIWorkspaceVersionService`, `AIWorkspaceCommandService` | `.ai/docs` knowledge workspace, opt-in `.ai` Git, Semver VERSION, Terminal or silent-unfocused Terminal commands with IDEA file macros |
| UI | `ManagementPanel`, `TopicDetailPanel`, `AIWorkspacePanel`, `PushReportDialog` | ToolWindow panels (tabs: tree / search / AI workspace) |
| Gutter | `NoteGutterIconRenderer`, `NotePopupHelper` | Custom gutter icon + interactive edit popup |
| Actions | `TopicLineAddAction`, `NavigateToNoteAction`, etc. | User operations |

## Data Storage

| File | Content | Scope |
|------|---------|-------|
| `CodeReadingNote.xml` | Topics, Lines, Groups | Synced to remote |
| `aiConfigRegistry.xml` | AI config tracked state, custom paths, ignore patterns, push hashes, tracked empty dirs | Project-level |
| `syncStatus.xml` | Sync timestamps, MD5 cache | Local only |
| `codeReadingNoteSync.xml` | Token, repo URL | Application-level |
| `ai-config-registry.json` (remote) | Cross-platform workspace metadata (tracked entries, custom paths, ignore patterns, file hashes, empty dirs) | Synced |
| `aiWorkspace.xml` | Runtime docs root (defaults to `.ai/docs`) | Project-level local |
| `.ai/VERSION` | Strict Semver workspace version (defaults to `1.0.0`) | `.ai` workspace |
| `.ai/workspace-commands.json` | Custom commands, schemaVersion 1 | `.ai` workspace |

## Events

IntelliJ `MessageBus`: `TopicListNotifier` (topic list changes), `TopicNotifier` (single
topic changes, `lineNoteChanged`), `AIConfigNotifier` (AI config registry/file changes),
`SyncStatusNotifier` (sync status changes).

## Known Documentation Gaps

The following were never covered by the old `.ai/ARCHITECTURE.md`. Their current
descriptions are based on a source skim (2026-08-29), not line-by-line verification:

- `AutoSyncScheduler` (notes, 3s debounce) and `AIConfigAutoSyncScheduler` (5s debounce)
  - see `docs/scenario/auto-sync.md`
- `SyncConflictDetector` (remote-newer detection) - see `docs/domain/sync/README.md`
- `AIConfigMergeAnalyzer` (three-way merge categorization) - see `docs/domain/ai-config/README.md`

Deep-dives live in `docs/`: use `.ai/context/INDEX.md` to route.
