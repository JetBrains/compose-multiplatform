plugins {
    val kotlinVersion = "1.6.10"
    val composeMultiplatformVersion = "1.1.0-beta04"
    val mobileMultiplatformVersion = "0.12.0"

    id("com.android.library") apply false
    id("dev.icerock.mobile.multiplatform.android-manifest") version mobileMultiplatformVersion apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    id("org.jetbrains.compose") version composeMultiplatformVersion apply false
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "16.0.0"
}
