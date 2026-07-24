plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.github.gmazzo.buildconfig")
}

group = "app.group"

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
                implementation("org.jetbrains.compose.material:material:COMPOSE_VERSION_PLACEHOLDER")
                implementation("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
            }
        }
    }
}

buildConfig {
    buildConfigField(String::class.java, "str", "")
}
