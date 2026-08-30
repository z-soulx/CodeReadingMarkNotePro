# ADR-0004: Empty directories represented as `.gitkeep` on remote

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

Git-style remotes cannot store empty directories, but the AI config workspace must track
them: an empty dir is a meaningful user choice (e.g. a scaffolded but unfilled config
folder) that should survive cross-device sync.

## Decision

- `AIConfigRegistry.scan()` records all discovered dirs (including empty) in `discoveredDirs`
- Users explicitly check empty dir checkboxes in `AIConfigTreePanel` to track them
- Tracked empty dirs persist in `AIConfigService.PersistentState.trackedEmptyDirs` and sync
  as `.gitkeep` placeholder files on remote

## Consequences

- Empty-dir tracking is opt-in and explicit, never inferred from `.gitkeep` presence alone
- Tracked state is double-bookkept: local `trackedEmptyDirs` + remote manifest, and pull
  must reconcile both (see ADR-0005's two-phase apply)
- A dir that becomes non-empty still carries the placeholder until state is re-synced
