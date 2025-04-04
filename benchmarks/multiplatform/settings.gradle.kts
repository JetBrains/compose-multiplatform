pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

rootProject.name = "compose-benchmarks"

dependencyResolutionManagement {
    versionCatalogs{
        create("libs") {
            // Override Kotlin and Compose versions with properties
            providers.run {
                with(gradleProperty("kotlin.version")) {
                    if (isPresent) version("kotlin", get())
                }
                with(gradleProperty("compose.version")) {
                    if (isPresent) version("compose-multiplatform", get())
                }
            }
        }
    }
}

include(":benchmarks")
