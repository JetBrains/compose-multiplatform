pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        if (extra["compose.useMavenLocal"] == "true") {
            mavenLocal()
        }
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose") //version is not required because the plugin is included to the build
        id("com.android.library").version(extra["agp.version"] as String)
        id("org.jetbrains.kotlinx.binary-compatibility-validator").version("0.15.0-Beta.2")
    }

    includeBuild("../gradle-plugins")
}

dependencyResolutionManagement {
    repositories {
        if (extra["compose.useMavenLocal"] == "true") {
            mavenLocal() // mavenLocal should be the first to get the correct version of skiko during a local build.
        }
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":SplitPane:library")
include(":SplitPane:demo")
include(":AnimatedImage:library")
include(":AnimatedImage:demo")
include(":resources:library")
include(":resources:demo:androidApp")
include(":resources:demo:desktopApp")
include(":resources:demo:shared")
include(":ui-tooling-preview:library")
include(":ui-tooling-preview:demo:desktopApp")
include(":ui-tooling-preview:demo:shared")
