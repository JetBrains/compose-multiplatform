# What this repository is

This repository contains sources of Compose Multiplatform supporting projects (Gradle plugin, Resources, samples, templates, etc.) which are used in [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform).
Compose Multiplatform project provides support only for non-Android targets.

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

Make sure to follow the [development guide](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/MULTIPLATFORM.md) in the core repository for local set up.

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

### Rules on commit messages' content

1. Use the body to explain what and why vs. how
    * Please make an extra effort to explain why changes are needed for every non-trivial modification.
2. Significant commits must mention relevant [YouTrack](https://youtrack.jetbrains.com/issues/CMP) issues in their messages
3. Commit changes together with the corresponding tests, unless the resulting commit becomes too unwieldy to grasp
4. Keep the subject (first line of the commit message) clean and readable. All additional information and directives for external tools
   should be moved to the message body.

### Rules on commit messages' style/formatting

1. Separate subject from body with a blank line
2. Capitalize the subject line
3. Do not end the subject line with a period
4. Use the imperative mood in the subject line
5. Limit the commit messages lines to 72 characters
    * Use “Commit Message Inspections” in IntelliJ IDE *Settings -> Version Control -> Commit*
    * vim: ```autocmd FileType gitcommit setlocal textwidth=72```

## Checklist

Before submitting the pull request, make sure that you can say "YES" to each point in this short checklist:

- You provided the link to the related issue(s) from YouTrack
- You made a reasonable amount of changes related only to the provided issues
- You can explain changes made in the pull request
- You ran the build locally and verified new functionality
- You ran related tests locally and they passed
- You do not have merge commits in the pull request
