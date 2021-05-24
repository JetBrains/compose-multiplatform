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
include("web-integration-core")
include("web-integration-widgets")

project(":web-core").projectDir = file("$rootDir/core")
project(":web-integration-core").projectDir = file("$rootDir/integration-core")
project(":web-integration-widgets").projectDir = file("$rootDir/integration-widgets")
