# Specs - Change Units

A spec unit is one pending change to the system: a feature, a bugfix, a refactor. It has a
lifecycle and is **frozen when done** - never edited afterwards, never deleted. This
replaces the old `tmpmd/` per-feature notes.

Describing something that already exists is NOT a spec - that belongs in `docs/`
(see `.ai/context/INDEX.md` routing rules).

## Creating a Unit

Directory naming: `<yyyymm>-<slug>/`, e.g. `202609-group-import-export/`. Copy
`_template/` as a starting point:

```
.ai/specs/202609-<slug>/
├── spec.md    # WHY + WHAT: intent, background, acceptance criteria
├── plan.md    # HOW: design, interface changes, impact scope
└── tasks.md   # Breakdown with DoD, including the docs-backflow task
```

Execution evidence (commands, outputs, summaries) goes to `.ai/runs/` (gitignored) -
`runs/<unit-name>-<n>.md`, pure record, no reflection.

## Lifecycle

```
docs/ (现状) -> spec.md -> plan.md + tasks.md -> runs/ (证据) -> 验收 -> 冻结 -> docs/ (回流更新)
```

1. **Draft**: write `spec.md` first. No acceptance criteria = not ready to build.
2. **Plan**: design in `plan.md`; list affected code paths and risks.
3. **Execute**: work through `tasks.md`; record evidence in `.ai/runs/`.
4. **Verify**: every acceptance criterion in `spec.md` is checked and the check is honest.
5. **Freeze + backflow**: mark tasks done, then update `docs/` to the new steady state
   (this is a mandatory task in `tasks.md`, not an optional nicety). Remove the unit from
   the working set in `.ai/context/INDEX.md`.

## After Freezing

- Change-note analysis at release time reads the units frozen since the last release
  (see `docs/runbooks/release.md`).
- Later corrections to a frozen unit's *content* go into a new unit or into `docs/` -
  frozen files only ever get their status line updated.
