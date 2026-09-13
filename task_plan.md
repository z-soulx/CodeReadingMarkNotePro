# Workspace notes and issue #15 — release 3.7.7
## Goal
Implement the supplied multi-project workspace plan with independent compatible storage and complete local operations.
## Phases
- [complete] 1. Spec and call-site review
- [complete] 2. Ownership, discovery, safe XML storage, lifecycle coordination
- [complete] 3. Project tree, local operations, search, navigation, gutter, sync isolation
- [complete] 4. Automated tests and Gradle test/build
- [complete] 5. Version 3.8.0, bilingual UI, Chinese docs/help
## Next Step
Deliver the verified 3.7.7 ZIP and report pending installed-plugin and live-repository acceptance.
## Issue #15 and version 3.7.7
- [complete] Read issue and reproduce the reported behavior with a local HTTP server
- [complete] Implement fix and align release metadata/docs to 3.7.7
- [complete] Run focused tests and test/build; deliver verified ZIP
## Implementation 3.9.0
- [complete] Binding/state, complete snapshots, conditional storage and remote writes
- [complete] Per-project engine, manual UI, automatic lifecycle scheduling
- [complete] Focused tests, test/build, bilingual docs and release artifacts
## Follow-up: workspace project sync design (2026-09-13)
- [complete] Inspect existing sync identity, routing, conflict and scheduling contracts.
- [complete] Write a separate spec, implementation plan and acceptance tasks; update indexes.
- [complete] Review design consistency and prepare recommended interaction and automation explanation.
## Errors Encountered
- Public GitHub read via Invoke-RestMethod failed during TLS receive; curl Schannel in sandbox failed with SEC_E_NO_CREDENTIALS. Retrying authorized read with normal network permissions.
- ZIP verification initially expected an uninstrumented JAR filename; inspected entries and verified the actual instrumented plugin JAR.
- Save verification caught empty JDOM Text vs self-closing element digest mismatch; canonicalization now ignores empty text. Storage pretty formatting now explicitly preserves note whitespace.
- Coordinator test fixture lacked ModalityState support; marked its synchronous model execution as EDT. Production compile passed; fix is confined to the fixture.
- PowerShell rg wildcard path was not expanded; use directory plus -g '*.properties'.
- Sync test compilation: importer FormatException is a nested class; qualified the catch type.
- apply_patch delete/add same path unsupported; use update.
- Python is only a Windows Store alias; use PowerShell and apply_patch.
