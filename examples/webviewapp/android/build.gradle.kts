plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}


dependencies {
    implementation(compose.material)
    implementation(project(":webview"))
    implementation(Deps.AndroidX.AppCompat.appCompat)
    implementation(Deps.AndroidX.Activity.activityCompose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}

android {
    compileSdk= (31)
    defaultConfig {
        applicationId = "com.example.android"
        minSdk = (24)
        targetSdk = (31)
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}