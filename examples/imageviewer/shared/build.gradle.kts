@file:Suppress("OPT_IN_IS_NOT_ENABLED")

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

version = "1.0-SNAPSHOT"

kotlin {
    androidTarget()
    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                //implementation(compose.materialIconsExtended) // TODO not working on iOS for now
                implementation("org.jetbrains.compose.components:components-resources:1.6.0-dev1275")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                // Kotlin Coroutines 1.7.1 contains Dispatchers.IO for iOS
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
                implementation("androidx.camera:camera-camera2:1.2.3")
                implementation("androidx.camera:camera-lifecycle:1.2.3")
                implementation("androidx.camera:camera-view:1.2.3")
                implementation("com.google.accompanist:accompanist-permissions:0.29.2-rc")
                implementation("com.google.android.gms:play-services-maps:18.1.0")
                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.maps.android:maps-compose:2.11.2")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(project(":mapview-desktop"))
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
    }
}

android {
    compileSdk = 34
    namespace = "example.imageviewer.shared"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
