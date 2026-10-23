/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

pluginManagement {
    val kotlinVersion = java.util.Properties().apply {
        file("../../buildSrc/gradle.properties").inputStream().use(::load)
    }.getProperty("kotlin.version")
    includeBuild("gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
    }
}

rootProject.name = "compose-html-compiler-plugin"
include(":tests")

// Compile the tests against this checkout's HTML runtime.
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("org.jetbrains.compose.html.eap:html-core-eap"))
            .using(project(":html-core-eap"))
        substitute(module("org.jetbrains.compose.html.eap:html-svg-eap"))
            .using(project(":html-svg-eap"))
    }
}
