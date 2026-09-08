# Compose HTML EAP

This directory contains the experimental commonized Compose HTML core and its internal runtime.
They retain the regular Compose HTML Kotlin package names, but use distinct Gradle project names and
the `org.jetbrains.compose.html.eap` Maven group.

The projects are excluded from the Gradle build by default. Enable them locally by creating
`html/local.properties` with:

```properties
compose.html.eap.enabled=true
```

The file is ignored by Git. CI enables the projects with the equivalent Gradle property.
Values in `html/local.properties` take precedence over values from `html/gradle.properties` and
command-line project properties.

Published coordinates start at version `0.0.1`:

- `org.jetbrains.compose.html.eap:html-core-eap:0.0.1`
- `org.jetbrains.compose.html.eap:internal-html-core-runtime-eap:0.0.1`

Consumers must add `https://packages.jetbrains.team/maven/p/cmp/dev` as a Maven repository. The EAP
dependencies intentionally use Gradle's `implementation` scope while the commonized API is being
evaluated.

```kotlin
dependencies {
    implementation("org.jetbrains.compose.html.eap:html-core-eap:0.0.1")
    implementation("org.jetbrains.compose.html.eap:internal-html-core-runtime-eap:0.0.1")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:0.0.1+dev1")
}
```

Do not put the regular and EAP Compose HTML artifacts on the same compilation classpath because
they intentionally expose the same Kotlin packages and declarations.
