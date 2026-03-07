# Operational Workflows

## Version Release

1. **Bump version** in both files (must match):
   - `build.gradle` -> `version` field
   - `src/main/resources/META-INF/plugin.xml` -> `<version>` tag

2. **Update change notes** in `src/main/resources/META-INF/changeNotes.html`:
   ```html
   <h1>x.y.z</h1>
   <ul>
     <li><b>Feature:</b> English desc（<b>新功能：</b>中文描述）</li>
     <li><b>Bug Fix:</b> English desc（<b>Bug修复：</b>中文描述）</li>
   </ul>
   ```
   - Analyze `tmpmd/` docs; classify as Features / Improvements / Bug Fixes / i18n
   - Write from **product perspective**, not developer perspective
   - Deduplicate: same-batch fixes for one feature = single entry

3. **Optionally update** `description.html` and `README.md` for major features

## Post-Feature Review

After completing a feature and cleaning `tmpmd/`:

1. Check if `.claude/CLAUDE.md` needs updating (new hard rules, new key paths)
2. Check if `.ai/ARCHITECTURE.md` needs updating (new components, changed data flow)
3. Update criteria:
   - **Must update**: New core feature, architecture change, new tech pattern
   - **Skip**: Pure refactor, minor bugfix, internal optimization
