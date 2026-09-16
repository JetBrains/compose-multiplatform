plugins {
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    id("com.android.application")
}

android {
    namespace = "me.sample.app"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }
    defaultConfig {
        applicationId = "org.example.project"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":featureModule"))
}
