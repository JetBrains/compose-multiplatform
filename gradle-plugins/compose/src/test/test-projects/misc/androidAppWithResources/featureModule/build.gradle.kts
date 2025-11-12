plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(11)
    jvm()

    androidLibrary {
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
        namespace = "me.sample.feature"
        compileSdk = 35
        minSdk = 23
    }

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.material:material:COMPOSE_VERSION_PLACEHOLDER")
            api("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}

//https://youtrack.jetbrains.com/issue/CMP-8325
compose.desktop {
    application { }
}