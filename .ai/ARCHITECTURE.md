# Architecture

## Domain Model

```
CodeReadingNoteService (project-level singleton)
└── TopicList
    └── Topic (name, datetime)
        └── TopicLine (url, line, description)
            └── Group (tag-based grouping, custom naming)
```

Core entities: `Topic` is a reading theme, `TopicLine` is an annotated code line within a topic, `Group` provides tag-based organization within topics, `TrashedLine` wraps deleted notes in a recoverable trash bin.

## Components

| Layer | Key Classes | Responsibility |
|-------|------------|----------------|
| Service | `CodeReadingNoteService` | State management, persistence via `PersistentStateComponent` |
| Domain | `Topic`, `TopicLine`, `TopicList`, `TrashedLine` | Business entities and operations |
| Sync | `SyncProvider`, `SyncService`, `GitHubSyncProvider` | Remote sync with conflict detection |
| AI Config | `AIConfigService`, `AIConfigRegistry`, `AIConfigSyncAdapter` | AI config file discovery, tracking, independent sync |
| UI | `ManagementPanel`, `TopicDetailPanel`, `AIWorkspacePanel`, `PushReportDialog` | ToolWindow panels (tabs: tree / search / AI workspace) |
| Gutter | `NoteGutterIconRenderer`, `NotePopupHelper` | Custom gutter icon + interactive edit popup |
| Actions | `TopicLineAddAction`, `NavigateToNoteAction`, etc. | User operations |

## Communication

Event-driven via IntelliJ `MessageBus`:
- `TopicListNotifier` -- topic list changes (add/remove topic, trash changes)
- `TopicNotifier` -- single topic changes (lines added/removed, `lineNoteChanged`)
- `AIConfigNotifier` -- AI config registry/file changes

## Data Storage

| File | Content | Scope |
|------|---------|-------|
| `CodeReadingNote.xml` | Topics, Lines, Groups | Synced to remote |
| `aiConfigRegistry.xml` | AI config tracked state, custom paths, ignore patterns, push hashes, tracked empty dirs | Project-level |
| `syncStatus.xml` | Sync timestamps, MD5 cache | Local only |
| `codeReadingNoteSync.xml` | Token, repo URL | Application-level |
| `ai-config-registry.json` (remote) | Cross-platform workspace metadata (tracked entries, custom paths, ignore patterns, file hashes, empty dirs) | Synced |

## Sync Architecture

Strategy pattern + factory: `SyncProvider` interface → `AbstractSyncProvider` → `GitHubSyncProvider`.

Two independent sync channels:
- **Notes**: `SyncService` → `push()`/`pull()` → `CodeReadingNote.xml`
- **AI Configs**: `AIConfigSyncAdapter` → `pushFiles()`/`pullFiles()` → `ai-configs/` directory + manifest

### Push Behavior

Incremental MD5-based push:
1. Combined hash check (tracked file paths + content + tracked empty dirs) — skips entire push if nothing changed
2. Per-file MD5 comparison against `lastPushedFileHashes` — only changed files are actually uploaded
3. Failed files excluded from hash recording — automatically retried on next push
4. Manifest diff (old vs new) → DELETE API for files no longer tracked
5. Empty dirs synced as `.gitkeep` placeholders; tracked empty dir state persisted in `AIConfigService`
6. Force push ignores MD5 and re-uploads all tracked files

### Workspace Metadata Sync

`ai-config-registry.json` pushed/pulled alongside files:
- `customPaths`: user-added scan directories
- `ignorePatterns`: user-defined ignore rules
- `trackedEntries`: per-file tracked state (path + tracked flag + type)
- `lastPushedFileHashes`: per-file content hashes
- `trackedEmptyDirs`: explicitly checked empty directories

Pull applies metadata in two phases:
1. **Pre-rescan**: `customPaths` + `ignorePatterns` (affect file discovery)
2. **Post-rescan**: `trackedEntries` + `fileHashes` + `trackedEmptyDirs` (apply to newly created entries)

### Error Handling

GitHub API errors are parsed with `formatApiError()`:
- JSON responses: extract `message` field
- HTML responses: extract `<title>`, decode HTML entities (`&middot;`, `&mdash;`, etc.)
- Actionable advice appended by HTTP status code (401 → check token, 404 → check repo, etc.)
- Non-ASCII file paths URL-encoded per path segment

### Push Report UI

`PushReportDialog` shows structured results after every push:
- Summary header with status icon and statistics
- Collapsible sections: Pushed, Skipped (unchanged), Failed (with reasons), Deleted, Empty Dirs
- "Force Push All" button integrated — user decides based on statistics
- "No changes" case shows informational message with force push option

## AI Config Layer

```
AIConfigService (project-level, PersistentStateComponent)
├── AIConfigRegistry
│   ├── scan() -- discovers files + empty dirs under known paths + custom paths
│   ├── AIConfigEntry[] -- tracked files with content hashes
│   ├── discoveredDirs -- all directories including empty ones
│   ├── userIgnorePatterns -- configurable ignore rules (persisted)
│   └── collectTrackedFilesContent() -- for sync
├── AIConfigSyncAdapter -- bridges to SyncProvider.pushFiles/pullFiles
│   ├── findEmptyTrackedDirs() -- reads persisted tracked empty dirs
│   ├── pushWorkspaceMetadata() -- serializes and pushes ai-config-registry.json
│   └── pullAndApplyMetadata() -- two-phase apply (pre-rescan + post-rescan)
├── PersistentState
│   ├── customPaths, ignorePatterns, trackedEntries
│   ├── lastPushedHash, lastPushedFileHashes
│   └── trackedEmptyDirs -- explicitly checked empty directories
└── VFS listener -- real-time file change detection
```

Known config types: `AIConfigType` enum (Cursor Rules, Claude, AI Docs, Windsurf, Codex, Copilot, Custom).

Ignore system: built-in patterns (`.DS_Store`, `*.swp`, etc.) + user-configurable patterns (persisted in `aiConfigRegistry.xml`). Supports exact name, `*.ext`, `prefix*`, and `dir/` path patterns.

Empty directory support: `discoveredDirs` set tracks all directories (including empty ones) during scan. Users explicitly check empty dir checkboxes in the tree to track them. Tracked empty dirs are persisted in `AIConfigService.PersistentState.trackedEmptyDirs` and synced as `.gitkeep` files on remote.

Tree UI: `AIConfigTreePanel` builds hierarchical `VirtualDir` tree from file paths + discovered dirs, with per-file sync status indicators (NEW/MODIFIED/SYNCED) based on stored push hashes. Adaptive expand/collapse button detects actual tree state via `hasAnyExpanded()`.

## Core Use Cases

### Create Topic
User → ToolWindow "create" button → `NewTopicDialog` → `TopicList.addTopic()` → MessageBus → UI refresh

### Add Line to Topic
User → Editor cursor → `AddToTopic` action → `AddTopicLineDialog` → `Topic.addTopicLine()` → persist

### Push AI Configs
User → AI Workspace tab → Push button → combined hash check → `AIConfigSyncAdapter.pushAIConfigs()` → per-file MD5 comparison → `GitHubSyncProvider.pushFiles()` → `PushReportDialog` (with Force Push option) → update hashes

### Pull AI Configs
User → AI Workspace tab → Pull button → `AIConfigSyncAdapter.pullAIConfigs()` → download files → apply metadata (pre-rescan: paths/patterns, post-rescan: tracked state/hashes/empty dirs) → refresh tree

## Class Diagram (PlantUML)

```plantuml
@startuml
class CodeReadingNoteService {
    - project
    - topicList
    + getState()
    + loadState()
    + listSource(project, file)
}

class TopicList {
    + addTopic()
    + removeTopic()
    + addTopicLine(Topic)
    + removeTopicLine()
}

class Topic { - name; - datetime }
class TopicLine { - url; - line; - description }

class AIConfigService {
    - registry: AIConfigRegistry
    - persistentState
    + rescan()
    + getLastPushedHash()
    + getLastPushedFileHashes()
    + getTrackedEmptyDirs()
    + setTrackedEmptyDirs()
}

class AIConfigRegistry {
    + scan()
    + getTrackedEntries()
    + getDiscoveredDirs()
    + getUserIgnorePatterns()
    + computeTrackedContentHash()
}

class AIConfigSyncAdapter {
    + pushAIConfigs(config, id, forceAll)
    + pullAIConfigs(config, id)
    - findEmptyTrackedDirs()
    - serializeMetadata()
    - applyMetadataPreRescan()
    - applyMetadataPostRescan()
}

class FilePushReport {
    + skippedFiles
    + pushedFiles
    + failedFiles
    + deletedFiles
    + emptyDirsSynced
    + toJson()
    + fromJson()
}

package ui {
    class ManagementPanel
    class TopicDetailPanel
    class AIWorkspacePanel
    class AIConfigTreePanel
    class PushReportDialog
    ManagementPanel o-- TopicDetailPanel
    ManagementPanel o-- AIWorkspacePanel
    AIWorkspacePanel o-- AIConfigTreePanel
    AIWorkspacePanel ..> PushReportDialog
}

interface TopicListNotifier
interface TopicNotifier
interface AIConfigNotifier

CodeReadingNoteService o-- TopicList
TopicList o-- Topic
Topic o-- TopicLine
AIConfigService o-- AIConfigRegistry
AIWorkspacePanel ..> AIConfigSyncAdapter
AIConfigSyncAdapter ..> FilePushReport
@enduml
```
