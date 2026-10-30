# Compose HTML compiler plugin

Prepares static HTML at compile time to reduce work during server-side rendering.

## Activate

Use the [Compose HTML EAP runtime](../README.md#published-artifacts).
Add the development repository to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        gradlePluginPortal()
    }
}
```

Add the plugin to your module's `build.gradle.kts` alongside your existing Kotlin and Compose compiler plugins:

```kotlin
plugins {
    id("org.jetbrains.compose.html.lowering") version "<version>"
}
```

Lowering is enabled for JVM targets.
The Gradle plugin adds the compiler plugin and sets the required compiler order.

### Using a local checkout

To resolve both plugins from source, add these entries to your project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("/path/to/html/eap/compiler-plugin/gradle-plugin")
    // Keep your existing plugin repositories here.
}

includeBuild("/path/to/html")
```

For the local EAP runtime setup, see the [EAP README](../README.md).

## Configure

Optional settings in your module's `build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

htmlLowering {
    enabledTargets.set(setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm))
}
```

Currently, only JVM rendering is supported. Set it to `emptySet()` to disable lowering for all targets.

## Test

Golden IR tests compare the HTML plugin's output before Compose and JVM backend lowering.
Source fixtures (`.kt`) and expected IR (`.txt`) live in [testData/ir](testData/ir).
Run from the repository root with a JDK 11 toolchain available:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :compose-html-compiler-plugin:test
```

A mismatch writes a sibling `.txt.actual` file. Review the diff before accepting it.
For example, from `html/eap/compiler-plugin`:

```shell
diff -u testData/ir/formControlPrecedence.txt testData/ir/formControlPrecedence.txt.actual
cp testData/ir/formControlPrecedence.txt.actual testData/ir/formControlPrecedence.txt
```
