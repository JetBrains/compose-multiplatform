pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                println("COMPOSE_INTEGRATION_VERSION=[${requested.version}]")
            }
        }
    }
}

include(":lib")
