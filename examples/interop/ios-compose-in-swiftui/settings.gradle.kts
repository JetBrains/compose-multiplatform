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
            version("compose-multiplatform", extra["compose.version"].toString())
            version("kotlin", extra["kotlin.version"].toString())
        }
    }
}

rootProject.name = "compose-in-swiftui"

include(":shared")
