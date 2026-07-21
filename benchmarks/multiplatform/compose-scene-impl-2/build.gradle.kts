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
                api(project(":compose-scene-api"))
                implementation(compose.ui)
                implementation(compose.runtime)
            }
        }

        val androidMain by getting

        // Intermediate source set for all Skia/Skiko targets (non-Android)
        val skikoMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting { dependsOn(skikoMain) }
        val appleMain by getting { dependsOn(skikoMain) }
        val webMain by getting { dependsOn(skikoMain) }
    }
}

android {
    namespace = "org.jetbrains.compose.benchmarks.scene.impl2"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
}
