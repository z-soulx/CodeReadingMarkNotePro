# Progress

## 2026-08-29

- Loaded `.ai/constitution.md`, `.ai/context/INDEX.md`, `.ai/context/system-overview.md`, and planning skill instructions.
- Created planning files for this implementation.
- Added workspace services for docs-root persistence, opt-in `.ai` Git, Semver VERSION, and JSON command storage/execution.
- Integrated docs-root, Git initialization, version bump, and quick commit actions into `AIWorkspacePanel` with background tasks.
- Added bilingual messages, spec unit, context/docs/release updates, version 3.7.4, and change notes.
- `JAVA_HOME=C:\\Program Files\\Java\\jdk-17 ./gradlew build` passed before adding focused tests.
- Added pure Semver unit coverage in `AIWorkspaceVersionServiceTest`; `./gradlew test` passes.
- Incorporated follow-up UX feedback: removed Docs Root toolbar action, changed Git/version icons, added nested `.ai/.git` ignore, replaced skeleton presets with Workspace/Notes Space/All, and rewrote Quick Reference with bilingual detailed structure notes.
- Added toolbar flows for adding and running custom workspace commands with confirmation and background execution; scanner now ignores `.git` directories by default. Quick Reference uses separated cards, spacing, and wrapped bilingual text.
- Final verification: `JAVA_HOME=C:\\Program Files\\Java\\jdk-17 ./gradlew test` passes after the follow-up UI and ignore-rule changes.
- Updated custom command execution to send quoted commands into IDEA Terminal via `TerminalToolWindowManager`; plugin declares an optional Terminal dependency and no longer uses the detached process path from the UI.
- Final UX adjustment: Quick Reference is expanded and scrollable by default; standalone Run Command toolbar action and manager confirmation prompt were removed so commands execute directly from the manager list.
- Refactored command management to one split list/form dialog: toolbar now exposes only Manage, Add/Edit reuse the same form, Save confirms once, and Run executes immediately.
- Added selectable Terminal/silent execution modes, IDEA file macros (`$FilePath$`, `$FileDir$`, `$FileName$`), and seeded `.ai/workspace-commands.json` with Cursor and Typora examples.
- Synchronized help, AI config domain docs, release runbook, and system overview with the two sample commands and execution modes.
- Fixed command manager sizing, Windows `.cmd/.bat` silent launching, Typora absolute-path sample, and added final Cursor/Typora usage examples to Help.
- 2026-08-30: Diagnosed Windows 10 `wt` working-directory inheritance issue. Added explicit `-d` directory argument for `wt`/`wt.exe` terminal commands, focused unit tests, follow-up spec, and bilingual help guidance.
- 2026-08-30: `JAVA_HOME=C:\Program Files\Java\jdk-17 .\gradlew.bat test` passed (Gradle cache required escalated access because it is outside the workspace).
- 2026-08-30: Silent action uses IDEA Terminal without focus (`deferSessionStartUntilShown=false`) so Cursor/Typora run the same way as Terminal mode. ProcessBuilder remains as a non-UI helper.
- 2026-08-30: `$FilePath$` defaults to Project tool window selection (file need not be open; falls back to editor). Each command can switch to editor-only. Typora sample sets `fileSource: project`.
