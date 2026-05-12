@file:Suppress("OPT_IN_IS_NOT_ENABLED")

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
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    android {
        compileSdk = 37
        namespace = "org.jetbrains.codeviewer.common"
        minSdk = 26

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.material.icons.extended)
            implementation(libs.components.resources)
        }
        androidMain {
            kotlin {
                srcDirs("src/jvmMain/kotlin")
            }
            dependencies {
                api(libs.androidx.activity.compose)
                api(libs.appcompat)
                api(libs.core.ktx)
            }
        }
        val desktopMain by getting {
            kotlin {
                srcDirs("src/jvmMain/kotlin")
            }
        }
    }
}
