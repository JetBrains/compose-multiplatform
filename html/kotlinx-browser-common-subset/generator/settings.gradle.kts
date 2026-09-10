pluginManagement {
    val htmlBuildProperties = java.util.Properties().apply {
        settingsDir.resolve("../../buildSrc/gradle.properties").inputStream().use(::load)
    }
    val kotlinVersion = requireNotNull(htmlBuildProperties.getProperty("kotlin.version")) {
        "kotlin.version is missing from html/buildSrc/gradle.properties"
    }
    val kspVersion = providers.gradleProperty("ksp.version").get()

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        id("com.google.devtools.ksp").version(kspVersion)
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":ksp-runner")
include(":verification")

rootProject.name = "kotlinx-browser-common-subset-generator"
