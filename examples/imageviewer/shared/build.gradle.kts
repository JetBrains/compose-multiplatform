import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop")
    js {
        browser()
        useEsModules()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    android {
        namespace = "example.imageviewer.shared"
        compileSdk = 37
        minSdk = 26

        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    compilerOptions {
        freeCompilerArgs = listOf("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.components.resources)
            implementation(libs.material.icons.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            api(libs.appcompat)
            api(libs.core.ktx)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.accompanist.permissions)
            implementation(libs.play.services.maps)
            implementation(libs.play.services.location)
            implementation(libs.maps.compose)
        }

        webMain.dependencies {
            implementation(npm("uuid", "^9.0.1"))
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(projects.mapviewDesktop)
        }
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test.junit4)
        }
    }
}
