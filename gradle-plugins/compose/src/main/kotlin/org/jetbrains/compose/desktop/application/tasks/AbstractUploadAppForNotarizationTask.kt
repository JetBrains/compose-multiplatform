/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.files.checkExistingFile
import org.jetbrains.compose.desktop.application.internal.files.findOutputFileOrDir
import org.jetbrains.compose.internal.utils.MacUtils
import org.jetbrains.compose.internal.utils.ioFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

abstract class AbstractUploadAppForNotarizationTask @Inject constructor(
    @get:Input
    val targetFormat: TargetFormat
) : AbstractNotarizationTask() {
    @get:InputDirectory
    val inputDir: DirectoryProperty = objects.directoryProperty()

    @get:Internal
    val requestsDir: DirectoryProperty = objects.directoryProperty()

    init {
        check(targetFormat != TargetFormat.AppImage) { "${TargetFormat.AppImage} cannot be notarized!" }
    }

    @TaskAction
    fun run() {
        val notarization = validateNotarization()
        val packageFile = findOutputFileOrDir(inputDir.ioFile, targetFormat).checkExistingFile()

        logger.quiet("Uploading '${packageFile.name}' for notarization (package id: '${notarization.bundleID}')")
        val args = arrayListOf(
            "altool",
            "--notarize-app",
            "--primary-bundle-id", notarization.bundleID,
            "--username", notarization.appleID,
            "--password", notarization.password,
            "--file", packageFile.absolutePath
        )
        if (notarization.ascProvider != null) {
            args.add("--asc-provider")
            args.add(notarization.ascProvider)
        }

        runExternalTool(
            tool = MacUtils.xcrun,
            args = args,
            processStdout = { output ->
               processUploadToolOutput(packageFile, output)
            }
        )
    }

    private fun processUploadToolOutput(packageFile: File, output: String) {
        val m = "RequestUUID = ([A-Za-z0-9\\-]+)".toRegex().find(output)
            ?: error("Could not determine RequestUUID from output: $output")

        val requestId = m.groupValues[1]

        val uploadTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
        val requestDir = requestsDir.ioFile.resolve("$uploadTime-${targetFormat.id}")
        val packageCopy = requestDir.resolve(packageFile.name)
        packageFile.copyTo(packageCopy)
        val requestInfo = NotarizationRequestInfo(uuid = requestId, uploadTime = uploadTime)
        val requestInfoFile = requestDir.resolve(NOTARIZATION_REQUEST_INFO_FILE_NAME)
        requestInfo.saveTo(requestInfoFile)

        logger.quiet("Request UUID: $requestId")
        logger.quiet("Request UUID is saved to ${requestInfoFile.absolutePath}")
        logger.quiet("Uploaded file is saved to ${packageCopy.absolutePath}")
    }
}
