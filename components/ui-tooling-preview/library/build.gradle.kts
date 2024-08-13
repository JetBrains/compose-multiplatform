import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.android.library")
}

kotlin {
    jvm("desktop")
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js {
        browser {
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
        }
    }
    macosX64()
    macosArm64()
}

android {
    compileSdk = 34
    namespace = "org.jetbrains.compose.ui.tooling.preview"
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-ui-tooling-preview",
    name = "Experimental Compose Multiplatform tooling library API. This library provides the API required to declare " +
            "@Preview composables in user apps."
)

afterEvaluate {
    // TODO(o.k.): remove this after we refactor jsAndWasmMain source set in skiko to get rid of broken "common" js-interop
    tasks.configureEach {
        if (name == "compileWebMainKotlinMetadata") enabled = false
    }
}
