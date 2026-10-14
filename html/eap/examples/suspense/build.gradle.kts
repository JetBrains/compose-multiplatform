/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val composeVersion: String = providers.gradleProperty("compose.version").get()
val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser { commonWebpackConfig { outputFileName = "app.js" } }
        binaries.executable()
    }
    wasmJs {
        browser { commonWebpackConfig { outputFileName = "app.js" } }
        binaries.executable()
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation(project(":html-core-eap"))
            implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$kotlinxBrowserCommonSubsetVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")
listOf("js", "wasmJs").forEach { target ->
    val webpack = tasks.named<KotlinWebpack>("${target}BrowserDevelopmentWebpack")
    tasks.register<JavaExec>("${target}RunSuspenseExample") {
        group = "application"
        description = "Serves progressive user HTML and the $target hydration bundle, then opens the browser."
        dependsOn(jvmMain.compileTaskProvider, webpack)
        mainClass.set("ServerKt")
        classpath(jvmMain.output.allOutputs)
        classpath(jvmMain.runtimeDependencyFiles)
        args(webpack.get().outputDirectory.get().asFile.absolutePath)
        args(providers.gradleProperty("suspense.port").getOrElse("8080"))
    }
}
