rootProject.name = "WebJsMain"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "KOTLIN_VERSION_PLACEHOLDER"
        id("org.jetbrains.kotlin.plugin.compose") version "KOTLIN_VERSION_PLACEHOLDER"
        id("org.jetbrains.compose") version "COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER"
    }
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
