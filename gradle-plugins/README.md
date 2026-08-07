# JetBrains Compose gradle plugin for easy configuration

Environment variables:
* `COMPOSE_GRADLE_PLUGIN_VERSION` - version of plugin
* `COMPOSE_GRADLE_PLUGIN_COMPOSE_VERSION` - version of JetBrains Compose used by the plugin

## Developing and testing

In order to work on the plugin and try out the changes on a Compose project,
a modified version of the plugin can be published to Maven local and then
used by a project.

### Publishing a snapshot to Maven local

Modify `gradle.properties` and set variable `deploy.version` to something
meaningful depending on the current compose version, such as this:

    deploy.version=1.2.0-SNAPSHOT

Afterwards, run:

    ./gradlew publishToMavenLocal

to build the plugin artifacts and publish them locally to
`~/.m2/repository/org/jetbrains/compose`.

### Using the snapshot version plugin from Maven local

In your project that uses Compose, make sure to have a `settings.gradle.kts`
with plugin management configured to resolve from Maven local:

```
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

Then, in your `build.gradle.kts` file, use the snapshot version:

    id("org.jetbrains.compose") version "1.2.0-SNAPSHOT"
