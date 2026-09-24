plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

dependencies {
    implementation(project(":benchmarks"))
    implementation(libs.activity.compose)
}

android {
    namespace = "org.jetbrains.compose.benchmarks"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.jetbrains.compose.benchmarks"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            pickFirsts += "META-INF/androidx/annotation/annotation/LICENSE.txt"
        }
    }
}
