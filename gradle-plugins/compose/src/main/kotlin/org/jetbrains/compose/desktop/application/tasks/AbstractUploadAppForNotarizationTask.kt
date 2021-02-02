package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.inject.Inject

abstract class AbstractUploadAppForNotarizationTask @Inject constructor(
    @get:Input
    val targetFormat: TargetFormat,
) : AbstractNotarizationTask() {
    @get:InputDirectory
    val inputDir: DirectoryProperty = objects.directoryProperty()

    @get:OutputFile
    val requestIDFile: RegularFileProperty = objects.fileProperty()

    init {
        check(targetFormat != TargetFormat.AppImage) { "${TargetFormat.AppImage} cannot be notarized!" }
    }

    @TaskAction
    fun run() {
        val notarization = validateNotarization()

        val inputFile = findOutputFileOrDir(inputDir.ioFile, targetFormat)
        val file = inputFile.checkExistingFile()

        logger.quiet("Uploading '${file.name}' for notarization (package id: '${notarization.bundleID}')")
        val (res, output) = ByteArrayOutputStream().use { baos ->
            PrintStream(baos).use { ps ->
                val res = execOperations.exec { exec ->
                    exec.executable = MacUtils.xcrun.absolutePath
                    exec.args(
                        "altool",
                        "--notarize-app",
                        "--primary-bundle-id", notarization.bundleID,
                        "--username", notarization.appleID,
                        "--password", notarization.password,
                        "--file", file
                    )
                    exec.standardOutput = ps
                }

                res to baos.toString()
            }
        }
        if (res.exitValue != 0) {
            logger.error("Uploading failed. Stdout: $output")
            res.assertNormalExitValue()
        }
        val m = "RequestUUID = ([A-Za-z0-9\\-]+)".toRegex().find(output)
            ?: error("Could not determine RequestUUID from output: $output")

        val requestId = m.groupValues[1]
        requestIDFile.ioFile.apply {
            parentFile.mkdirs()
            writeText(requestId)
        }

        logger.quiet("Request UUID: $requestId")
        logger.quiet("Request UUID is saved to ${requestIDFile.ioFile.absolutePath}")
    }
}
