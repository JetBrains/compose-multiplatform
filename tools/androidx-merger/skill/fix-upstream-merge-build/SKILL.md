---
name: fix-upstream-merge-build
description: Fix a failing fork build after merging one upstream commit, including solving merge conflict and adapting non-Android targets.
---

# Fix build

## Project description
This is an AOSP fork of the Androidx repo, in which Compose Multiplatform (a fork of Jetpack Compose) and related libraries are developed.

During merges, there is:
- common code
  - commonMain, commonTest

- fork-only code - used for developing Compose Multiplatform with related libraries and targets
  - independent build infrustucture "settings-fork.gradle", "buildSrc-fork", "build-fork.gradle"
  - iOS, desktop, js, jsWasm sourceSets and their combinations, and test versions
  - AI-related: .junie, .claude, .codex

- AOSP only code - used only for developing AOSP-only libraries and targets
  - independent build infrustucture "settings.gradle", "buildSrc", "build.gradle"
  - androidMain, and their test versions
  - iOS, desktop, js, jsWasm sourceSets for libraries and targets that are developed in AOSP, and just used as a published artifact in the fork
  - AI-related: AGENTS.md, .agents

## Find commits
- Find the previous `mergeCommit`. This is the most probable commit that caused failure, because the merging script stopped there
- Find the first commit on the right side that have only one parent. This is the CL (Changelist, PR, MR) branch `clCommit` that was merged to the AOSP branch. Usually it contains a single commit, but can contain more
- Use this branch to understand the context of the upstream changes

## Rules
- Don't change AOSP-only code
- Don't change common-only code

- Exceptions of changing common-only code:
  - a fork target cannot work without it
  - important fork target functionality was removed

- Rules if changing common-only code if necessary:
  - Make any change minimal
  - Make it easy to solve future merge conflicts
- Check with `./gradlew assemble compileTest`
- If a new API is needed and we depend on an artifact instead of a project - we need to depend on the latest published version - find it in `libraryversions.toml` and update `redirectversions.toml` if necessary.
- Apply only relevant `build.gradle` changes to `build-fork.gradle`
- Add only the code needed to compile and not crashing in runtime
- Avoid comments unless they are absolutely needed
- Don't implement new features. Choose what is more acceptable until it is implemented in this priority:
  - no-op
  - printing a warning in format "Public class or function name: $warning"
  - exception
- For the new features that need to be implemented, add TODO:
  - add `// TODO(Merge) <Priority>, Implement after merging <clCommit>`.
  - after this TODO add short description of what is needed to be implemented and what happens if it is not implemented
  - <Priority> can be Critical, Major, Normal, Minor

- Create only one commit
- If necessary to amend the commit, only amending commit you created is allowed
- In the title describe shortly what you changed

## Fixing steps
- If the `mergeCommit` contains `# Conflicts`, fix all conflicts
- Use `androidMain` changes to determine what other targets need.
- Run the build, fix until green
- Commit the fix with a message starting `(AI) Fix `
