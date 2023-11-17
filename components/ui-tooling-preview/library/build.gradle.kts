import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.android.library")
}

val composeVersion = extra["compose.version"] as String

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()
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
            testTask(Action {
                enabled = false
            })
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
    artifactId = "ui-tooling-preview",
    name = "Experimental Compose Multiplatform tooling library API. This library provides the API required to declare " +
            "@Preview composables in user apps."
)
