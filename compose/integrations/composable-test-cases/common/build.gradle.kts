plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    configureTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2-wasm0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0-wasm1")
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }
    }
}
