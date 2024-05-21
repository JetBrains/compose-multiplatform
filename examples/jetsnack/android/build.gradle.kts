plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("multiplatform")
    kotlin("plugin.compose")
}

group "com.example"
version "1.0-SNAPSHOT"

kotlin {
    androidTarget()
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.5.0")
}

android {
    compileSdk = 34
    namespace = "com.example.android"
    defaultConfig {
        applicationId = "com.example.android"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
