pluginManagement {
    plugins {
        kotlin("multiplatform") version "KOTLIN_VERSION_PLACEHOLDER"
        kotlin("plugin.compose") version "KOTLIN_VERSION_PLACEHOLDER"
        id("org.jetbrains.compose") version "COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER"
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

rootProject.name = "skiko-runtime-publication"
