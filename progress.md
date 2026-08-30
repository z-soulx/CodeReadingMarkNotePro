# Progress

## 2026-08-30

- User asked to ship Cursor + Typora as first-run built-in commands that users can delete.
- Seed rule: write only when `.ai/workspace-commands.json` does not exist; never re-inject into an existing file.
- Implemented `ensureSeeded()` on load, startup, skeleton create, and Git init. Display names from bundles.
- `JAVA_HOME=C:\Program Files\Java\jdk-17 .\gradlew.bat test` passed. Unit `202608-ai-builtin-commands` frozen.
