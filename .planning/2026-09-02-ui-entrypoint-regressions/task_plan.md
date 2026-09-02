# Fix UI Icons And macOS Command Dialog

## Goal

Ship version 3.7.6 with resolvable plugin icons for the Tool Window and two editor actions, and ensure the Manage Custom Commands dialog shows Add, Save, Delete, and Run without clipping on macOS.

## Phases

- [completed] Read repository constraints, routed context, current spec, and implementation
- [completed] Align the existing spec and implement the descriptor/layout/release changes
- [completed] Run tests, build the plugin, and inspect the packaged artifact
- [completed] Record verification evidence and close with explicit manual-platform verification gap
- [completed] Correct the icon implementation after installed-plugin acceptance failed
- [in_progress] Rebuild, inspect, and obtain installed-plugin acceptance before freezing the spec

## Next Step

Close the currently running default 2024.3 sandbox, rerun `gradlew runIde` so it receives the dedicated runtime icon package, and perform visual acceptance.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Keep the existing root planning files unchanged | They document a completed 3.7.5 task. |
| Use the supplied implementation plan as intent | The user explicitly asked to implement that plan in fresh context. |
| Preserve the existing AI Workspace toolbar | The confirmed requirement places Run only in Manage Custom Commands. |
| Use layout-derived preferred width with a scaled 900x500 floor | It preserves the existing Windows baseline while accommodating platform-specific button sizes. |
| Do not freeze before platform installation checks | The constitution defines completion as verified, and this Windows-only non-GUI environment cannot honestly execute the macOS/manual interaction criteria. |
| Treat the user's installed-plugin screenshots as authoritative | They show the fully qualified field reference resolves to IDE fallback icons, invalidating package-presence-only verification. |
| Use direct `/META-INF/pluginIcon.svg` descriptor paths | Both 2024.3 and 2025.3 resolvers return the intended yellow 16x16 icon; direct resource loading removes reflective classloader dependence. |
| Replace `/META-INF/pluginIcon.svg` with a dedicated `/icons/codeReadingMarkNote.svg` runtime resource | A fresh 2024.3 `runIde` loaded the direct-META-INF 3.7.6 JAR but still rendered placeholder chevrons, proving the standalone resolver check was insufficient. |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| Combined patch could not match the mojibake-rendered `INDEX.md` working-set line | 1 | Split the edit and match stable headings/context instead of terminal-rendered Unicode. |
| Static `rg` assertion had a PowerShell quoting/regex parse error | 1 | Re-run with fixed-string searches and XML parsing. |
| Gradle wrapper could not lock the configured cache outside the workspace sandbox | 1 | Re-ran with approval for `E:\\work\\PG\\Gradlerepo` access. |
| Approved Gradle run picked up Java 1.7, but Gradle 8.5 requires Java 8+ | 1 | Verify the known JDK 17 path and set task-local `JAVA_HOME`/`PATH`. |
| Artifact inspection assumed the inner JAR filename included `-3.7.6` | 1 | List outer ZIP entries and inspect the actual JAR path. |
| `icons.MyIcons.PLUGIN` passed package inspection but rendered IDE fallback icons after installation | 1 | Reopen the icon design; verify actual platform resolution and use a direct packaged resource path if supported. |
| Repository-wide icon regex was parsed as a PowerShell array expression | 1 | Use single-quoted regex or fixed-string searches. |
| Two official JetBrains documentation requests failed during TLS receive | 1 | Inspect the locally cached IntelliJ 2024.3 implementation instead of repeating the same network request. |
| Historical artifact comparison found the previously listed ZIP files absent | 1 | Use Git history for old inputs and rebuild the new candidate after the resolver test. |
| JShell first failed on sandboxed Java Preferences, then its Windows PTY did not submit pasted code reliably | 1 | Exit JShell and compile a temporary standalone resolver probe under the task planning directory. |
| Standalone resolver probe could load all candidates but could not query dimensions before JBUIScale initialization | 1 | Precompute a 1.0 diagnostic system scale in the probe, then rerun rasterization. |
| Screenshot-based 40x40 clipping hypothesis was not reproduced | 1 | Resolver produced the correct 16x16 yellow icon; inspect the exact installed artifact and IDE runtime log before changing resources. |
| User screenshots were taken from a process still running installed plugin 3.7.1 | 1 | Check for a staged update, validate against the actual 2025.3 runtime, rebuild 3.7.6, and require IDE restart/install before visual acceptance. |
| Installed-JAR icon listing used `-join` at the wrong PowerShell pipeline precedence | 1 | Parenthesize the filtered result before joining. |
| `Get-ChildItem -Filter` was passed an unsupported array | 1 | Enumerate the lib directory, then filter filenames with `Where-Object`. |
| JDK 17 probe compiler rejected IDEA 2025.3 classes compiled for Java 21 (class version 65) | 1 | Use IDEA 2025.3's bundled JBR 21 only for the runtime diagnostic; keep project builds on Java 17. |
| Combined direct-path patch had incorrect planning-file context | 1 | Confirmed no partial edit, then split product/spec and planning updates. |
| Rebuild failed in `prepareSandbox` because the instrumented 3.7.6 JAR is open by another process | 1 | Identify the sandbox process and use a non-disruptive build path; do not terminate user processes without authorization. |
| `Get-CimInstance Win32_Process` was denied while reading command lines | 1 | Use the JDK `jps -lv` diagnostic instead. |
| A cached-source search passed a wildcard path literally to `rg` on Windows | 1 | Avoid wildcard roots and use PowerShell enumeration or test the init script directly. |
| The first inner-descriptor check addressed the XML root as `idea_plugin` | 1 | Re-ran with XPath against `/idea-plugin`; package checks then returned version 3.7.6 and three direct icon paths. |
| A fresh direct-META-INF 3.7.6 JAR still rendered placeholder chevrons in real `runIde` | 1 | Separate Marketplace metadata from runtime UI icons and use a plugin-unique `/icons/codeReadingMarkNote.svg` resource. |
