@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", extra["kotlin.version"].toString())
            version("compose", extra["compose.version"].toString())
        }
    }
}

rootProject.name = "compose-in-uikit"

include(":shared")
