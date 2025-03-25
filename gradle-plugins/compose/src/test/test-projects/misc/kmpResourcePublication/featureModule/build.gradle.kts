plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
}

kotlin {
    androidTarget()
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.components.resources)
        }
    }
}

android {
    namespace = "me.sample.feature"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}