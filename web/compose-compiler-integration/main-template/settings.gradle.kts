pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                val useVersion = if (extra.has("COMPOSE_CORE_VERSION")) {
                    extra["COMPOSE_CORE_VERSION"].toString()
                } else {
                    "0.0.0-SNASPHOT"
                }
                println("COMPOSE_INTEGRATION_VERSION=[$useVersion]")
                useVersion(useVersion)
            }
        }
    }
}

include(":lib")
