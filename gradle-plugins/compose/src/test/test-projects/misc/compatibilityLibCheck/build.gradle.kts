import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = "org.company.app"
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        minSdk = 23
        androidResources.enable = true
    }

    jvm()

    js { browser() }
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.ui:ui:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.foundation:foundation:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}

allprojects {
    val subproject = this
    subproject.plugins.withType<NodeJsPlugin>().configureEach {
        subproject.extensions.getByType(NodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
    subproject.plugins.withType<WasmNodeJsPlugin>().configureEach {
        subproject.extensions.getByType(WasmNodeJsEnvSpec::class.java).version.set(libs.versions.node)
    }
}
