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
            version("agp", extra["agp.version"].toString())
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "imageviewer"

include(":androidApp")
include(":shared")
include(":webApp")
include(":desktopApp")
include(":mapview-desktop")
