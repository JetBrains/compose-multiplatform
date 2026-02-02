rootProject.name = "oldAgpResources"
include(":featureModule")
include(":appModule")
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        maven("https://redirector.kotlinlang.org/maven/dev/")
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform").version("KOTLIN_VERSION_PLACEHOLDER")
        id("org.jetbrains.kotlin.plugin.compose").version("KOTLIN_VERSION_PLACEHOLDER")
        id("org.jetbrains.compose").version("COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
        id("com.android.library").version("AGP_VERSION_PLACEHOLDER")
        id("com.android.application").version("AGP_VERSION_PLACEHOLDER")
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        maven("https://redirector.kotlinlang.org/maven/dev/")
        mavenLocal()
    }
}