# Progress
## 2026-09-15 - release 3.7.8
- Loaded repository constraints and restored the completed linked-project/data-eligibility work. Started release metadata and publication-prerequisite audit from version 3.7.7.
- Updated build, plugin descriptor, and bilingual change notes to 3.7.8. Marketplace token is configured; signing material is absent, so the repository's supported unsigned path was used.
- JDK 17 `test build` passed with 71 tests and no failures/errors/skips. Packaged descriptor is 3.7.8; ZIP is 786096 bytes with SHA-256 `8007E18D5BF4269517EA3912295AB60C6AC66E8B16CB1521B0A451A45DA6FBA2`.
- `publishPlugin --dry-run` and the actual `publishPlugin` task both completed successfully. Plugin verification retained the existing optional Terminal dependency warning; macOS installed-plugin acceptance remains unexecuted.
## 2026-09-15 - notes-data project eligibility
- Restored the linked-project plan and repository constraints. New requirement narrows child discovery to roots with an existing `.idea/CodeReadingNote.xml`; root-project behavior remains unchanged.
- Amended the active spec, design, and tasks so child eligibility is explicitly based on the persisted plugin notes file rather than `.idea` metadata alone.
- Updated discovery, regular/link test fixtures, notes/sync/platform docs, system overview, and bilingual help. Focused verification is next.
- Focused `WorkspaceDiscoveryTest` passed 7/7 with the notes-file eligibility predicate. Full JDK 17 test/build is next.
- Full `test build` passed 71/71. Final audit added explicit create/delete-and-rescan assertions for the eligibility transition; one final full rerun remains.
- Final rerun passed: focused discovery 7/7 and full suite 71/71, zero failures/errors/skips; build succeeded and the 3.7.7 ZIP hash is recorded in run evidence.
## 2026-09-15 - macOS linked workspace projects
- Loaded repository red lines, context routing, system overview, and restored the existing workspace-sync planning context. Started focused investigation; no production changes yet.
- Routed through sync, auto-sync, GitHub, and IntelliJ integration documentation. Source search found an explicit no-follow policy in `WorkspaceDiscovery`, matching the macOS symptom.
- Reviewed discovery, service, context, path resolution, and focused tests. The fix needs linked-root discovery plus real-path-aware identity/ownership; implementation design is in progress.
- Amended the active verification spec, plan, and tasks with the linked-project contract before production edits.
- Implemented linked-project-root discovery without arbitrary link traversal, real-path `NoteProjectContext`/coordinator identity, alias-aware display, canonical file ownership, physical-target watching, and focused symlink/junction regression coverage. Verification started.
- Static diff check found only a pre-existing `.gitignore` blank-line warning plus line-ending notices. First focused test attempts were blocked by external Gradle-cache permissions and inherited Java 7; JDK 17 rerun pending.
- JDK 17 compilation reached source and failed only on the new missing `InvalidPathException` import; import corrected and focused rerun started.
- Focused `WorkspaceDiscoveryTest` passed 7/7, including duplicate aliases and canonical file ownership. Backflow docs now describe linked-root discovery, alias display, physical identity, and VFS watching.
- Updated the bilingual end-user workspace guide; the older frozen multi-project spec remains unchanged as historical scope evidence.
- First full run executed 71 tests with one failure in alias file ownership. Root cause was premature lexical fallback; fixed the routing precedence and queued a rerun.
- Corrected focused suite passed 7/7. Final JDK 17 `test build` passed: 71 tests, zero failures/errors/skips; 3.7.7 ZIP rebuilt. macOS installed-plugin and real GitHub acceptance remain unexecuted.
- Post-build audit found alias-path note creation could still be classified as external by lexical VFS ancestry. Replaced it with real-path-aware `NotePaths.relative` and extended the linked-project regression; final verification rerun required.
- Final rerun after relative-path correction passed `test build` again with 71/71 tests; evidence and package hash audit follow.
- Final artifact: `CodeReadingMarkNotePro-3.7.7.zip`, 785503 bytes, SHA-256 `22A9B6DD95905C0135B814E92A4D12C6EC2D61B7F86B347FBD8E1106D5492B94`; Gradle/plugin descriptor versions both 3.7.7.
## 2026-09-13 — publishing prerequisites
- Confirmed JAVA_HOME points at Java 7; JDK 17 exists. Fixed unconditional explicit and implicit ZIP Signer dependencies for unsigned publishing in build.gradle; partial signing materials now fail clearly.
- With JDK 17, unsigned publishPlugin --dry-run excludes downloadZipSigner; signed dry-run with quoted placeholder paths includes it. signPlugin verifyPlugin passed with signing skipped and no signer download. Partial configuration was rejected as expected. Existing optional Terminal config-file and Gradle deprecation warnings remain. Updated release guide; actual Marketplace upload and certificate signing were not executed.
## 2026-09-13 — issue #15 / 3.7.7
- Final test/build passed: 70 tests, zero failures/errors/skips. Verified packaged descriptor version 3.7.7 and issue-fix/workspace classes. ZIP: 783369 bytes; SHA256 D7155138D74C5A44269AC6029C31D49336EFEAC702E5C6BFF3995F787F7CA946. Current EN/ZH sync keys match (65); final evidence in .ai/runs/202609-issue-15-existing-remote-push-1.md. Live reporter repository, Windows/macOS installed-plugin and real multi-window acceptance remain unexecuted; no publication or remote write performed.
- Read public issue and attached screenshot; no comments present. Windows sandbox TLS initialization failed, authorized elevated curl read succeeded. User requires release version 3.7.7 including existing workspace work.
- Fixed legacy branch-scoped SHA/content/checksum reads, fail-closed SHA errors, structured JSON error decoding/redaction, and distinct 422 states in the workspace transport. Local HTTP regression confirms consecutive updates without DELETE. Initial test run passed; current version/docs consolidated to 3.7.7, final build pending.
## 2026-09-13 — workspace sync implementation
- User authorized implementation of the design. Re-read constraints, working set and source; preserving existing 3.8.0 workspace changes. No remote operations or publishing authorized by implementation work.
- Added per-project binding/state XML, content baseline classification, SHA-conditional GitHub notes transport, shared sync coordinator, manual target actions, overview/settings/conflict UI, automatic polling and legacy scheduler delegation. Initial compile passed. Added protocol, storage and baseline tests; test execution in progress.
- Final revision includes explicit preservation of unknown local XML extensions, verified save barriers, binding generation checks, shared rate-limit wait, fair per-project automatic scheduling and runtime language notifications. Updated Chinese domain/scenario docs, bilingual help and 3.9.0 release metadata.
- Final gradlew test build succeeded: 63 tests, zero failures/errors/skipped. 62 new notes.sync keys match EN/ZH without duplicates; diff whitespace check passed. ZIP version/classes verified inside instrumented JAR. Artifact: build/distributions/CodeReadingMarkNotePro-3.9.0.zip (779995 bytes), SHA256 BC9AA7D7CEDC63B4FCA3390BEC1767B48F6B29AF60DC6BE6009F78794676088C.
- Not run: installed-plugin UI / real GitHub transfer / real two-window root persistence acceptance, macOS installation or Plugin Verifier version matrix. No commit, remote push/pull, publishing or live IDE installation performed.
## 2026-09-13 — workspace sync design
- Restored previous implementation state and planning files; loaded repository constraints and sync documentation. Design only; existing 3.8.0 changes remain intact.
- Verified source contracts; created 202609-workspace-project-notes-sync spec/plan/tasks and updated INDEX/docs routing. Design covers explicit targeting, stable bindings, safe bidirectional polling, baseline/SHA races, complete payloads, persistence and multi-window lifecycle.
- Reviewed acceptance/design consistency and file existence; git diff --check passed (existing LF/CRLF conversion warnings only). No production code or version changes, no Gradle tests/build or remote sync executed for this documentation-only follow-up.
## 2026-09-13
- Read repository constraints and routed notes/sync/platform/release docs.
- Initial git tree clean. Inspected service, models, importer and UI call sites.
- Implemented runtime ownership, coordinator, discovery/storage, project tree and action routing. First compilation found Disposable package imports; fixed. Gradle requires cache write escalation. PowerShell brace expansion unsupported; use explicit paths.
- Expanded suite: 38/39 passed. The cross-project rejection test needs a language service application fixture; added an isolated application proxy. Reviewed reload/edit race and added revision checks, root sync mutation gating and durable recovery outside deleted project directories.
- All 39 tests and Gradle build passed. Added 3.8.0 help/change notes/domain documentation. Final audit separates scanning from the serial writer and addresses close/reload conflicts; final rebuild still pending.
- Final revision: 42/42 tests passed, Gradle test/build succeeded, 16 workspace translation keys verified, diff whitespace check clean. 3.8.0 ZIP generated. Spec remains in verification pending installed-plugin/manual checks; evidence in .ai/runs/202609-multi-project-notes-workspace-1.md.
