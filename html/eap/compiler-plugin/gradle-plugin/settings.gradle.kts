/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

pluginManagement {
    val kotlinVersion = java.util.Properties().apply {
        file("../../../buildSrc/gradle.properties").inputStream().use(::load)
    }.getProperty("kotlin.version")
    plugins {
        kotlin("jvm") version kotlinVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "compose-html-lowering-gradle-plugin"
