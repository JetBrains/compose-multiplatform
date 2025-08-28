import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting

        androidMain.dependencies {
            api(libs.androidx.ui.tooling.preview)
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain)
        }

        val appleMain by getting
        appleMain.dependsOn(nonAndroidMain)

        val desktopMain by getting
        desktopMain.dependsOn(nonAndroidMain)

        val jsMain by getting
        jsMain.dependsOn(nonAndroidMain)

        val wasmJsMain by getting
        wasmJsMain.dependsOn(nonAndroidMain)
    }
}

android {
    compileSdk = 35
    namespace = "org.jetbrains.compose.ui.tooling.preview"
    defaultConfig {
        minSdk = 23
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
