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
| UI | `ManagementPanel`, `TopicDetailPanel`, `TopicLineDetailPanel` | ToolWindow panels (JBList + JBSplitter) |
| Gutter | `NoteGutterIconRenderer`, `NotePopupHelper` | Custom gutter icon + interactive edit popup |
| Actions | `TopicLineAddAction`, `NavigateToNoteAction`, `ReverseLocateAction`, etc. | User operations |
| Integration | Editor inlay, bookmark linkage, i18n bundle | IDE integration |

## Communication

Event-driven via IntelliJ `MessageBus`:
- `TopicListNotifier` -- topic list changes (add/remove topic, trash changes)
- `TopicNotifier` -- single topic changes (lines added/removed, `lineNoteChanged`)

Panels subscribe to notifiers; domain objects publish through MessageBus.

## Data Storage

| File | Content | Scope |
|------|---------|-------|
| `CodeReadingNote.xml` | Topics, Lines, Groups | Synced to remote |
| `syncStatus.xml` | Sync timestamps, MD5 cache | Local only |
| `codeReadingNoteSync.xml` | Token, repo URL | Application-level |

## Sync Architecture

Strategy pattern + factory: `SyncProvider` interface -> `AbstractSyncProvider` -> `GitHubSyncProvider`.

State machine: DIRTY -> PENDING -> SYNCING -> SYNCED (or ERROR)

Conflict detection: MD5 + timestamp dual verification. Auto-sync triggers on data persistence with debounce.

## Core Use Cases

### Create Topic
User -> ToolWindow "create" button -> `NewTopicDialog` (validate name) -> `TopicList.addTopic()` -> MessageBus publish -> UI refresh

### Add Line to Topic
User -> Editor cursor on line -> `AddToTopic` action -> `AddTopicLineDialog` (select topic, optional comment) -> `Topic.addTopicLine()` -> persist

### Add Line + Create Topic (combined)
Same as above, but user clicks "create new topic" in `AddTopicLineDialog` -> opens `NewTopicDialog` inline -> creates topic -> auto-selects it -> continues adding line

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

package ui {
    class ManagementPanel
    class TopicDetailPanel { + setTopic(Topic); + clear() }
    class TopicLineDetailPanel { + setTopicLine(TopicLine) }
    ManagementPanel o-- TopicDetailPanel
    TopicDetailPanel o-- TopicLineDetailPanel
}

package actions {
    class TopicAddAction
    class TopicRemoveAction
    class TopicLineAddAction
    class TopicLineRemoveAction
}

interface TopicListNotifier
interface TopicNotifier

CodeReadingNoteService o-- TopicList
TopicList o-- Topic
Topic o-- TopicLine
TopicList ..> TopicListNotifier : publish
Topic ..> TopicNotifier : publish
ManagementPanel ..> TopicListNotifier : subscribe
TopicDetailPanel ..> TopicNotifier : subscribe
@enduml
```
