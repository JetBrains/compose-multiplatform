plugins {
    java
    id("maven-publish")
}

mavenPublicationConfig {
    displayName = "JDK version probe"
    description = "JDK version probe (Internal)"
    artifactId = "gradle-plugin-internal-jdk-version-probe"
}

tasks.jar.configure {
    manifest.attributes["Main-Class"] = "org.jetbrains.compose.desktop.application.internal.JdkVersionProbe"
}