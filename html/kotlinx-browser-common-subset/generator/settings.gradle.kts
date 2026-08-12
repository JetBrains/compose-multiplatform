pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm").version("2.3.20")
        kotlin("multiplatform").version("2.3.20")
        id("com.google.devtools.ksp").version("2.3.11")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":runner")
include(":verification")

rootProject.name = "kotlinx-browser-common-subset-generator"
