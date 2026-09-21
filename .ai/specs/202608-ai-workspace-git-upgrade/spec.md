# AI Workspace Git and Version Upgrade

- Status: frozen

## Goal

Upgrade the plugin to 3.7.4 and provide a project-level AI Workspace service for the `.ai` knowledge workspace, an opt-in `.ai` Git repository, strict Semver versioning, quick commits, and safe Terminal commands while preserving existing AI config sync formats.

## Scope

- Runtime docs are created under `.ai/docs`; there is no Docs Root toolbar action and moving docs out of `.ai` is a manual user operation.
- Git is initialized only after an explicit user action and is always executed with `.ai` as its working tree.
- `.ai/VERSION` defaults to `1.0.0`; patch bump and `chore: <version>` commit are supported.
- Skeleton creation offers three scopes: Workspace (`context`, `adr`, `specs`, `runs`), Notes Space (`docs` and its architecture/domain/scenario/integration/suppliers/shared/runbooks subdirectories), and All.
- Custom commands are visible in a single management dialog, persisted as `.ai/workspace-commands.json`, and edited through one shared form. Save confirms once in that form. Each command can choose IDEA Terminal or a silent background process; Run executes directly from the manager without another confirmation.
- Command arguments support `$FilePath$`, `$FileDir$`, and `$FileName$` macros. Each command chooses the file source: Project tool window selection (default; falls back to the editor) or the open editor file.
- Existing `AIConfigService`, `aiConfigRegistry.xml`, and remote metadata protocols remain unchanged.

## Acceptance

- Missing version/command state uses compatibility-safe defaults; docs start at `.ai/docs`.
- Git and AI config scanning ignore `.git`, `token.txt`, `runs/`, and temporary files.
- Invalid paths, Semver, executable paths, and shell control characters are rejected.
- New UI text exists in English and Chinese bundles; Git runs in background tasks and Terminal commands are dispatched through IDEA Terminal without detached process execution from the UI.

## Release Readiness

- Plugin version is `3.7.4` in both `build.gradle` and `META-INF/plugin.xml`.
- `META-INF/changeNotes.html`, `META-INF/description.html`, and the AI Workspace help guide describe the final behavior.
- `./gradlew test` passes on Java 17.
