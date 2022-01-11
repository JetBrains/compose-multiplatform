/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.web.internal

import org.jetbrains.compose.experimental.dsl.ExperimentalWebApplication
import org.jetbrains.compose.internal.registerTask
import org.jetbrains.compose.experimental.web.tasks.ExperimentalUnpackSkikoWasmRuntimeTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal fun KotlinJsIrTarget.configureExperimentalWebApplication(app: ExperimentalWebApplication) {
    val mainCompilation = compilations.getByName("main")
    val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-wasm/$targetName")
    val taskName = "unpackSkikoWasmRuntime${targetName.capitalize()}"
    mainCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)
    val unpackRuntime = project.registerTask<ExperimentalUnpackSkikoWasmRuntimeTask>(taskName) {
        runtimeClasspath = project.configurations.getByName(mainCompilation.runtimeDependencyConfigurationName)
        outputDir.set(unpackedRuntimeDir)
    }
    mainCompilation.compileKotlinTaskProvider.configure { compileTask ->
        compileTask.dependsOn(unpackRuntime)
    }
}