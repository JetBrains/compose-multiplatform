rootProject.name = "composeApp"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "KOTLIN_VERSION_PLACEHOLDER"
        id("org.jetbrains.kotlin.plugin.compose") version "KOTLIN_VERSION_PLACEHOLDER"
        id("org.jetbrains.compose") version "COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER"
    }
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/cmp/dev")
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
    }
}
