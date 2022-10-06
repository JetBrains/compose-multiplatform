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

When the new minor version of Jetpack Compose is released, we pick the release commit, use it as a base for [Compose Multiplatform fork](https://github.com/JetBrains/androidx), finish new platform features, stabilize all platforms, and release Compose Multiplatform after some time.
The gap between the Compose Multiplatform release and the Jetpack Compose release is usually 1-3 months.

When you build your application for Android, the artifacts published by Google are used.

In the next table you can see versions of the corresponding Jetpack Compose libraries.
These are the versions on which we based Compose Multiplatform, and the versions used when you build your application for Android.

Compose Multiplatform | Jetpack Compose Compiler | Jetpack Compose Runtime, UI, Foundation, Material | Jetpack Compose Material 3
--- |--------------------|---------------------------------------------------| ---
1.0.0 | 1.1.0-rc03 | 1.1.0-rc03                                        | 1.0.0-alpha03
1.1.1 | 1.1.0 | 1.1.0                                             | 1.0.0-alpha05
1.2.0 | 1.3.2 | 1.2.1                                             | 1.0.0-alpha14

You may want to use [a cutting edge Compose Compiler](https://developer.android.com/jetpack/androidx/releases/compose-kotlin) published by Google for your Compose Multiplatform project.
For example, when the new Kotlin is released,  and there is the Jetpack Compose Compiler that supports it,
and Compose Multiplatform still doesn't support it.

In that case you can set `kotlinCompilerPlugin` in the `compose` section of your `build.gradle.kts` file:

```kotlin
compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.3.1")
}
```
But because it isn't tested with Compose Multiplatform, stability isn't guaranteed.
