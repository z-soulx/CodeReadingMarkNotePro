# ADR-0006: Remote workspace metadata is JSON, not IDE XML

- Status: accepted
- Date: 2026-08-29 (recorded at docs restructure; decision predates it)

## Context

Local plugin state uses IntelliJ `PersistentStateComponent` XML, but the remote AI config
manifest (`ai-config-registry.json`) is read by other machines and potentially other
tools. IDE XML schemas are Java-coupled and awkward for cross-platform consumers.

## Decision

Remote workspace metadata is serialized as JSON (`AIConfigSyncAdapter` serializes/pushes
`ai-config-registry.json` alongside files; pulls parse and apply it), while local state
stays in `PersistentStateComponent` XML.

## Consequences

- Two serialization formats exist by design; do not "unify" them casually
- The JSON schema is a cross-version contract: add fields with defaults, never rename or
  repurpose (see constitution "Data & Compatibility")
- Local XML remains free to evolve independently
