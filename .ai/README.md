# .ai/ - Project Spec Space

Development specs and architecture for Code Reading Mark Note Pro. AI assistants load on demand via `.claude/CLAUDE.md`.

## Files

| File | Content |
|------|---------|
| `ARCHITECTURE.md` | Domain model, components, data storage, sync design, AI config layer |
| `WORKFLOW.md` | Version release procedure, post-feature review process |

## Naming Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature spec / PRD | `PRD_<feature>.md` | `PRD_export_import.md` |
| Design doc | `DESIGN_<topic>.md` | `DESIGN_conflict_resolution.md` |

## vs other directories

- **`.ai/`**: Permanent architecture and specs — survives across releases
- **`tmpmd/`**: Temporary dev notes per feature/bugfix, cleaned after completion
- **`help/`**: User-facing plugin documentation (sync guide, AI workspace guide)
