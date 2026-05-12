import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}

android {
    compileSdk = 37
    namespace = "org.jetbrains.nav_cupcake"
    defaultConfig {
        applicationId = "org.jetbrains.nav_cupcake"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
