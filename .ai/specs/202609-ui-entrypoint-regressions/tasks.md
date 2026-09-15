# Tasks: Restore UI entry points

- Unit: `202609-ui-entrypoint-regressions`

## DoD (Definition of Done)

All acceptance criteria in `spec.md` verified; evidence recorded in `.ai/runs/`;
docs backflow done; constitution constraints re-checked.

## Tasks

- [ ] T1: Repair Tool Window and editor action icon references and verify them in the actual 2024.3 sandbox
- [x] T2: Make the existing manager-dialog action row determine the required platform-aware width
- [x] T3: Bump to 3.7.6 and verify tests/package
- [x] Backflow: update help and domain documentation to the new steady state
- [ ] T4: Install on Windows and macOS; verify icons, English/Chinese command-dialog layout, both Terminal modes, resize, and high-DPI behavior
- [ ] Remove the unit from `.ai/context/INDEX.md` and freeze it after T4 passes
