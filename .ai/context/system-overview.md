# System Overview - Current State Facts

What exists right now, as a compact fact sheet. Narrative explanations live in `docs/`;
decisions and their rationale live in `.ai/adr/`.

## Identity

- IntelliJ IDEA plugin `soulx.CodeReadingMarkNotePro`: code reading notes and bookmarks.
  Pro adds group management, GitHub sync, multi-language UI, AI config workspace.
- Java 17 + Gradle + IntelliJ Platform 2024.3+. Tests under `src/test` cover workspace Semver/commands, nested notes discovery/ownership, compatible XML/conflicts, model operations and shared-window coordination. UI/sync verification still uses `gradlew build` plus installed-plugin acceptance.

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

Workspace notes (3.7.7): `notesworkspace.WorkspaceNotesService` discovers nested `.idea` projects;
`WorkspaceNotesCoordinator` shares data per normalized root across open windows; `WorkspaceXmlStore`
stores child XML with conflict detection and atomic replacement. `NoteProjectContext` carries runtime
ownership. Root `CodeReadingNoteService` state remains root-only; 3.7.7 routes notes sync explicitly to each project's collection. Recovery snapshots live under
the IDEA configuration directory's `CodeReadingNote/workspace-recovery/`.

| Layer | Key Classes | Responsibility |
|-------|------------|----------------|
| Service | `CodeReadingNoteService` | State management, persistence via `PersistentStateComponent` |
| Domain | `Topic`, `TopicLine`, `TopicList`, `TrashedLine` | Business entities and operations |
| Sync | `WorkspaceNotesSyncCoordinator`, `NotesSyncBinding`, `NotesSyncDecision`, `NotesSyncSchedule`, `GitHubNotesRemote`; root `SyncService` facade | Per-project manual/bidirectional notes sync, baseline comparison, conditional SHA writes; AI uses existing provider channel |
| AI Config | `AIConfigService`, `AIConfigRegistry`, `AIConfigSyncAdapter`, `AIConfigAutoSyncScheduler`, `AIConfigMergeAnalyzer` | AI config discovery, tracking, independent sync, three-way merge |
| AI Workspace | `AIWorkspaceService`, `AIWorkspaceGitService`, `AIWorkspaceVcsSupport`, `AIWorkspaceChangeListService`, `AIWorkspaceVersionService`, `AIWorkspaceCommandService` | `.ai/docs` knowledge workspace, opt-in `.ai` Git, parent `/.ai/` ignore and untrack, IDEA Directory Mapping + `.ai` changelist + native Commit UI, Semver VERSION, Terminal or silent-unfocused Terminal commands with IDEA file macros |
| UI | `ManagementPanel`, `TopicDetailPanel`, `AIWorkspacePanel`, `PushReportDialog` | ToolWindow panels (tabs: tree / search / AI workspace) |
| Gutter | `NoteGutterIconRenderer`, `NotePopupHelper` | Custom gutter icon + interactive edit popup |
| Actions | `TopicLineAddAction`, `NavigateToNoteAction`, etc. | User operations |

## Data Storage

| File | Content | Scope |
|------|---------|-------|
| `CodeReadingNote.xml` | Topics, Lines, Groups | Synced to remote |
| `aiConfigRegistry.xml` | AI config tracked state, custom paths, ignore patterns, push hashes, tracked empty dirs | Project-level |
| `syncStatus.xml` | Sync timestamps, MD5 cache | Local only |
| `.idea/notesSyncBinding.xml` | Remote identity, policy, check interval; no token | Each note project |
| IDEA config `CodeReadingNote/notes-sync/` | Partitioned notes baselines, pause, apply journals and backups | Local only, application-coordinated |
| `codeReadingNoteSync.xml` | Token, repo URL | Application-level |
| `ai-config-registry.json` (remote) | Cross-platform workspace metadata (tracked entries, custom paths, ignore patterns, file hashes, empty dirs) | Synced |
| `aiWorkspace.xml` | Runtime docs root (defaults to `.ai/docs`) | Project-level local |
| `.ai/VERSION` | Strict Semver workspace version (defaults to `1.0.0`) | `.ai` workspace |
| `.ai/workspace-commands.json` | Custom commands, schemaVersion 1. Missing file is seeded once with Cursor/Typora when `.ai/` exists. | `.ai` workspace |

## Events

IntelliJ `MessageBus`: `TopicListNotifier` (topic list changes), `TopicNotifier` (single
topic changes, `lineNoteChanged`), `AIConfigNotifier` (AI config registry/file changes),
`SyncStatusNotifier` (legacy root/AI status changes), `NotesSyncNotifier` (notes sync status with project ownership).

## Known Documentation Gaps

The following were never covered by the old `.ai/ARCHITECTURE.md`. Their current
descriptions are based on a source skim (2026-08-29), not line-by-line verification:

- `AIConfigAutoSyncScheduler` (5s debounce); notes scheduling was documented from the 3.7.7 implementation
  - see `docs/scenario/auto-sync.md`
- `AIConfigMergeAnalyzer` (three-way merge categorization) - see `docs/domain/ai-config/README.md`

Deep-dives live in `docs/`: use `.ai/context/INDEX.md` to route.
