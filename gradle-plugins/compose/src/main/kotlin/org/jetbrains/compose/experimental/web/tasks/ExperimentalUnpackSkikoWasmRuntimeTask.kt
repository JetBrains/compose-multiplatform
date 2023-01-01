/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.web.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.debug

abstract class ExperimentalUnpackSkikoWasmRuntimeTask @Inject constructor(
    val files: FileSystemOperations,
    val archives: ArchiveOperations,
): DefaultTask() {
    @get:InputFiles
    lateinit var runtimeClasspath: Configuration

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val runtimeArtifacts = runtimeClasspath.resolvedConfiguration.resolvedArtifacts
        for (artifact in runtimeArtifacts) {
            logger.debug { "Checking artifact: id=${artifact.id}, file=${artifact.file}" }
            val id = artifact.id.componentIdentifier
            if (id is ModuleComponentIdentifier && id.group == "org.jetbrains.skiko") {
                logger.debug { "Found skiko artifact: $artifact" }
                unpackSkikoRuntime(id.version)
            }
        }
    }

    private fun unpackSkikoRuntime(skikoVersion: String) {
        val skikoRuntimeConfig = project.configurations.detachedConfiguration(
            project.dependencies.create("org.jetbrains.skiko:skiko-js-wasm-runtime:$skikoVersion")
        )

        for (file in skikoRuntimeConfig.resolve()) {
            if (file.name.endsWith(".jar", ignoreCase = true)) {
                files.sync { copySpec ->
                    copySpec.from(archives.zipTree(file))
                    copySpec.into(outputDir)
                }
            }
        }
    }
}
