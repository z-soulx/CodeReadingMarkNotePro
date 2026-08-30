# Findings

- Constitution requires full i18n coverage, backward-compatible persistence, no blocking EDT I/O, and semver consistency.
- Current repository has no planning files and no automated test suite according to project context.
- The custom command manager dispatches commands through `TerminalToolWindowManager.createLocalShellWidget` and then `executeCommand`.
- A saved command with executable `wt` currently sends only `wt` after setting the IDEA Terminal widget directory. Windows 10 Windows Terminal can open a new tab/window at its profile default instead of the inherited shell directory.
- Silent mode now uses the same IDEA Terminal shell as Terminal mode, without focusing the tool window. Previously silent ProcessBuilder hit CreateProcess 193/2; previously unfocused Terminal passed `deferSessionStartUntilShown=true`, so the command never ran until the user opened Terminal.
- `$FilePath$` previously used only the open editor file, so Typora opened `workspace-commands.json` when that tab was focused. Macros now use Project tool window selection by default (file need not be open), with an optional editor-only mode.
