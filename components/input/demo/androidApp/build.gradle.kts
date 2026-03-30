/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 35
    namespace = "org.jetbrains.compose.components.input.demo"
    defaultConfig {
        applicationId = "org.jetbrains.compose.components.input.demo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        implementation(libs.compose.ui)
        implementation(libs.compose.foundation)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.activity.compose)
        implementation(project(":input:demo:shared"))
    }
}

