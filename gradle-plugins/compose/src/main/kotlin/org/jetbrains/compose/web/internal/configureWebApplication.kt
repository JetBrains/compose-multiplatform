/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.registerTask
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.compose.web.dsl.WebApplication
import org.jetbrains.compose.web.tasks.AbstractUnpackSkikoWasmRuntimeTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal fun configureWebApplication(project: Project, webExtension: WebExtension) {
    if (webExtension._isApplicationInitialized) {
        for (jsTarget in webExtension.targetsToConfigure(project)) {
            jsTarget.configureWebApplication(webExtension.application)
        }
    }
}

private fun KotlinJsIrTarget.configureWebApplication(app: WebApplication) {
    val mainCompilation = compilations.getByName("main")
    val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-wasm/$targetName")
    val taskName = "unpackSkikoWasmRuntime${targetName.capitalize()}"
    mainCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)
    val unpackRuntime = project.registerTask<AbstractUnpackSkikoWasmRuntimeTask>(taskName) {
        runtimeClasspath = project.configurations.getByName(mainCompilation.runtimeDependencyConfigurationName)
        outputDir.set(unpackedRuntimeDir)
    }
    mainCompilation.compileKotlinTaskProvider.configure { compileTask ->
        compileTask.dependsOn(unpackRuntime)
    }
}