Perform the skill below and push.

(Temporary) If `./gradlew assemble compileTests` fails with a network failure, retry twice.

---
name: fix-upstream-merge-build
description: Fix a failing fork build after merging one upstream commit, including solving merge conflicts and adapting non-Android targets.
---

# Fix build

## Project description

This is an AOSP fork of the Androidx repo, in which Compose Multiplatform, a fork of Jetpack Compose, and related libraries are developed.

During merges, there is:

- Common code:
  - `commonMain`, `commonTest`
- Fork-only code used for developing Compose Multiplatform with related libraries and targets:
  - Independent build infrastructure: `settings-fork.gradle`, `buildSrc-fork`, `build-fork.gradle`
  - iOS, desktop, JS, WasmJS source sets, their combinations, and test versions
  - AI-related: `.junie`, `.claude`, `.codex`
- AOSP-only code used only for developing AOSP-only libraries and targets:
  - Independent build infrastructure: `settings.gradle`, `buildSrc`, `build.gradle`
  - `androidMain` and its test versions
  - iOS, desktop, JS, and WasmJS source sets for libraries and targets developed in AOSP and used only as published artifacts in the fork
  - AI-related: `AGENTS.md`, `.agents`

## Find commits

- Air Automation clones the repository shallowly. Fetch the full Git history with `git fetch --unshallow` before finding commits.
- Find `mergeCommit`: the commit with a `Merge script:` note and without a `Merge solver:` note. This is the commit sent by the merging script and the commit to which the review note must be attached. Stop if it is not a merge commit.
- Find the first commit on the right side that has only one parent. This is the CL branch `clCommit` that was merged into the AOSP branch. Usually it contains a single commit, but it can contain more.
- Use this branch to understand the context of the upstream changes.

## General rules

- Don't change AOSP-only code except as described in `Cases`.
- Don't change common-only code except as described in `Cases`.
- Check with `./gradlew assemble compileTests`.
- Apply only relevant `build.gradle` changes to `build-fork.gradle`.
- Add only the code needed to compile without crashing at runtime.
- Avoid comments unless they are absolutely needed.
- Create at most one commit. If no changes are needed, don't create a commit.
- If necessary, amend only the commit you created.
- In the title, briefly describe what you changed.

## Cases

### Conflict

- Solve every conflict, even if the build is successful.

#### Conflict in common code

- This usually happens because the fork has critical changes that are not upstreamed to AOSP.
- Understand what these changes do and reapply them to the new state.
- They can cover multiple files, not only the file with the conflict. Use blame to understand which files are involved.

#### Conflict in AOSP-only code (`androidMain`, `androidDeviceTest`, etc.)

- Understand whether the cause is:
  - A legitimate fork change, which is rare.
  - A non-legitimate fork change, such as a cherry-pick from a changed release-branch version.
- If it is not legitimate, reset it to the AOSP state.
- If it is legitimate, decide whether to:
  - Merge it.
  - Reset it to the fork state.
  - Add it to `.gitattributes` if it is a fork-related file that should always use the fork state.

### New feature

- Don't implement new features. Choose the most acceptable temporary behavior in this order:
  - No-op.
  - Print a warning in the format `Public class or function name: $warning`.
  - Throw an exception.
- For features that still need implementation, add:
  - `// TODO(Merge) <Priority>, Implement after merging <clCommit>`
  - Immediately after the TODO, briefly describe what must be implemented and what happens if it is not implemented.
  - `<Priority>` can be `Critical`, `Major`, `Normal`, or `Minor`.

### New API in an AOSP-only module

- This happens when a module with fork-only targets depends on a module published entirely from AOSP.
- Depend on the latest published version. Find it in `libraryversions.toml` and update `redirectversions.toml` if necessary.

### Breakage in common code that is critical for the fork

- This happens when:
  - A target does not support compilation of new common code.
  - An important common or fork-only feature was broken.
- Make every change minimal.
- Make future merge conflicts easy to solve.

## Fixing steps

- If `mergeCommit` has a `Merge script: conflict` note, fix all conflicts.
- Use `androidMain` changes to determine what other targets need.
- Run the build and fix it until green.
- When `./gradlew assemble compileTests` fails, inspect only the last 300 lines of `log.txt` with `tail -n 300 log.txt`. Increase the limit only if the failure summary or relevant stack trace is missing.
- If changes are needed, create one commit with a message starting `(AI) Fix `.

## Report review

- Always add a Git note to the original `mergeCommit` after completing the assessment.
- If no changes were needed, use `Merge solver: no changes required`.
- If a fix was committed, use `Merge solver: fixed in <fixCommit>`.
- If the issue could not be fixed, use `Merge solver: unresolved - <reason>`.
- Push the branch first. Then push the note with `git push origin refs/notes/commits`.
