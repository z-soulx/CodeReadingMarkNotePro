# Spec: Restore UI entry points

- Unit: `202609-ui-entrypoint-regressions`
- Status: verification (Windows/macOS installation checks pending)
- Type: bugfix

## Intent (WHY)

The plugin icon is missing from the Tool Window stripe and from the Add to Topic and Navigate to Note editor actions. On macOS, Manage Custom Commands can clip the rightmost Run Custom Command button because the dialog forces a fixed preferred width instead of honoring the platform controls' actual preferred sizes.

## Background

The notes lifecycle and editor actions are described in `docs/scenario/note-lifecycle.md`. Custom commands are described in `docs/domain/ai-config/README.md` and `help/AI_WORKSPACE_GUIDE.md`.

## Scope

In scope: repair plugin icon references; keep Add, Save, Delete, and Run in one manager-dialog action row while sizing the dialog from the platform controls; release/current-system documentation; version 3.7.6.

Out of scope: adding an AI Workspace toolbar Run action; changing command persistence, execution modes, or note data formats.

## Acceptance Criteria

- [ ] The Code Reading Mark Note Pro Tool Window stripe shows the yellow note icon rather than the IDE fallback icon.
- [ ] Add to Topic and Navigate to Note each show the yellow note icon in the editor context menu rather than sharing fallback chevrons.
- [ ] On macOS in English and Chinese, Manage Custom Commands initially shows the complete Add, Save, Delete, and Run labels in one row without clipping.
- [ ] The four-button row remains visible when the dialog is resized and under high-DPI scaling, without regressing the existing Windows layout.
- [x] Run continues to delegate enabled selections to the existing IDEA Terminal or Silent Terminal execution path; no selection or a disabled command remains a no-op. (source/package inspection)
- [x] AI Workspace does not add a separate Run toolbar action. (source inspection)
- [x] `gradlew test buildPlugin` passes; the packaged plugin is version 3.7.6 and contains three direct `/icons/codeReadingMarkNote.svg` descriptor references, the dedicated runtime SVG, and no runtime reference to `/META-INF/pluginIcon.svg` or `MyIcons.PLUGIN`.
