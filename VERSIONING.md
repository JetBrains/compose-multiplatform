## Compatibility and versioning overview

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
1.7.20 | 1.2.0 | JS is not supported (fixed in the 1.2.1)
1.7.20 | 1.2.1

### Using the latest Kotlin version 

When a new version of Kotlin is released, the corresponding Compose Multiplatform release may not yet have been published. There are still ways to use it, although stability is not guarantied. Even if it compiles fine, there can be hidden runtime errors, so it is not recommended to use them for production builds.

#### Using Jetpack Compose Compiler

The compilation process of composable functions is handled by the Compose compiler plugin. Each release of the compiler plugin is strictly bound to a single version of the Kotlin compiler. Normally, the Gradle plugin chooses an appropriate version of the compiler plugin automatically. But there is a way to choose another version of Compose Compiler. For example, you can use Jetpack Compose Compiler published by Google.

First, check [this page](https://developer.android.com/jetpack/androidx/releases/compose-kotlin#pre-release_kotlin_compatibility) to find a compatible version. If there is one, use it this way:
```
compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.4.0-alpha02")
}
```
(`1.4.0-alpha02` corresponds Kotlin 1.7.21)

#### Disabling Kotlin compatibility check

If there is no compatible version of Jetpack Compose Compiler (or you encountered errors), you can try to use Compose Compiler for another version of Kotlin, but disable the Kotlin version check. It can work, if you upgrade to a hotfix version of Kotlin, and most probably won't work if you upgrade to a major version of Kotlin.

```
compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.7.20"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.7.21")
}
```

Here we set a fixed version of Compose Compiler and configure it by specifying additional arguments. The argument `suppressKotlinVersionCompatibilityCheck` disables the internal Kotlin check that happens inside the compiler. In this argument you should specify the version of Kotlin that is applied to your project. It is required to avoid situations when you upgraded Kotlin and forgot to update Compose Compiler.

### Relationship between the Jetpack Compose and Compose Multiplatform release cycles

Compose Multiplatform shares a lot of code with [Jetpack Compose](https://developer.android.com/jetpack/compose) for Android, a framework developed by Google.
We keep our release cycles aligned, making sure that the common part is properly tested and stabilized.

When a new version of Jetpack Compose is released, we pick the release commit, use it as a base for the next [Compose Multiplatform](https://github.com/JetBrains/androidx) version, finish new platform features, stabilize all platforms, and release Compose Multiplatform.
The gap between a Compose Multiplatform release and a Jetpack Compose release is usually 1 to 3 months.

When you build your application for Android, the artifacts published by Google are used. For example, if you apply the Compose Multiplatform 1.2.0 Gradle plugin and add `implementation(compose.material3)` to your `dependencies`, then your project will use the `androidx.compose.material3:material3:1.0.0-alpha14` artifact in the Android target (but `org.jetbrains.compose.material3:material3:1.2.0` in the other targets). See the `Updated dependencies` sections in the [CHANGELOG](https://github.com/JetBrains/compose-jb/blob/master/CHANGELOG.md) to know exactly which version of the Jetpack Compose artifact will be used.
