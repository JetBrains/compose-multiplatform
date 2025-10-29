plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(17)
    jvm()

    androidLibrary {
        namespace = "me.sample.feature"
        compileSdk = 35
        minSdk = 24
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.material3)
            api(compose.components.resources)
        }
    }
}

//https://youtrack.jetbrains.com/issue/CMP-8325
compose.desktop {
    application { }
}

compose.resources {
    publicResClass = true
}