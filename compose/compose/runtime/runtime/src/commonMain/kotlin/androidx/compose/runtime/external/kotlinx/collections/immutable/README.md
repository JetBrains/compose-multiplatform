Kotlinx immutable collections

Imported from https://github.com/Kotlin/kotlinx.collections.immutable
commit 75ed654e34e7111a076d87b8c075fd140e6b4cf7 (HEAD, tag: v0.3.4)

At the time of this import, kotlinx.collections.immutable was unstable
and Jetbrains raised a concern that by Compose depending upon it,
there would be issues when breaking changes occur in upstream and users
have conflicting versions of the library on their classpath.  JB
requested that we shade the library in order avoid binary compat
issues.  We don't have effective tools to shade common dependencies
so importing the source and renaming the package ended up being
the most reasonable strategy.

This was done as a fix to: b/183400125


