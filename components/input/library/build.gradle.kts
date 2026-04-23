/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
    macosArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
            }
        }
        val commonMain by getting {
            dependencies {
                // Expose foundation, runtime, and ui to consumers of this library
                api(libs.compose.foundation)
                api(libs.compose.runtime)
                api(libs.compose.ui)
            }
        }

        // Keep their exact source set hierarchy for CI compatibility
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

        // Ensure tests have access to the UI testing frameworks
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "org.jetbrains.compose.components.input"
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
    artifactId = "components-input",
    name = "Experimental Compose Multiplatform input library API. This library provides advanced hardware-aware pointer click modifiers."
)