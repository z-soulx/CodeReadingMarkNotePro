# Spec: First-run built-in workspace commands

- Unit: `202608-ai-builtin-commands`
- Status: frozen
- Type: feature

## Intent (WHY)

New `.ai` workspaces start with an empty command list. Users repeatedly add the same two silent commands (open Cursor on the project, open the selected Markdown in Typora). Those should appear once as ordinary saved commands so they can be edited or deleted; they must not come back after the user removes them.

## Background

Custom commands already persist in `.ai/workspace-commands.json` (`docs/domain/ai-config/README.md`). `load()` returns an empty list when the file is missing. Help documents Cursor and Typora as copy-paste examples, not as first-run defaults.

## Scope

In scope: seed two commands when the JSON file is absent and `.ai/` already exists; bilingual display names; tests; help/docs/changeNotes.

Out of scope: merging into existing JSON; recreating deleted commands; changing execution or Git UI.

## Acceptance Criteria

- [x] If `.ai/` exists and `workspace-commands.json` does not, the plugin writes schemaVersion 1 with `launch-cursor-project` (`cursor` `.`, silent) and `open-selected-md-in-typora` (`typora` `$FilePath$`, silent, fileSource project).
- [x] If `workspace-commands.json` already exists (including `commands: []`), those two ids are not re-inserted.
- [x] Display names come from i18n bundles; users can delete the commands and the empty/remaining list is saved.
- [x] `./gradlew test` passes.
