# ADR-0002: Notes and AI configs sync on independent channels

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

The plugin syncs two very different payloads: a single notes file (`CodeReadingNote.xml`)
owned entirely by the plugin, and a set of user AI config files that also exist on disk and
belong to other tools. Sharing one sync pipeline would entangle their timing, error
handling, and reporting.

## Decision

Two independent sync channels:
- **Notes**: `SyncService` -> `push()`/`pull()` -> `CodeReadingNote.xml`
- **AI Configs**: `AIConfigSyncAdapter` -> `pushFiles()`/`pullFiles()` -> `ai-configs/`
  directory + manifest

Each has its own auto-sync scheduler (`AutoSyncScheduler` 3s debounce; `AIConfigAutoSyncScheduler`
5s debounce) and its own conflict handling.

## Consequences

- A failure or pause on one channel never blocks the other
- Two code paths to keep consistent (hash bookkeeping, pause-on-conflict, status reporting)
- Shared provider API (`pushFiles`/`pullFiles` vs `push`/`pull`) must stay clearly separated
