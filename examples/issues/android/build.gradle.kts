plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    compileSdk = 34
    namespace = "org.jetbrains.issues"

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        applicationId = "org.jetbrains.issues.Issues"
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(project(":common"))
}
