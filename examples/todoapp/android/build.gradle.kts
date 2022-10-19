plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packagingOptions {
        exclude("META-INF/*")
    }
}

dependencies {
    implementation(project(":common:database"))
    implementation(project(":common:utils"))
    implementation(project(":common:root"))
    implementation(project(":common:compose-ui"))
    implementation(compose.material)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinLogging)
    implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinTimeTravel)
    implementation(Deps.ArkIvanov.Decompose.decompose)
    implementation(Deps.ArkIvanov.Decompose.extensionsCompose)
    implementation(Deps.AndroidX.AppCompat.appCompat)
    implementation(Deps.AndroidX.Activity.activityCompose)

    // Workaround for https://github.com/JetBrains/compose-jb/issues/2340
    implementation("androidx.compose.material:material:${Deps.JetpackComposeWorkaround.VERSION}")
}
