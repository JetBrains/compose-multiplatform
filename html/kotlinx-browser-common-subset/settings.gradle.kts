pluginManagement {
    val htmlBuildProperties = java.util.Properties().apply {
        settingsDir.resolve("../buildSrc/gradle.properties").inputStream().use(::load)
    }
    val kotlinVersion = requireNotNull(htmlBuildProperties.getProperty("kotlin.version")) {
        "kotlin.version is missing from html/buildSrc/gradle.properties"
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("multiplatform").version(kotlinVersion)
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

includeBuild("generator") {
    name = "kotlinx-browser-common-subset-generator"
}

rootProject.name = "kotlinx-browser-common-subset"
