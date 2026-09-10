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
When developing the browser subset or building code that needs unpublished subset changes, pass
`--include-build kotlinx-browser-common-subset` from `html` to use its local sources instead of the
configured published artifact. Omit this option when the published subset already provides the
required APIs and behavior; see the local browser subset section below.
The `compose.html.eap.enabled` value in `html/local.properties` takes precedence over the value in
`html/gradle.properties` and command-line project properties.

Published coordinates start at version `0.0.1`:

- `org.jetbrains.compose.html.eap:html-core-eap:0.0.1`
- `org.jetbrains.compose.html.eap:html-svg-eap:0.0.1`
- `org.jetbrains.compose.html.eap:internal-html-core-runtime-eap:0.0.1`

Consumers must add `https://packages.jetbrains.team/maven/p/cmp/dev` as a Maven repository. The EAP
dependencies intentionally use Gradle's `implementation` scope while the commonized API is being
evaluated.

```kotlin
dependencies {
    implementation("org.jetbrains.compose.html.eap:html-core-eap:0.0.1")
    implementation("org.jetbrains.compose.html.eap:html-svg-eap:0.0.1")
    implementation("org.jetbrains.compose.html.eap:internal-html-core-runtime-eap:0.0.1")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:0.0.1+dev1")
}
```

Do not put the regular and EAP Compose HTML artifacts on the same compilation classpath because
they intentionally expose the same Kotlin packages and declarations.

## Local browser subset and publishing

The browser subset is a separate Gradle build. EAP modules normally consume its published Maven
artifact, so local subset edits are not picked up automatically. To develop or test against those
edits without publishing them first, include the subset as a composite build. Gradle then
substitutes the Maven dependency with the local project:

```shell
./gradlew -Pcompose.html.eap.enabled=true --include-build kotlinx-browser-common-subset \
    :html-core-eap:check :html-svg-eap:check :internal-html-core-runtime-eap:check
```

Run this command from `html`. Do not use this substitution for release publishing: it changes
generated publication dependencies to the local subset's version (`0.0.1-SNAPSHOT` by default).

If EAP artifacts depend on unpublished subset changes, first publish the subset under a new version,
set `compose.html.eap.kotlinx-browser-common-subset.version` to that version, and verify without
`--include-build` against the published dependency.
The consumer dependency example above must then use that new subset version too.

## String rendering example

This JVM-only example renders a complete page to `html/eap/example/build/index.html` and opens it in your
default browser. From the repository root, run:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-example:run
```
