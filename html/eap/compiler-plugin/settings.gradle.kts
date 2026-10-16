/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.4.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    }
}

rootProject.name = "compose-html-compiler-plugin"
include(":sample")

// Compile the sample against this checkout's HTML runtime.
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("org.jetbrains.compose.html.eap:html-core-eap"))
            .using(project(":html-core-eap"))
    }
}
