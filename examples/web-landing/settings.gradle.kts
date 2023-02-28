pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://packages.jetbrains.team/maven/p/karpovich-sandbox/ksandbox")
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "compose-web-lp"

