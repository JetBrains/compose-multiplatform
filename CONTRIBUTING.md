# What this repository is

This repository contains sources of Compose Multiplatform supporting projects (Gradle plugin, Resources, samples, templates, etc.).

# What this repository is not

The main codebase for Compose Multiplatform is located in a [separate repository](https://github.com/JetBrains/compose-multiplatform-core),
which is a fork of [AOSP](https://github.com/androidx/androidx).
Contributions that affect Android or common code of the main codebase should be submitted in AndroidX repository.

# Contributing

We love contributions!
If you want to find some issues to start off with,
try [this query](https://youtrack.jetbrains.com/issues/CMP?q=tag:%20%7BUp%20For%20Grabs%7D%20%23Unresolved) which should find all open Compose Multiplatform issues that are marked as "up-for-grabs".

If you'd like to fix a bug or add a feature, [create a YouTrack issue](https://youtrack.jetbrains.com/newIssue?project=CMP) for it first.
You'll then be able to discuss the proposed change with maintainers.
It would help to avoid situations when the change conflicts with some other feature or help discover potential edge cases.

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

If you are working on the [compose-multiplatform-core](https://github.com/JetBrains/compose-multiplatform-core) repo, make sure to follow the [development guide](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/MULTIPLATFORM.md) for local set up.

## Submitting patches

The best way to submit a patch is to [fork the project on GitHub](https://help.github.com/articles/fork-a-repo/) and then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) to the `master` branch via [GitHub](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.

## Rules for commit messages

Most of these rules are originated from the [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/)
article, and it's highly recommended to read it.

### Rules on pull requests' description

1. Explain what and why vs. how
    * Please make an extra effort to explain why changes are needed for every non-trivial modification.
    * Describe how you tested your changes
    * Add Release Notes
    * Follow the [PR template](https://github.com/JetBrains/compose-multiplatform/blob/master/.github/PULL_REQUEST_TEMPLATE.md)
2. Mention relevant [YouTrack](https://youtrack.jetbrains.com/issues/CMP) issues in their messages
3. Commit changes together with the corresponding tests, unless the resulting commit becomes too unwieldy to grasp

### Rules on PR messages style/formatting

1. Capitalize the title
2. Do not end the title with a period
3. Use the imperative mood in the title

## Checklist

Before submitting the pull request, make sure that you can say "YES" to each point in this short checklist:

- You provided the link to the related issue(s) from YouTrack
- You made a reasonable amount of changes related only to the provided issues
- You can explain changes made in the pull request
- You ran the build locally and verified new functionality
- You ran related tests locally and they passed
- You do not have merge commits in the pull request
