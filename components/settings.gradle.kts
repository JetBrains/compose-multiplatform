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
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("com.android.library").version(extra["agp.version"] as String)
    }
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

include(":SplitPane:components-splitpane")
include(":SplitPane:components-splitpane-demo")
include(":AnimatedImage:components-animatedimage")
include(":AnimatedImage:components-animatedimage-demo")
include(":resources:components-resources")
include(":resources:components-resources-demo:androidApp")
include(":resources:components-resources-demo:desktopApp")
include(":resources:components-resources-demo:shared")
include(":ui-tooling-preview:components-ui-tooling-preview")
include(":ui-tooling-preview:components-ui-tooling-preview-demo:desktopApp")
include(":ui-tooling-preview:components-ui-tooling-preview-demo:shared")
