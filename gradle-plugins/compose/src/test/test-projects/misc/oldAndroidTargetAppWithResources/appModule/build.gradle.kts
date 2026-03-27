plugins {
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    id("com.android.application")
}

android {
    namespace = "me.sample.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.example.project"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":featureModule"))
}
