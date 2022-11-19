plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    android()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":resources:demo-shared"))
                implementation("androidx.appcompat:appcompat:1.5.1")
                implementation("androidx.activity:activity-compose:1.5.0")
            }
        }
    }
}

android {
    compileSdkVersion(32)
    defaultConfig {
        applicationId = "me.user.androidApp"
        minSdkVersion(24)
        targetSdkVersion(32)
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}