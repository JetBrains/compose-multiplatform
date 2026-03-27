plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("native.cocoapods")
    id("org.jetbrains.compose")
}

kotlin {
    cocoapods {
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        pod("Base64", "1.1.2")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    iosArm64()
    iosSimulatorArm64()

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
