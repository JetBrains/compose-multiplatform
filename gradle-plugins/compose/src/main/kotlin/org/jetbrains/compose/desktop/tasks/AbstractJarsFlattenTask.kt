/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.utils.clearDirs
import java.io.File

abstract class AbstractJarsFlattenTask : AbstractComposeDesktopTask() {

    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:OutputDirectory
    val destinationDir: DirectoryProperty = objects.directoryProperty()

    @TaskAction
    fun execute() {
        fileOperations.clearDirs(destinationDir)

        fileOperations.copy {
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            it.from(flattenJars(inputFiles))
            it.into(destinationDir)
        }
    }

    private fun flattenJars(files: FileCollection) = files.map {
        when {
            it.isZipOrJar() -> this.archiveOperations.zipTree(it)
            else -> it
        }
    }

    private fun File.isZipOrJar() = name.endsWith(".jar", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
}