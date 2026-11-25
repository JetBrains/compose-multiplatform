plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        namespace = "me.sample.feature"
        minSdk = 23
        androidResources.enable = true
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}

// TODO https://youtrack.jetbrains.com/issue/CMP-11110 Remove compromised root certificate workaround after updating to Kotlin 2.5.0
// Override the node version to fix https://youtrack.jetbrains.com/issue/CMP-11011
// Workaround is from https://youtrack.jetbrains.com/issue/KT-90732
// Configured per project, as `allprojects` is not compatible with Gradle isolated projects
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin>().configureEach {
    extensions.getByType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec::class.java).version.set("26.12.1")
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin>().configureEach {
    extensions.getByType(org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec::class.java).version.set("26.12.1")
}
