# Spec: Keep project commits from including .ai files

- Unit: `202608-ai-git-commit-isolation`
- Status: frozen
- Type: bugfix

## Intent (WHY)

The native Commit window lists `.ai` files in the same Changes group as project files. A project commit message then takes `.ai` along. Isolation must happen even before nested Git exists, and already-tracked `.ai` paths must be removed from the parent index.

## Background

3.7.5 added `/.ai/` to `.gitignore` and a Directory Mapping, but only after Initialize .ai Git. Gitignore does not untrack files already in the parent index, so they stay in the default Changes list. See `docs/domain/ai-config/README.md`.

## Scope

In scope: parent isolation whenever `.ai/` exists; `git rm --cached` of tracked `.ai` paths in the parent repo (no auto-commit); dedicated `.ai` changelist; Open .ai Git still opens native UI. Out of scope: rewriting already-pushed history; a second Commit tool window.

## Acceptance Criteria

- [x] Opening a project that has `.ai/` writes `/.ai/` into the project `.gitignore` even if nested Git is not initialized.
- [x] Tracked `.ai` paths are removed from the parent index (`git rm -r --cached`); the working tree is kept. A one-time information balloon explains that the parent repo will show those deletions until the user commits them.
- [x] After that parent cleanup commit, project Commits no longer list `.ai` files in the default Changes group.
- [x] Changes under `.ai` that still appear (nested Git mapping) live in a dedicated `.ai` changelist, not Default.
- [x] Existing Initialize / Open .ai Git / Commit Workspace Version still work. New strings are bilingual. `./gradlew test` passes.
