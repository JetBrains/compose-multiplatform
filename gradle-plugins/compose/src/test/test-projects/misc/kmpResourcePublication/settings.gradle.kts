rootProject.name = "kmpResourcePublication"
include(":cmplib")
include(":featureModule")
//include(":appModule")
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        //todo temporary repo for custom KGP plugin
        maven("https://packages.jetbrains.team/files/p/mpp/kgp-for-compose2")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform").version("KOTLIN_VERSION_PLACEHOLDER")
        id("org.jetbrains.kotlin.plugin.compose").version("KOTLIN_VERSION_PLACEHOLDER")
        id("org.jetbrains.compose").version("COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
        id("com.android.kotlin.multiplatform.library").version("AGP_VERSION_PLACEHOLDER")
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        //todo temporary repo for custom KGP plugin
        maven("https://packages.jetbrains.team/files/p/mpp/kgp-for-compose2")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        mavenLocal()
        maven {
            url = uri(rootProject.projectDir.resolve("my-mvn"))
        }
    }
}