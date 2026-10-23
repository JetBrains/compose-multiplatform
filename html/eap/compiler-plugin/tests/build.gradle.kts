/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import java.util.Properties
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.html.lowering")
}

repositories {
    google()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

val compilerJar = rootProject.tasks.named<Jar>("jar").flatMap { it.archiveFile }

val htmlProperties = Properties().apply {
    rootProject.file("../../gradle.properties").inputStream().use(::load)
}
val composeVersion = htmlProperties.getProperty("compose.version")
val htmlVersion = htmlProperties.getProperty("compose.html.eap.version")
val browserVersion = htmlProperties.getProperty("compose.html.eap.kotlinx-browser-common-subset.version")
val kotlinVersion = Properties().apply {
    rootProject.file("../../buildSrc/gradle.properties").inputStream().use(::load)
}.getProperty("kotlin.version")

kotlin {
    jvmToolchain(21)
    jvm { compilerOptions { freeCompilerArgs.add("-Xverify-ir=error") } }
    js { browser { testTask { useKarma { useChromeHeadless() } } } }
    wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.html.eap:html-core-eap:$htmlVersion")
            implementation("org.jetbrains.compose.html.eap:html-svg-eap:$htmlVersion")
            implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$browserVersion")
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
            implementation("org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:$kotlinVersion")
        }
        val webTest by getting {
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC") }
        }
    }
}

// The comparison suites compile their own samples with lowering both enabled and disabled.
tasks.withType<KotlinJvmTest>().configureEach {
    inputs.file(compilerJar)
    doFirst { systemProperty("htmlLowering.pluginJar", compilerJar.get().asFile.absolutePath) }
}

val jvmTest = kotlin.targets.getByName("jvm").compilations.getByName("test")
val generateHydrationFixture = tasks.register<JavaExec>("generateHydrationFixture") {
    val generatedSources = layout.buildDirectory.dir("generated/hydration")
    mainClass.set("org.jetbrains.compose.html.compiler.GenerateFixtureKt")
    classpath(jvmTest.output.allOutputs, jvmTest.runtimeDependencyFiles)
    args(generatedSources.get().asFile.absolutePath)
    outputs.dir(generatedSources)
}
kotlin.sourceSets.named("webTest") { kotlin.srcDir(generateHydrationFixture) }
