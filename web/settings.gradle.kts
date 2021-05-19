pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { 
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") 
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ui/dev")
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                useModule("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:${extra["COMPOSE_CORE_VERSION"]}")
            }
        }
    }
}

include("web-core")
include("web-integration")

project(":web-core").projectDir = file("$rootDir/core")
project(":web-integration").projectDir = file("$rootDir/integration")
