/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.utils.clearDirs
import java.io.File
import javax.inject.Inject

abstract class UnpackSkikoWasmRuntimeTask : DefaultTask() {
    @get:InputFiles
    lateinit var skikoRuntimeFiles: FileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    internal abstract val archiveOperations: ArchiveOperations

    @get:Inject
    internal abstract val fileOperations: FileSystemOperations

    @TaskAction
    fun run() {
        fileOperations.clearDirs(outputDir)

        for (file in skikoRuntimeFiles.files) {
            if (file.name.endsWith(".jar", ignoreCase = true)) {
                unpackJar(file)
            }
        }
    }

    private fun unpackJar(file: File) {
        fileOperations.copy { copySpec ->
            copySpec.from(archiveOperations.zipTree(file))
            copySpec.into(outputDir)
        }
    }
}