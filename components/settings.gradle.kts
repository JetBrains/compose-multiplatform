pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenLocal()

        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://packages.jetbrains.team/maven/p/karpovich-sandbox/ksandbox")
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("com.android.library").version(extra["agp.version"] as String)
    }
}

include(":SplitPane:library")
include(":SplitPane:demo")
include(":AnimatedImage:library")
include("AnimatedImage:demo")
include(":resources:library")
include(":resources:demo:androidApp")
include(":resources:demo:desktopApp")
include(":resources:demo:jsApp")
include(":resources:demo:shared")
