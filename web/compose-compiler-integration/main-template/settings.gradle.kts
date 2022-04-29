pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }

    resolutionStrategy {
        val kotlinVersion = extra["kotlin.version"] as String
        println("KotlinVersion=[$kotlinVersion]")
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                val useVersion = if (extra.has("COMPOSE_CORE_VERSION")) {
                    extra["COMPOSE_CORE_VERSION"].toString()
                } else {
                    "0.0.0-SNASPHOT"
                }
                println("COMPOSE_INTEGRATION_VERSION=[$useVersion]")
                useVersion(useVersion)
            } else if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                useVersion(kotlinVersion)
            }
        }
    }
}

include(":lib")
