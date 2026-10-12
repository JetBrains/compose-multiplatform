pluginManagement {
    repositories {
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        kotlin("plugin.compose").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("compose", extra["compose.version"].toString())
        }
    }
}