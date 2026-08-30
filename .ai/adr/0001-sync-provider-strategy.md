# ADR-0001: Sync backend behind a provider strategy

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

Notes and AI configs sync to a remote host for cross-device backup. GitHub is the only
supported host today, but hard-coding GitHub API calls into sync flows would make a second
backend (GitLab, Gitee, S3...) a rewrite rather than an addition.

## Decision

Remote sync goes through a strategy + factory: `SyncProvider` (interface) ->
`AbstractSyncProvider` (shared plumbing) -> `GitHubSyncProvider` (API specifics), selected
by `SyncProviderFactory` from `SyncProviderType` in `SyncConfig`.

## Consequences

- Adding a backend = one new subclass + enum entry; sync flows stay untouched
- Provider-specific quirks (error formats, path encoding) are contained in one place:
  `sync/github/`
- Everything truly shared (config validation, reporting) belongs in `AbstractSyncProvider`;
  letting provider details leak upward is the failure mode to watch
