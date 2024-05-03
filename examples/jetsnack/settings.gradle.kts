pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        val kotlinGeneration = extra["kotlin.generation"]
        kotlin("multiplatform").version(extra["kotlin.version.$kotlinGeneration"] as String)
        kotlin("android").version(extra["kotlin.version.$kotlinGeneration"] as String)
        id("com.android.application").version(extra["agp.version"] as String)
        id("com.android.library").version(extra["agp.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.wasm.version.$kotlinGeneration"] as String)
    }
}

rootProject.name = "compose-jetsnack"

include(":common", ":android", ":desktop", ":web")
