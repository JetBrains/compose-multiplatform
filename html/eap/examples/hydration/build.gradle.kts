/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.JavaExec

val renderedSite = layout.buildDirectory.dir("generated/site")
val renderedIndex = renderedSite.map { it.file("index.html") }
val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val composeVersion: String = providers.gradleProperty("compose.version").get()
val usePublished = rootProject.extra["compose.html.eap.examples.usePublished"] as Boolean
val htmlVersion = rootProject.extra["compose.html.eap.examples.version"] as String

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }
    wasmJs {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("androidx.compose.runtime:runtime:$composeVersion")
                implementation(
                    if (usePublished) "org.jetbrains.compose.html.eap:html-core-eap:$htmlVersion"
                    else project(":html-core-eap")
                )
                implementation(
                    "org.jetbrains.compose.html:kotlinx-browser-common-subset:" +
                        kotlinxBrowserCommonSubsetVersion
                )
            }
        }
        val webMain by getting {
            resources.srcDir(renderedSite)
        }
    }
}

val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")
val renderHydrationExample = tasks.register<JavaExec>("renderHydrationExample") {
    group = "application"
    description = "Renders the server HTML used by the hydration example."
    dependsOn(jvmMain.compileTaskProvider)
    mainClass.set("ServerKt")
    classpath(jvmMain.output.allOutputs)
    classpath(jvmMain.runtimeDependencyFiles)
    args(renderedIndex.get().asFile.absolutePath)
    outputs.file(renderedIndex)
}

listOf("js", "wasmJs", "metadataWebMain").forEach { targetName ->
    tasks.matching { it.name == "${targetName}ProcessResources" }.configureEach {
        dependsOn(renderHydrationExample)
    }
}
