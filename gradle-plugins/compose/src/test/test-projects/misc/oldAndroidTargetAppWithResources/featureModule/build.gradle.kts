plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
}

kotlin {
    jvm()

    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.material:material:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}
android {
    namespace = "me.sample.feature"
    compileSdk = 35
}

compose.resources {
    publicResClass = true
}