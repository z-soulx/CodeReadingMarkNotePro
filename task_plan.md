# Seed first-run built-in workspace commands

## Goal

When `.ai/workspace-commands.json` is missing, write two deletable built-in commands (Cursor + Typora). If the file already exists, never re-inject them.

## Phases

- [completed] Spec unit `202608-ai-builtin-commands` + INDEX working set
- [completed] Seed in `AIWorkspaceCommandService` (missing file only) + i18n names
- [completed] Unit tests for built-in ids/fields/validate
- [completed] Help/docs/changeNotes 3.7.5 + freeze unit

## Next Step

Reload the plugin. On a project with `.ai/` and no `workspace-commands.json`, confirm the two commands appear. Delete them and confirm they stay gone.

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| | | |
