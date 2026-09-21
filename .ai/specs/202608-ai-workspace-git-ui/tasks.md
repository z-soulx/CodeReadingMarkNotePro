# Tasks: .ai independent Git and IDEA Commit UI

- Unit: `202608-ai-workspace-git-ui`

## DoD (Definition of Done)

All acceptance criteria in `spec.md` verified; evidence recorded in `.ai/runs/`;
docs backflow done; constitution constraints re-checked.

## Tasks

- [x] T1: Parent gitignore isolation in `AIWorkspaceGitService` + this repo `.gitignore`; unit tests for the ignore helper
- [x] T2: `AIWorkspaceVcsSupport` Directory Mapping + startup activity registered in `plugin.xml`
- [x] T3: Open .ai Git toolbar action (`CheckinFiles`, Commit tool window fallback)
- [x] T4: i18n keys, version 3.7.5, changeNotes, help, docs
- [x] T5: `./gradlew test`
- [x] Backflow: update `docs/` pages touched by this change to the new steady state
- [x] Update `.ai/context/INDEX.md` working set; freeze this unit
