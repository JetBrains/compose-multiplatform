pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    plugins {
        val kotlinVersion = extra["kotlin.version"] as String
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)

        val agpVersion = extra["agp.version"] as String
        id("com.android.application").version(agpVersion)

        val composeVersion = extra["compose.version"] as String
        id("org.jetbrains.compose").version(composeVersion)
    }
}

rootProject.name = "falling-balls-mpp"
