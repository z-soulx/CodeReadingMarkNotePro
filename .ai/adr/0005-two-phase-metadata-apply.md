# ADR-0005: Two-phase metadata apply on pull (pre-rescan / post-rescan)

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

Workspace metadata (`ai-config-registry.json`) mixes two kinds of fields: discovery inputs
(`customPaths`, `ignorePatterns` change *which files are found*) and per-file results
(`trackedEntries`, `fileHashes`, `trackedEmptyDirs` describe *entries the scan produced*).
Applying everything in one pass is order-dependent: result fields applied before a rescan
reference entries that do not exist yet.

## Decision

Pull applies metadata in two phases around a rescan:
1. **Pre-rescan**: `customPaths` + `ignorePatterns` (they parameterize discovery)
2. **Post-rescan**: `trackedEntries` + `fileHashes` + `trackedEmptyDirs` (applied to the
   entries the new scan just created)

## Consequences

- The phase split is a hard contract: moving a field across the rescan boundary breaks
  pull correctness (ignored patterns not honored, or tracked flags dropped)
- New metadata fields must be classified into a phase when introduced
