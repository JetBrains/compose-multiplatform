/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.html.lowering")
}

val compilerProject = project(":compose-html-compiler-plugin")
val compilerJar = compilerProject.tasks.named<Jar>("jar").flatMap { it.archiveFile }
val compilerPluginClasspath = files(compilerJar, compilerProject.configurations.named("runtimeClasspath"))

val composeVersion = providers.gradleProperty("compose.version").get()
val browserVersion = providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()

kotlin {
    jvm { compilerOptions { freeCompilerArgs.add("-Xverify-ir=error") } }
    js { browser { testTask { useKarma { useChromeHeadless() } } } }
    wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation(project(":html-core-eap"))
            implementation(project(":html-svg-eap"))
            implementation("org.jetbrains.compose.html:kotlinx-browser-common-subset:$browserVersion")
        }
        jvmTest.dependencies {
            implementation(kotlin("compiler-embeddable"))
            implementation(kotlin("compose-compiler-plugin-embeddable"))
        }
        val webTest by getting {
            dependencies { implementation(libs.kotlinx.coroutines.core) }
        }
    }
}

// The comparison suites compile their own samples with lowering both enabled and disabled.
tasks.withType<KotlinJvmTest>().configureEach {
    jvmArgs("-Xss2m") // needed for nested tests to pass on workflow
    inputs.files(compilerPluginClasspath)
    doFirst {
        systemProperty(
            "htmlLowering.pluginClasspath",
            compilerPluginClasspath.files.joinToString(",") { it.absolutePath })
    }
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
