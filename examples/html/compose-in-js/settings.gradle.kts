pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("compose-multiplatform", extra["compose.version"].toString())
            version("kotlin", extra["kotlin.version"].toString())
        }
    }
}

rootProject.name = "web-compose-in-js"
