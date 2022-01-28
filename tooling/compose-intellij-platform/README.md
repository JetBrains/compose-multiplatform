## Compose for IDE Plugin Development

This is an experimental plugin, enabling Intellij plugin development
with Compose.
For now, it only provides Compose runtime classes and native libraries,
so they can be reused by different plugins.

### Usage

The following steps assume [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin) is used
for Intellij plugin development:
1. Add Compose as `compileOnly` dependency to your plugin's dependencies.
2. Add the platform plugin's ID to `intellij` block in a Gradle script and to a
`plugin.xml`.
3. Add `kotlin.stdlib.default.dependency=false` to `gradle.properties`.
4. Use `runIde` Gradle task to run a test IDE.

```
// build.gradle.kts

import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(compose.desktop.currentOs)
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    pluginName.set("Example plugin name")
    version.set("2021.3")
    plugins.set(listOf("org.jetbrains.compose.intellij.platform:0.1.0"))
}
```

```
<!-- src/main/resources/META-INF/plugin.xml -->
<idea-plugin>
    <id>com.jetbrains.ComposeDemoPlugin</id>
    <name>Jetpack Compose for Desktop Demo</name>
    <vendor>Demo Vendor</vendor>

    <description>...</description>

    <depends>com.intellij.modules.platform</depends>
    <depends>org.jetbrains.compose.intellij.platform</depends>
</idea-plugin>
```

A complete example can be found at 
[examples/intellij-plugin-with-experimental-shared-base](../../examples/intellij-plugin-with-experimental-shared-base).