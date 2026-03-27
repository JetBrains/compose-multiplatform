plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 35
    namespace = "org.jetbrains.compose.resources.demo"
    defaultConfig {
        applicationId = "me.user.androidApp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        implementation(libs.compose.ui)
        implementation(libs.compose.foundation)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.activity.compose)
        implementation(project(":resources:demo:shared"))
    }
}
