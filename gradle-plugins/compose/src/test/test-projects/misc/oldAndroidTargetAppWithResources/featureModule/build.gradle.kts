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
            api(compose.runtime)
            api(compose.material3)
            api(compose.components.resources)
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