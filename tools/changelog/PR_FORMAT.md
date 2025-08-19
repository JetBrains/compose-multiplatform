Each Pull Request should contain "## Release Notes" section that describes changes in this PR.

Each release, changes from all PRs are combined into a list and added to CHANGELOG.md
(by [changelog.main.kts](changelog.main.kts) script).

## Possible Release Notes

### No Release Notes
```
## Release Notes
N/A
```

### Simple Change
```
## Release Notes
### Highlights - iOS
- Describe a change
```

### Simple Change with Details
```
## Release Notes
### Highlights - iOS
- Describe a change
  - Describe details 1
  - Describe details 2
```

### Multiple Changes
```
## Release Notes
### Features - Desktop
- Describe change 1
  - Describe details
- Describe change 2

### Fixes - Web
- Describe change 3
```

### Prerelease Fix
Will be included in alpha/beta/rc changelog, excluded from stable.
```
## Release Notes
### Fixes - Multiple Platforms
- _(prerelease fix)_ Fixed CPU overheating on pressing Shift appeared in 1.8.0-alpha02
```

### Cherry-picks to a release branch
The PR can contain only cherry-picks. We can point to them instead of copying the release notes.
```
## Release Notes
https://github.com/JetBrains/compose-multiplatform/pull/5292
https://github.com/JetBrains/compose-multiplatform/pull/5294
https://github.com/JetBrains/compose-multiplatform/pull/5295
```

## Possible Sections
<!-- 
- Note that this is parsed by [changelog.main.kts]
- Update the PR templates after changing the sections:
https://github.com/JetBrains/compose-multiplatform/edit/master/.github/PULL_REQUEST_TEMPLATE.md
https://github.com/JetBrains/compose-multiplatform-core/edit/jb-main/.github/PULL_REQUEST_TEMPLATE.md
-->
 
### Sections
```
- Highlights             # major features, performance improvements
- Known Issues           # issues planned to be fixed, with possible workarounds
- Breaking Changes       # incompatible changes without deprecation cycle
- Migration Notes        # deprecations, removals, minimal version increases, defined behavior changes
- Features               # minor features, performance improvements
- Fixes                  # bug fixes, undefined behavior changes
```

### Subsections
```
- Multiple Platforms     # any module, 2 or more platform changes
- iOS                    # any module, iOS-only changes
- Desktop                # any module, Desktop-only changes
- Web                    # any module, Web-only changes
- Android                # any module, Android-only changes
- Resources              # specific module, prefer it over the platform ones
- Gradle Plugin          # specific module, prefer it over the platform ones
- Lifecycle              # specific module, prefer it over the platform ones
- Navigation             # specific module, prefer it over the platform ones
- SavedState             # specific module, prefer it over the platform ones
```
