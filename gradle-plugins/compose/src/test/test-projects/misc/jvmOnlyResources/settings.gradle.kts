rootProject.name = "jvmOnlyResources"
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("org.jetbrains.kotlin.jvm").version("KOTLIN_VERSION_PLACEHOLDER")
        id("org.jetbrains.compose").version("COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
    }
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}