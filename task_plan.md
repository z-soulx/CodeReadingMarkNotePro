# AI Workspace 与版本管理升级

## Goal

Implement the supplied AI Workspace upgrade plan, including service-layer support, UI integration, persistence-safe defaults, documentation, version 3.7.4, and verification.

## Phases

- [completed] Inspect existing AI workspace, persistence, UI, and build structure
- [completed] Add workspace models/services and persistence-safe behavior
- [completed] Integrate UI and i18n resources
- [completed] Add/update specs and documentation
- [completed] Update plugin version/change notes
- [completed] Build and verify

## Follow-up: Windows Terminal working directory

- [completed] Inspect `wt` command dispatch and define an explicit-directory fix
- [completed] Implement Windows Terminal command construction and focused tests
- [completed] Update the change spec/docs and run Gradle verification

## Follow-up: Silent command Windows/macOS launch

- [completed] Diagnose CreateProcess 193 (cursor unix shim) and 2 (typora.exe not on PATH)
- [completed] Implement OS-aware silent launcher and tests
- [completed] Portable sample commands and docs
- [pending] User reloads plugin and confirms Cursor/Typora silent run

## Follow-up: File source for $FilePath$

- [completed] Default macros to Project tool window selection, with editor fallback
- [completed] Add per-command fileSource combo (project vs editor)
- [pending] User reloads plugin, highlights a file in the Project tree, runs Typora

## Next Step

Rebuild/reload the plugin. For Typora, highlight the file in the Project tool window (need not open it), then Run. Switch File for $FilePath$ to editor if you want the open tab.

## Errors Encountered

- Initial Gradle invocation used the system Java 7 and failed; rerun with installed JDK 17.
- First panel compile used an unavailable `Messages.showInputDialog` overload; corrected to the IntelliJ 2024.3 signature.
