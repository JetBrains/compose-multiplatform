plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    compileSdk = 34
    namespace = "org.jetbrains.compose.resources.demo"
    defaultConfig {
        applicationId = "me.user.androidApp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        implementation(project(":resources:demo:shared"))
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("androidx.activity:activity-compose:1.8.0")
        implementation("androidx.compose.foundation:foundation:1.5.3")
        implementation("androidx.compose.ui:ui:1.5.3")
    }
}
