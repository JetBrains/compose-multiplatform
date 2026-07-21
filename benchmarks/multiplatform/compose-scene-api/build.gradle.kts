plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvm("desktop")

    androidTarget()

    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    js { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.runtime)
            }
        }
    }
}

android {
    namespace = "org.jetbrains.compose.benchmarks.scene.api"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
}
