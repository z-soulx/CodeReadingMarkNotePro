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
Provide corrected JDK 17 publication command; prerequisite verification is complete.
## Publish prerequisites
- [complete] Correct optional signer dependency and document JDK 17 invocation
- [complete] Verify unsigned/signed task graphs and unsigned build; record evidence
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
## Follow-up: macOS linked workspace projects (2026-09-15)
- [complete] Reproduce and identify linked-project discovery or identity failure.
- [complete] Amend the active spec and implement a compatibility-safe fix.
- [complete] Add focused regression tests and run the relevant Gradle verification.
## Follow-up: notes-data project eligibility (2026-09-15)
- [complete] Amend discovery contract so only child roots with `.idea/CodeReadingNote.xml` are eligible.
- [complete] Update regular and linked discovery implementation/tests plus current-system documentation.
- [complete] Run focused and full Gradle verification and refresh evidence.
## Release 3.7.8 (2026-09-15)
- [complete] Audit release metadata, change notes, signing, and Marketplace prerequisites.
- [complete] Update all required version/release documentation to 3.7.8.
- [complete] Run full test/build and verify the packaged artifact.
- [complete] Publish with the configured Marketplace token and record the exact outcome.
## Errors Encountered
- Signer dependency also propagated through cliPath convention; removed that provider when unsigned after initial dry-run still included downloadZipSigner.
- PowerShell split unquoted -P file arguments at .pem; quoted complete arguments and signed dry-run passed. Partial signing configuration intentionally fails validation.
- Public GitHub read via Invoke-RestMethod failed during TLS receive; curl Schannel in sandbox failed with SEC_E_NO_CREDENTIALS. Retrying authorized read with normal network permissions.
- ZIP verification initially expected an uninstrumented JAR filename; inspected entries and verified the actual instrumented plugin JAR.
- Save verification caught empty JDOM Text vs self-closing element digest mismatch; canonicalization now ignores empty text. Storage pretty formatting now explicitly preserves note whitespace.
- Coordinator test fixture lacked ModalityState support; marked its synchronous model execution as EDT. Production compile passed; fix is confined to the fixture.
- PowerShell rg wildcard path was not expanded; use directory plus -g '*.properties'.
- Sync test compilation: importer FormatException is a nested class; qualified the catch type.
- apply_patch delete/add same path unsupported; use update.
- Python is only a Windows Store alias; use PowerShell and apply_patch.
- Focused Gradle test could not lock the configured external Gradle cache under sandbox; reran with approved access.
- Approved Gradle run inherited Java 7, which Gradle 8.5 rejects; rerun with the installed JDK 17 selected explicitly.
- First JDK 17 compile found a missing `InvalidPathException` import in `WorkspaceNotesService`; added the explicit import before rerunning.
- Combined documentation patch missed the exact IntelliJ integration text and was rejected atomically; reread the lines and applied a narrower patch.
- Full suite exposed an owner-routing regression: an alias file lexically matched the workspace fallback before canonical matching. The fast path now returns only a specific child root and canonicalizes fallback-only matches.
- Two combined verification-record patches missed exact existing text and were rejected atomically; reread the target fragments and applied smaller patches.

## Next Step (2026-09-15)
Monitor JetBrains Marketplace processing for the uploaded 3.7.8 release; macOS installed-plugin acceptance remains separate manual verification.
