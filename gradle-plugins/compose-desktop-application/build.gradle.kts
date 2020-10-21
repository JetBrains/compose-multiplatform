plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("maven-publish")
}

gradlePluginConfig {
    pluginId = "org.jetbrains.compose.desktop.application"
    artifactId = "compose-desktop-application-gradle-plugin"
    displayName = "Jetpack Compose Desktop Application Plugin"
    description = "Plugin for creating native distributions and run configurations"
    implementationClass = "org.jetbrains.compose.desktop.application.ApplicationPlugin"
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
}
