pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/cmp/dev")
        }
        mavenLocal()
    }

    resolutionStrategy {
        val kotlinVersion = extra["kotlin.version"] as String
        println("KotlinVersion=[$kotlinVersion]")
        eachPlugin {
            if (requested.id.id == "org.jetbrains.compose") {
                val useVersion = if (extra.has("compose.version")) {
                    extra["compose.version"].toString()
                } else {
                    "0.0.0-SNAPSHOT"
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
