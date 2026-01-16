plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        compileSdk = 35
        namespace = "me.sample.app"
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
            implementation("me.sample.library:cmplib:1.0")
            implementation(project(":featureModule"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.ui:ui-test:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}
