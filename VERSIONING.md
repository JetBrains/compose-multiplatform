 ## Features

### Supported platforms
   * macOS (x86-64, arm64)
   * Windows (x86-64)
   * Linux (x86-64, arm64)
   * Web browsers

### Limitations

Following limitations apply to 1.0 release.

  * Only 64-bit x86 Windows is supported
  * Only JDK 11 or later is supported due to the memory management scheme used in Skia bindings
  * Only JDK 15 or later is supported for packaging native distributions due to jpackage limitations

Knowing issues on older versions:
- OpenJDK 11.0.12 has [an issue](https://github.com/JetBrains/compose-jb/issues/940), when we switch keyboard layout on MacOs (isn't reproducible in OpenJDK 11.0.15)
  
[comment]: <> (__SUPPORTED_GRADLE_VERSIONS__)

### Kotlin compatibility

A new version of Kotlin may be not supported immediately after its release. But after some time we will release a version of Compose Multiplatform
that supports it.
Starting from 1.2.0, Compose Multiplatform supports multiple versions of Kotlin.

Kotlin version | Minimal Compose version | Notes
--- | --- | ---
1.5.31 | 1.0.0
1.6.20 | 1.1.1
1.7.10 | 1.2.0
1.7.20 | 1.2.0 | JS is not supported (will be fixed in the next versions)

### Relation to Jetpack Compose and Compose Multiplatform release cycle

Compose Multiplatform shares a lot of code with [Jetpack Compose](https://developer.android.com/jetpack/compose) for Android developed by Google.
We keep our release cycles aligned, so the common part is properly tested and stabilized.

When a new version of Jetpack Compose is released, we pick the release commit, use it as a base for the next [Compose Multiplatform](https://github.com/JetBrains/androidx) version, finish new platform features, stabilize all platforms, and release Compose Multiplatform after some time.
A gap between a Compose Multiplatform release and a Jetpack Compose release is usually 1-3 months.

When you build your application for Android, the artifacts published by Google are used. For example, if you applied Compose Multiplatform 1.2.0 Gradle plugin, and added `implementation(compose.material3)` into your `dependencies`, then you project uses the `androidx.compose.material3:material3:1.0.0-alpha14` artifact in Android target (but `org.jetbrains.compose.material3:material3:1.2.0` in the other targets). See `Updated dependencies` sections in [CHANGELOG](https://github.com/JetBrains/compose-jb/blob/master/CHANGELOG.md) to know exactly what version of the Jetpack Compose artifact is used.

The Compose Compiler version can be changed independently of other Compose libraries. In order to support newer versions of Kotlin, you may want to use [a cutting edge Compose Compiler](https://developer.android.com/jetpack/androidx/releases/compose-kotlin#pre-release_kotlin_compatibility) published by Google in your Compose Multiplatform project. For example, when the new version of Kotlin is released, a corresponding Compose Multiplatform release may not yet have been published, but manually specifying a newer Compose Compiler version can allow you to build your Compose Multiplatform app using the latest Kotlin release. In that case you can set `kotlinCompilerPlugin` in the `compose` section of your `build.gradle.kts` file:

```kotlin
compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.3.1")
}
```
But because it isn't tested with Compose Multiplatform, stability isn't guaranteed.
