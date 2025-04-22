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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(compose.components.resources)
            }
        }
    }
}
