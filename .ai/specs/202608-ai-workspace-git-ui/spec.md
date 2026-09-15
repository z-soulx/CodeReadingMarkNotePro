# Spec: .ai independent Git and IDEA Commit UI

- Unit: `202608-ai-workspace-git-ui`
- Status: frozen
- Type: feature

## Intent (WHY)

Users expect `.ai` to be an independent Git workspace, but IDEA's Commit tool window currently lists `.ai` files as unversioned files of the plugin/project repository. The product should isolate `.ai` from the parent Git root, register it as its own VCS mapping, and open IDEA's native Commit UI for that directory in one action.

## Background

3.7.4 already offers opt-in `.ai` Git via CLI (`git init` / `git add` / `git commit` with `.ai` as the working tree). The parent `.gitignore` only excludes `.ai/.git`, `runs/`, and `token.txt`, so the rest of `.ai` still appears in the parent Commit window. CLI init never calls IDEA VCS APIs, so Directory Mappings stay on the project root only. See `docs/domain/ai-config/README.md` (AI Workspace runtime section) and `help/AI_WORKSPACE_GUIDE.md`.

## Scope

In scope: parent-repo isolation via `/.ai/`, IDEA Directory Mapping for `.ai`, startup repair when `.ai/.git` already exists, one-click Open .ai Git using native Commit UI, i18n, help/docs, version 3.7.5.

Out of scope: git submodule, opening `.ai` in a second IDEA window, replacing CLI git with git4idea APIs, guaranteeing the Commit window hides unrelated parent-repo changes.

## Acceptance Criteria

- [x] Parent project `.gitignore` contains `/.ai/` after Initialize .ai Git (or Open .ai Git when Git was not yet initialized); files under `.ai/` no longer appear as parent-repo unversioned files.
- [x] After `.ai/.git` exists, IDEA Directory Mappings include a Git mapping for the `.ai` directory; reopening the project restores the mapping if it was missing.
- [x] AI Workspace toolbar Open .ai Git initializes Git when needed, then opens IDEA's native Commit UI scoped to `.ai` (`CheckinFiles`); if that action is unavailable, the Commit tool window is activated instead.
- [x] Existing Initialize .ai Git and Commit Workspace Version keep their current CLI behavior.
- [x] New UI strings exist in English and Chinese bundles; `build.gradle` and `plugin.xml` are `3.7.5`; change notes and help/docs describe the nested-repo Commit UI.
- [x] `./gradlew test` passes on Java 17.
