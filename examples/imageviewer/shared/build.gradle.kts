@file:Suppress("OPT_IN_IS_NOT_ENABLED")

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

version = "1.0-SNAPSHOT"

kotlin {
    android()
    jvm("desktop")
    ios()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared code for the sample"
        homepage = "https://github.com/JetBrains/compose-jb"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
        extraSpecAttributes["resources"] =
            "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                //implementation(compose.materialIconsExtended) // TODO not working on iOS for now
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.0")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.9.0")
                implementation("androidx.camera:camera-camera2:1.2.2")
                implementation("androidx.camera:camera-lifecycle:1.2.2")
                implementation("androidx.camera:camera-view:1.2.2")
                implementation("com.google.accompanist:accompanist-permissions:0.29.2-rc")
                implementation("com.google.android.gms:play-services-maps:18.1.0")
                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.maps.android:maps-compose:2.11.2")
            }
        }
        val iosMain by getting {
            dependencies {
                // Kotlin Coroutines 1.7.0 contains Dispatchers.IO
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }


        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(project(":mapview-desktop"))
            }
        }
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
