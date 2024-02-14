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
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.activity.compose)
        implementation(project(":resources:demo:shared"))
    }
}
