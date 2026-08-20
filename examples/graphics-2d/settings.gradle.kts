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
            version("agp", extra["agp.version"].toString())
            version("compose-multiplatform", extra["compose.version"].toString())
            version("kotlin", extra["kotlin.version"].toString())
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "graphics-2d"

include(":androidApp")
include(":shared")
include(":desktopApp")
include(":jsApp")
