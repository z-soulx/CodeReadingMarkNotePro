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
| UI | `ManagementPanel`, `TopicDetailPanel`, `AIWorkspacePanel` | ToolWindow panels (tabs: tree / search / AI workspace) |
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
| `aiConfigRegistry.xml` | AI config tracked state, custom paths, ignore patterns, push hashes | Project-level |
| `syncStatus.xml` | Sync timestamps, MD5 cache | Local only |
| `codeReadingNoteSync.xml` | Token, repo URL | Application-level |

## Sync Architecture

Strategy pattern + factory: `SyncProvider` interface → `AbstractSyncProvider` → `GitHubSyncProvider`.

Two independent sync channels:
- **Notes**: `SyncService` → `push()`/`pull()` → `CodeReadingNote.xml`
- **AI Configs**: `AIConfigSyncAdapter` → `pushFiles()`/`pullFiles()` → `ai-configs/` directory + manifest

Push behavior: pushes tracked files, deletes remote files no longer tracked (reads old manifest → diffs → DELETE API), includes empty dir markers in manifest.

Change detection: MD5 hash comparison (combined hash for notes, per-file hashes + discovered dirs for AI configs).

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
│   └── findEmptyTrackedDirs() -- identifies empty dirs for manifest
└── VFS listener -- real-time file change detection
```

Known config types: `AIConfigType` enum (Cursor Rules, Claude, AI Docs, Windsurf, Codex, Copilot, Custom).

Ignore system: built-in patterns (`.DS_Store`, `*.swp`, etc.) + user-configurable patterns (persisted in `aiConfigRegistry.xml`). Supports exact name, `*.ext`, `prefix*`, and `dir/` path patterns.

Empty directory support: `discoveredDirs` set tracks all directories (including empty ones) during scan, displayed in tree and included in sync manifest as `path/` markers.

Tree UI: `AIConfigTreePanel` builds hierarchical `VirtualDir` tree from file paths + discovered dirs, with per-file sync status indicators (NEW/MODIFIED/SYNCED) based on stored push hashes.

## Core Use Cases

### Create Topic
User → ToolWindow "create" button → `NewTopicDialog` → `TopicList.addTopic()` → MessageBus → UI refresh

### Add Line to Topic
User → Editor cursor → `AddToTopic` action → `AddTopicLineDialog` → `Topic.addTopicLine()` → persist

### Push AI Configs
User → AI Workspace tab → Push button → hash comparison → `AIConfigSyncAdapter.pushAIConfigs()` → `GitHubSyncProvider.pushFiles()` → update per-file hashes

## Class Diagram (PlantUML)

```plantuml
@startuml
class CodeReadingNoteService {
    - project
    - topicList
    + getState()
    + loadState()
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
    + rescan()
    + getLastPushedHash()
    + getLastPushedFileHashes()
}

class AIConfigRegistry {
    + scan()
    + getTrackedEntries()
    + getDiscoveredDirs()
    + getUserIgnorePatterns()
    + computeTrackedContentHash()
}

class AIConfigSyncAdapter {
    + pushAIConfigs()
    + pullAIConfigs()
}

package ui {
    class ManagementPanel
    class TopicDetailPanel
    class AIWorkspacePanel
    class AIConfigTreePanel
    ManagementPanel o-- TopicDetailPanel
    ManagementPanel o-- AIWorkspacePanel
    AIWorkspacePanel o-- AIConfigTreePanel
}

interface TopicListNotifier
interface TopicNotifier
interface AIConfigNotifier

CodeReadingNoteService o-- TopicList
TopicList o-- Topic
Topic o-- TopicLine
AIConfigService o-- AIConfigRegistry
AIWorkspacePanel ..> AIConfigSyncAdapter
@enduml
```
