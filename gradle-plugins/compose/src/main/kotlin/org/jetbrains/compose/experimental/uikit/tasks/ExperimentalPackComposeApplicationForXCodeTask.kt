/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class ExperimentalPackComposeApplicationForXCodeTask : DefaultTask() {
    @get:Input
    internal abstract val targetType: Property<UikitTarget>

    @get:Input
    internal abstract val buildType: Property<NativeBuildType>

    @get:InputFile
    internal abstract val kotlinBinary: RegularFileProperty

    @get:Input
    internal abstract val executablePath: Property<String>

    @get:OutputDirectory
    internal abstract val destinationDir: DirectoryProperty

    @TaskAction
    fun run() {
        val destinationDir = destinationDir.get().asFile
        project.delete(destinationDir)
        project.mkdir(destinationDir)

        val executableSource = kotlinBinary.get().asFile
        val dsymSource = File(executableSource.absolutePath + ".dSYM")

        val executableDestination = destinationDir.resolve(executablePath.get())
        val dsymDestination = File(executableDestination.parentFile.absolutePath + ".dSYM")

        for (sourceFile in dsymSource.walk().filter { it.isFile }) {
            val relativePath = sourceFile.relativeTo(dsymSource)
            val destFile = dsymDestination.resolve(relativePath)
            destFile.parentFile.mkdirs()
            if (sourceFile.name == executableSource.name) {
                sourceFile.copyTo(destFile.resolveSibling(executableDestination.name), true)
            } else {
                sourceFile.copyTo(destFile, true)
            }
        }

        executableDestination.parentFile.mkdirs()
        // We need to preserve executable flag for resulting executable, "FileKt.copyTo" extension method does not allow this.
        Files.copy(executableSource.toPath(), executableDestination.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
    }

    internal enum class UikitTarget(val simulator: Boolean, val targetName: String) {
        X64(true, "uikitX64"),
        Arm64(false, "uikitArm64")
    }
}