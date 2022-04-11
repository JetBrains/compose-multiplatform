import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("multiplatform-setup")
    id("android-setup")
}

kotlin {
    fun isIphoneSimulatorBuild(): Boolean =
        System.getenv("NATIVE_ARCH") == "arm64" && System.getenv("SDK_NAME")?.startsWith("iphonesimulator") == true

    fun isIphoneOsBuild(): Boolean =
        System.getenv("SDK_NAME")?.startsWith("iphoneos") == true

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget =
        when {
            isIphoneSimulatorBuild() -> ::iosSimulatorArm64
            isIphoneOsBuild() -> ::iosArm64
            else -> ::iosX64
        }

    iosTarget("ios") {
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(Deps.ArkIvanov.MVIKotlin.rx)
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
                implementation(Deps.ArkIvanov.Decompose.decompose)
                implementation(Deps.Badoo.Reaktive.reaktive)
            }
        }
    }
}
