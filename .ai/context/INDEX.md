# Context Index - Navigation & Loading Routes

Single entry point for deciding what to read before working. Read this file first; it routes
to everything else. File paths are relative to the repository root.

## Routing Decision (read before writing any doc)

| You are about to... | It goes to |
|---|---|
| Change the system (goal, scope, acceptance, tasks) | `.ai/specs/<yyyymm>-<slug>/` (see `.ai/specs/README.md`) |
| Describe the current system (architecture, domain knowledge, status quo) | `docs/` (Chinese, narrative) or `.ai/context/system-overview.md` (facts index) |
| Record why a long-lived decision was made | `.ai/adr/` (append-only, never rewrite a merged ADR) |
| Record execution evidence (commands, outputs, summaries) | `.ai/runs/` (gitignored, pure record, no reflection) |

Do not retrofit `specs/` structure onto live systems: a spec that has no pending acceptance
criteria is a sign it belongs in `docs/` instead.

## Load Routes by Task

| Task | Load (in order) |
|---|---|
| Any code change | `.ai/constitution.md` -> relevant route below |
| Notes domain (topics, lines, groups, trash, gutter/popup) | `docs/domain/notes/README.md` |
| Notes sync / auto-sync / conflicts | `docs/domain/sync/README.md` -> `docs/scenario/auto-sync.md` -> `docs/domain/sync/github.md` |
| AI config workspace (scan, ignore, tree UI, types) | `docs/domain/ai-config/README.md` -> `.ai/adr/0004-empty-dirs-as-gitkeep.md`, `.ai/adr/0005-two-phase-metadata-apply.md` |
| GitHub API behavior / error messages | `docs/integration/github-api.md` |
| IntelliJ platform dependencies (state, message bus, VFS, bookmarks) | `docs/integration/intellij-platform.md` |
| UI / i18n work | Constitution "Internationalization" + `src/main/resources/messages/` |
| Release | `docs/runbooks/release.md` |
| Terminology doubt | `docs/shared/glossary.md` |
| "What is this project / where is everything" | `.ai/context/system-overview.md` |

## Current Working Set

`202609-ui-entrypoint-regressions` - verification: 3.7.6 built; Windows/macOS installed-plugin checks pending.

When a spec is opened, list it here with a one-line status; remove the line when the
unit is frozen. (This is the only volatile section of this file.)

## Update Obligation

- Structure changes (new doc, moved doc): update this index and `docs/README.md` in the same change
- Completed change: follow the backflow step in `.ai/specs/README.md` (update `docs/` to the new steady state)
- This file is a contract, not a history: overwrite in place, no changelogs here
