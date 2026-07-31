pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", extra["kotlin.version"].toString())
            version("compose-multiplatform", extra["compose.version"].toString())
        }
    }
}

rootProject.name = "web-compose-bird"

