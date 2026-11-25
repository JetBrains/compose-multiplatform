plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    js { browser() }

    sourceSets.commonMain.dependencies {
        implementation(libs.compose.ui)
    }
}
