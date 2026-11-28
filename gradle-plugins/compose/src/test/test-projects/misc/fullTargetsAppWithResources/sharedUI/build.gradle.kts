import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

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
            version = release(37) { minorApiLevel = 2 }
        }
        minSdk = 24
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
            api("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
        }
    }

    val xcf = XCFramework("SharedUI")
    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "SharedUI"
                    xcf.add(this)
                }
            }
        }
}

compose.resources {
    publicResClass = true
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
