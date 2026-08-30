# Spec: Silent custom commands on Windows and macOS

- Unit: `202608-silent-command-cross-platform`
- Status: frozen
- Type: bugfix

## Intent (WHY)

Silent mode is a real background launch path, not documentation. Windows users currently
cannot silently start Cursor or Typora: `CreateProcess` error 193 hits the Unix `cursor`
shim, and error 2 hits `typora.exe` when it is not on PATH. The same saved command names
must work on Windows and macOS.

## Background

`docs/domain/ai-config/README.md` describes silent mode as a `ProcessBuilder` launch.
IDEA Terminal works because the shell resolves PATH, `.cmd`, and GUI apps. Silent mode
calls `CreateProcess` directly, prefers the extensionless `cursor` file over `cursor.cmd`,
does not search Program Files, and waits until GUI apps exit.

## Scope

In scope: silent executable resolution, Windows PATHEXT / sibling `.cmd`/`.exe`, well-known
install locations, macOS `open -a` fallback, GUI detach vs CLI wait, portable sample
commands (`cursor`, `typora`), i18n errors, help/docs.

Out of scope: Terminal-mode shell built-ins, new command schema fields, Linux-specific
app bundles.

## Acceptance Criteria

- [x] Silent mode remains a real launch path (IDEA Terminal without focus); it is not a description-only flag.
- [x] Silent Terminal starts the shell immediately (`deferSessionStartUntilShown=false`) so commands run without opening/focusing the tool window.
- [x] On Windows, an extensionless `cursor` shim is not launched; `cursor.cmd` or `cursor.exe` is used instead (avoids CreateProcess 193).
- [x] On Windows, `typora` / `typora.exe` resolves via PATH or well-known Program Files / LocalAppData locations (avoids CreateProcess 2 when the app is installed but not on PATH).
- [x] On macOS, `cursor` and `typora` run from PATH when present, otherwise `open -a`.
- [x] Sample `.ai/workspace-commands.json` uses portable names that work on Windows and macOS.
- [x] GUI silent launches do not block until the app window is closed; CLI silent launches still wait for exit code.
- [x] New user-facing errors exist in English and Chinese bundles.
- [x] `./gradlew test` passes on Java 17.
