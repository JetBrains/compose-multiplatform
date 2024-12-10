plugins {
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    id("com.android.application")
    kotlin("android")
}

group "com.example"
version "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.5.0")
}

android {
    compileSdk = 35
    namespace = "example.jetsnack"
    defaultConfig {
        applicationId = "org.jetbrains.Jetsnack"
        minSdk = 24
        targetSdk = 35
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
