# Tasks: Silent custom commands on Windows and macOS

- Unit: `202608-silent-command-cross-platform`

## DoD (Definition of Done)

All acceptance criteria in `spec.md` verified; evidence recorded in `.ai/runs/`;
docs backflow done; constitution constraints re-checked.

## Tasks

- [x] T1: Add `AIWorkspaceSilentLauncher` with Windows PATHEXT/sibling/well-known and macOS `open -a` resolution
- [x] T2: Wire silent `execute` to the launcher; detach GUI processes; i18n errors
- [x] T3: Focused unit tests for shim vs `.cmd`, Typora well-known path, macOS `open -a`, cmd wrapping
- [x] T4: Portable sample commands and help/docs for Windows + macOS
- [x] T5: `./gradlew test` on Java 17
- [x] Backflow: update `docs/` pages touched by this change to the new steady state
- [x] Update `.ai/context/INDEX.md` working set; freeze this unit
