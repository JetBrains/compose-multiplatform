# Compose HTML compiler plugin

Prepares static HTML at compile time to reduce work during server-side rendering.

## Activate

Use the [Compose HTML EAP runtime](../README.md#published-artifacts).
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
