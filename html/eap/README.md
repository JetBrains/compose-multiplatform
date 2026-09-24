# Compose HTML EAP

This directory contains the experimental commonized Compose HTML core, SVG library, and internal
runtime. They retain the regular Compose HTML Kotlin package names, but use distinct Gradle project
names and the `org.jetbrains.compose.html.eap` Maven group.

The projects are excluded from the Gradle build by default. Enable them locally by creating
`html/local.properties` with:

```properties
compose.html.eap.enabled=true
```

The file is ignored by Git. CI enables the projects with the equivalent Gradle property.
The `compose.html.eap.enabled` value in `html/local.properties` takes precedence over the value in
`html/gradle.properties` and command-line project properties.

## HTML validation

Fast validation is the default. Set `COMPOSE_HTML_VALIDATE_STRICTLY=true` on the JVM or in Node to render
with strict validation. Browser hydration follows the server's mode. Direct callers can pass
`validateStrictly = true` to `hydrateComposable`.

| During hydration | Fast (default) | Strict |
| --- | --- | --- |
| Text, attributes, or styles differ | Keeps server values until a Compose update | Reports a mismatch and renders on the client |
| `allowHydrationMismatch()` | Applies client values | Applies client values |
| Structure differs | Renders on the client | Renders on the client |

Strict mode also checks class tokens and foreign-attribute collisions.

## Published artifacts

The [JetBrains development repository](https://packages.jetbrains.team/maven/p/cmp/dev/org/jetbrains/compose/html/eap/)
lists the available EAP modules and versions. Replace `<version>` below.

```kotlin
repositories {
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

dependencies {
    implementation("org.jetbrains.compose.html.eap:html-core-eap:<version>")
    implementation("org.jetbrains.compose.html.eap:html-svg-eap:<version>")
    implementation("org.jetbrains.compose.html.eap:internal-html-core-runtime-eap:<version>")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:<version>")
}
```
Local build versions are configured in `html/gradle.properties`.

Do not put the regular and EAP Compose HTML artifacts on the same compilation classpath because
they intentionally expose the same Kotlin packages and declarations.

## Examples

### String Rendering

The example renders a complete page to
`html/eap/examples/string-rendering/build/index.html` and opens it in your default browser. From
the repository root, run:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-example:run
```

### Hydration example

Renders its initial HTML on the JVM, bundles the browser entry point, and opens
the interactive result in a development server. From the repository root, run either target:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-hydration-example:jsBrowserDevelopmentRun

html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-hydration-example:wasmJsBrowserDevelopmentRun
```
