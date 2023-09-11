## Compatibility and versioning overview

### Supported platforms
   * Android
   * iOS
   * macOS (x86-64, arm64)
   * Windows (x86-64)
   * Linux (x86-64, arm64)
   * Web browsers

### Limitations

Following limitations apply to 1.0 release.

  * Only 64-bit x86 Windows is supported
  * Only JDK 11 or later is supported due to the memory management scheme used in Skia bindings
  * Only JDK 17 or later is supported for packaging native distributions due to jpackage limitations

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
1.8.0  | 1.3.0 | 1.3.0 is not supported by earlier k/native versions
1.8.10 | 1.3.1
1.8.20 | 1.4.0
1.8.21 | 1.4.3
1.8.22 | 1.4.3
1.9.0 | 1.4.3
1.9.10 | 1.5.1

### Using the latest Kotlin version 

When a new version of Kotlin is released, the corresponding Compose Multiplatform release may not yet have been published. There are still ways to use it, although stability is not guarantied. Even if it compiles fine, there can be hidden runtime errors, so it is not recommended to use them for production builds.

#### Using Jetpack Compose Compiler

> **Note**   
> The Jetpack Compose Compiler Plugin `androidx.compose.compiler:compiler` is guaranteed to function properly for **Kotlin/JVM** targets, including both the desktop and Android platforms. However, its reliability may not extend to **Kotlin/JS** and **Kotlin/Native** targets. For these scenarios, we recommend using the Compose Multiplatform Compiler Plugin `org.jetbrains.compose.compiler:compiler` to ensure compatibility. See [Using the Compose Multiplatform compiler](#using-the-compose-multiplatform-compiler).

The compilation process of composable functions is handled by the Compose compiler plugin. Each release of the compiler plugin is strictly bound to a single version of the Kotlin compiler. Normally, the Gradle plugin chooses an appropriate version of the compiler plugin automatically. But there is a way to choose another version of Compose Compiler. For example, you can use Jetpack Compose Compiler published by Google.

First, check [this page](https://developer.android.com/jetpack/androidx/releases/compose-kotlin#pre-release_kotlin_compatibility) to find a compatible version. If there is one, use it this way:
```
compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.4.2")
}
```
(`1.4.2` corresponds Kotlin 1.8.10)


#### Disabling Kotlin compatibility check

If there is no compatible version of Jetpack Compose Compiler (or you encountered errors), you can try to use Compose Compiler for another version of Kotlin, but disable the Kotlin version check. It can work, if you upgrade to a hotfix version of Kotlin, and most probably won't work if you upgrade to a major version of Kotlin.

```
compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.7.20"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.7.21")
}
```

Here we set a fixed version of Compose Compiler and configure it by specifying additional arguments. The argument `suppressKotlinVersionCompatibilityCheck` disables the internal Kotlin check that happens inside the compiler. In this argument you should specify the version of Kotlin that is applied to your project. It is required to avoid situations when you upgraded Kotlin and forgot to update Compose Compiler.

#### Using the Compose Multiplatform Compiler

Typically, `-dev` versions of Compose Multiplatform (such as 1.5.0-dev1084) contain actual version mappings from Kotlin to the Compose Compiler. This includes Beta and RC (Release Candidate) builds of Kotlin.

If you're looking to test a Beta or RC version of Kotlin that isn't directly supported by the stable release of Compose Multiplatform, there are two potential solutions:

1) Consider using the most recent `-dev` build of Compose Multiplatform. See the [releases page](https://github.com/JetBrains/compose-multiplatform/releases).
2) Manually specify the `kotlinCompilerPlugin` version. You can find the suitable version by consulting the following file:  [ComposeCompilerCompatibility](https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/ComposeCompilerCompatibility.kt#L7).

For instance, if you wish to use Kotlin 1.9.0-RC, you can do so in the following way:

```kotlin
compose {
    kotlinCompilerPlugin.set("1.4.8-beta")
}
```

**Note:** Unstable versions of Compose Multiplatform compiler plugin (like `1.4.8-beta)` are not available in mavenCentral. Please add `maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")` to the list of repositories in order to use such versions.

### Relationship between the Jetpack Compose and Compose Multiplatform release cycles

Compose Multiplatform shares a lot of code with [Jetpack Compose](https://developer.android.com/jetpack/compose) for Android, a framework developed by Google.
We keep our release cycles aligned, making sure that the common part is properly tested and stabilized.

When a new version of Jetpack Compose is released, we pick the release commit, use it as a base for the next [Compose Multiplatform](https://github.com/JetBrains/androidx) version, finish new platform features, stabilize all platforms, and release Compose Multiplatform.
The gap between a Compose Multiplatform release and a Jetpack Compose release is usually 1 to 3 months.

When you build your application for Android, the artifacts published by Google are used. For example, if you apply the Compose Multiplatform 1.5.0 Gradle plugin and add `implementation(compose.material3)` to your `dependencies`, then your project will use the `androidx.compose.material3:material3:1.1.1` artifact in the Android target (but `org.jetbrains.compose.material3:material3:1.5.0` in the other targets). See the table below to know exactly which version of the Jetpack Compose artifact is used.

Compose Multiplatform version | Jetpack Compose version | Jetpack Compose Material3 version
--- | --- | ---
[1.5.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.1)|1.5.0|1.1.1
[1.5.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.5.0)|1.5.0|1.1.1
[1.4.3](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.4.3)|1.4.3|1.0.1
[1.4.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.4.1)|1.4.3|1.0.1
[1.4.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.4.0)|1.4.0|1.0.1
[1.3.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.3.1)|1.3.3|1.0.1
[1.3.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.3.0)|1.3.3|1.0.1
[1.2.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.2.1)|1.2.1|1.0.0-alpha14
[1.2.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.2.0)|1.2.1|1.0.0-alpha14
[1.1.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.1.1)|1.1.0|1.0.0-alpha05
[1.1.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.1.0)|1.1.0|1.0.0-alpha05
[1.0.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.0.1)|1.1.0-beta02|1.0.0-alpha03
[1.0.0](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.0.0)|1.1.0-beta02|1.0.0-alpha03
