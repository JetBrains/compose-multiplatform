plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

group = "app.group"

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
                implementation("org.jetbrains.compose.material:material:COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
                implementation("org.jetbrains.compose.components:components-resources:COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER")
            }
        }
    }
}
