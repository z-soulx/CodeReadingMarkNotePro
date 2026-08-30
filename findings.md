# Findings

- `AIWorkspaceCommandService.load()` currently returns an empty list when `.ai/workspace-commands.json` is missing; it never writes defaults.
- `upsert()` / `delete()` persist whatever `load()` returns. Seeding only when the file is absent means deleting commands (including emptying `commands: []`) sticks.
- Existing projects that already have the JSON must not get a forced merge of `launch-cursor-project` / `open-selected-md-in-typora`.
- Seeded `displayName`/`name` must come from bundles (constitution: no hardcoded UI strings). IDs stay as given.
- `validate()` accepts portable names `cursor` / `typora` and `$FilePath$`. Do not create `.ai/` just to seed; only seed when that directory already exists.
- Startup already runs when `.ai/` exists (`AIWorkspaceVcsStartupActivity`); that is the right place to write the file without waiting for the command dialog.
- Help currently documents Typora as id `open-selected-md`; the built-in id is `open-selected-md-in-typora`.
