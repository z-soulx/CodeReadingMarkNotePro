# Plan: Silent custom commands on Windows and macOS

- Unit: `202608-silent-command-cross-platform`

## Design

Extract silent argv construction into `AIWorkspaceSilentLauncher` (pure, testable):

1. Windows PATH search uses PATHEXT (`.exe`, `.cmd`, `.bat`, `.com`) before any extensionless file.
2. Absolute path to a Unix shim prefers a sibling `.cmd`/`.exe`.
3. If still unresolved, search `ProgramFiles`, `ProgramFiles(x86)`, and `%LOCALAPPDATA%\Programs`
   for `<Name>\<Name>.exe` and Cursor's `resources\app\bin\cursor.cmd`.
4. `.cmd`/`.bat` still wrap with `cmd.exe /c`. Extensionless files are never passed to `CreateProcess`.
5. macOS: PATH binary first; else `open -a <App>` using `/Applications` or `~/Applications`.
6. GUI launches (`open`, `*.exe` except `java.exe`, `cursor.cmd`, Typora) detach after a short
   immediate-failure wait. `.bat` CLI tools keep `waitFor` and captured output.

`AIWorkspaceCommandService.execute` uses the launcher; Terminal mode is unchanged.

## Impact

| Area | Files / modules | Risk |
|------|----------------|------|
| Silent launch | `AIWorkspaceSilentLauncher`, `AIWorkspaceCommandService` | Wrong PATHEXT order could still pick a shim |
| UI errors | `CodeReadingNoteBundle*.properties` | Missing i18n keys |
| Samples / help | `.ai/workspace-commands.json`, `help/AI_WORKSPACE_GUIDE.md`, `docs/domain/ai-config/README.md` | Users with old absolute Windows paths still work via sibling/well-known resolution |
| Tests | `AIWorkspaceCommandServiceTest` | Temp-dir filesystem assumptions |

## Constraints Checked

- [x] Constitution: i18n coverage for any new UI text
- [x] Constitution: persisted-format backward compatibility (new fields have safe defaults)
- [x] Resource cleanup (listeners, schedulers)

## Alternatives Considered

- Always send silent commands through `cmd.exe` / `sh -c`: reintroduces shell-control-character risk rejected by the original spec.
- OS-specific command JSON entries: breaks `.ai` Git sharing across Windows and macOS.
- Registry App Paths lookup: extra native surface; Program Files + LocalAppData cover Cursor and Typora.
