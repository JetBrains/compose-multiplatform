rootProject.name = "ComposeDemoPlugin"

pluginManagement {
    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("plugin.compose").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}
