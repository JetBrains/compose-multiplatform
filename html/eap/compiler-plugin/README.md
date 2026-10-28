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

## Publish the compiler artifact

Run these commands from the repository root. EAP modules are disabled by default, so enable them for the publishing invocation:

```bash
# Publish to ~/.m2/repository
./gradlew -Pcompose.html.eap.enabled=true \
  :compose-html-compiler-plugin:publishToMavenLocal

# Publish to the configured remote Maven repository
./gradlew -Pcompose.html.eap.enabled=true \
  :compose-html-compiler-plugin:publishMavenPublicationToInternalRepository
```

The compiler artifact coordinates are `org.jetbrains.compose.html:compose-html-compiler-plugin:<version>`. Its version comes from the shared `compose.html.eap.version` Gradle property (default: `0.0.1`).

The remote repository task targets the repository named `internal`, which defaults to JetBrains' CMP dev Maven repository. Configure `COMPOSE_REPO_URL`, `COMPOSE_REPO_USERNAME`, and `COMPOSE_REPO_KEY` as Gradle properties or environment variables for the destination and credentials. These commands publish the compiler artifact only. The Gradle plugin is a separate build; publish its implementation and plugin marker publications with:

```bash
# Publish the Gradle plugin and marker to ~/.m2/repository
./gradlew -p eap/compiler-plugin/gradle-plugin publishToMavenLocal

# Publish both to the configured remote Maven repository
./gradlew -p eap/compiler-plugin/gradle-plugin publishAllPublicationsToInternalRepository
```

Use the same `COMPOSE_REPO_*` settings for remote publishing. The Gradle plugin build reads the shared `compose.html.eap.version` property as well.

## Configure

Optional settings in your module's `build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

htmlLowering {
    enabledTargets.set(setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm))
}
```

Currently, only JVM rendering is supported. Set it to `emptySet()` to disable lowering for all targets.
