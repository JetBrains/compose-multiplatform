@file:Suppress("OPT_IN_IS_NOT_ENABLED")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries {
            framework {
                baseName = "shared"
                isStatic = true
            }
        }
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    android {
        namespace = "org.jetbrains.chat.shared"
        compileSdk = 37
        minSdk = 26

        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.foundation)
            implementation(libs.material)
            implementation(libs.components.resources)
            implementation(libs.material.icons.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            api(libs.appcompat)
            api(libs.core.ktx)
        }
    }
}
