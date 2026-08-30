# ADR-0003: Incremental MD5 push with retry-on-next-push failure semantics

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

AI config workspaces can hold dozens of files; re-uploading all of them on every push is
slow and burns GitHub API quota. But skipping files based on a naive "already pushed" flag
would permanently lose files that failed once.

## Decision

Push is incremental and hash-based:
1. Combined hash (tracked paths + content + tracked empty dirs) short-circuits no-op pushes
2. Per-file MD5 vs `lastPushedFileHashes` decides which files upload
3. **Failed files are excluded from hash recording** - they are re-attempted on the next push
4. Manifest diff (old vs new) issues DELETE calls for files no longer tracked
5. Force push ignores MD5 and re-uploads everything (escape hatch via `PushReportDialog`)

## Consequence

- Correctness invariant: a file's hash is recorded only after a successful upload. Breaking
  this (recording hashes before/despite failure) silently stops retrying failed files
- Empty-dir state and manifest diffs make "delete" a first-class push concern, not an
  afterthought
