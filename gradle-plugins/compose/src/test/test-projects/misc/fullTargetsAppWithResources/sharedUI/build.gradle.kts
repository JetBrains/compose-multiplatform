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
    androidLibrary {
        namespace = "org.company.app"
        compileSdk = 35
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
