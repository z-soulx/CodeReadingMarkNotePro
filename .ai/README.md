# .ai/ - Product & Architecture Docs

Permanent reference documents for the Code Reading Mark Note Pro plugin. AI assistants load these on demand from `.claude/CLAUDE.md`.

## Files

| File | Content |
|------|---------|
| `ARCHITECTURE.md` | Domain model, components, data storage, sync design, PlantUML class diagram |
| `WORKFLOW.md` | Version release procedure, post-feature review process |
| `README_SYNC.md` | Sync feature spec: GitHub sync, cross-device, conflict detection |

## Naming Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature spec | `README_<FEATURE>.md` | `README_SYNC.md` |
| PRD | `PRD_<feature>.md` | `PRD_export_import.md` |
| Design doc | `DESIGN_<topic>.md` | `DESIGN_conflict_resolution.md` |

## vs `tmpmd/`

- **`.ai/`**: Permanent specs and architecture -- survives across releases
- **`tmpmd/`**: Temporary dev notes created per feature/bugfix, cleaned after completion
