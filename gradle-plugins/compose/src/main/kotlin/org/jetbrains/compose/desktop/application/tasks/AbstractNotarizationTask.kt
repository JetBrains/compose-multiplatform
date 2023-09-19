/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.files.checkExistingFile
import org.jetbrains.compose.desktop.application.internal.files.findOutputFileOrDir
import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.validation.validate
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.MacUtils
import org.jetbrains.compose.internal.utils.ioFile
import java.io.File
import javax.inject.Inject

abstract class AbstractNotarizationTask @Inject constructor(
    @get:Input
    val targetFormat: TargetFormat
) : AbstractComposeDesktopTask() {

    @get:Nested
    @get:Optional
    internal var nonValidatedNotarizationSettings: MacOSNotarizationSettings? = null

    @get:InputDirectory
    val inputDir: DirectoryProperty = objects.directoryProperty()

    init {
        check(targetFormat != TargetFormat.AppImage) { "${TargetFormat.AppImage} cannot be notarized!" }
    }

    @TaskAction
    fun run() {
        val notarization = nonValidatedNotarizationSettings.validate()
        val packageFile = findOutputFileOrDir(inputDir.ioFile, targetFormat).checkExistingFile()

        notarize(notarization, packageFile)
        staple(packageFile)
    }

    private fun notarize(
        notarization: ValidatedMacOSNotarizationSettings,
        packageFile: File
    ) {
        logger.info("Uploading '${packageFile.name}' for notarization")
        val args = listOfNotNull(
            "notarytool",
            "submit",
            "--wait",
            "--apple-id",
            notarization.appleID,
            "--team-id",
            notarization.teamID,
            packageFile.absolutePath
        )
        runExternalTool(tool = MacUtils.xcrun, args = args, stdinStr = notarization.password)
    }

    private fun staple(packageFile: File) {
        runExternalTool(
            tool = MacUtils.xcrun,
            args = listOf("stapler", "staple", packageFile.absolutePath)
        )
    }
}
