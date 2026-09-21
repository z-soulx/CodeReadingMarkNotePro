# Plan: Restore UI entry points

- Unit: `202609-ui-entrypoint-regressions`

## Design

Keep `META-INF/pluginIcon.svg` as Marketplace metadata only. Use a plugin-unique `/icons/codeReadingMarkNote.svg` runtime resource directly in `plugin.xml` for the Tool Window and editor actions, and make `MyIcons.PLUGIN` load the same dedicated resource. This avoids both reflective descriptor lookup and the special `META-INF/pluginIcon.svg` path that produced placeholder chevrons in a real IntelliJ 2024.3 sandbox.

Keep Add, Save, Delete, and Run in the existing action row in `AIWorkspaceCommandsDialog`. After all controls and borders are installed, derive the content's natural preferred size and use it together with a scaled 900x500 minimum. This lets macOS button metrics expand the dialog while preserving the existing Windows baseline and current `AIWorkspaceCommandService.runConfigured()` execution path.

Do not add a separate Run action to `AIWorkspacePanel`.

## Impact

| Area | Files / modules | Risk |
|------|----------------|------|
| Plugin registration | `META-INF/plugin.xml`, `icons/codeReadingMarkNote.svg`, `icons.MyIcons` | Invalid resource path could hide icons; validate in the actual 2024.3 plugin sandbox, not only a standalone resolver |
| AI Workspace UI | `AIWorkspaceCommandsDialog` | Platform-specific control metrics or high-DPI scaling could clip the action row |
| Release/docs | version, change notes, plugin description, AI workspace help/docs | Documentation drift |

## Constraints Checked

- [x] Constitution: no new UI text; existing button labels remain bundle-backed
- [x] Constitution: persisted-format backward compatibility
- [x] Resource cleanup (no new listeners or schedulers)

## Alternatives Considered

Add a direct Run action to the AI Workspace toolbar: rejected because the confirmed requirement is to restore the existing manager-dialog button, and a second entry would crowd the toolbar and duplicate command-selection behavior.

Keep a hard-coded 900px preferred width: rejected because it overrides the platform-derived width and clips the macOS Run button.

Use `MyIcons.PLUGIN` or `icons.MyIcons.PLUGIN`: rejected after installed-plugin acceptance because reflective descriptor loading did not restore the icon.

Use `/META-INF/pluginIcon.svg` directly: rejected after the 2024.3 `runIde` sandbox loaded a fresh 3.7.6 JAR containing that path but still rendered placeholder chevrons. The standalone resolver result was not representative of the real descriptor-loading path.
