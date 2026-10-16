/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add(
            "-Xcompiler-plugin-order=" +
                "org.jetbrains.compose.html.lowering>androidx.compose.compiler.plugins.kotlin"
        )
        freeCompilerArgs.add("-Xverify-ir=error")
    }
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:1.12.0")
    implementation("org.jetbrains.compose.html.eap:html-core-eap:0.0.1")
    implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:0.0.1+dev2")
    testImplementation(kotlin("test-junit"))
    add("kotlinCompilerPluginClasspath", project(":"))
}
