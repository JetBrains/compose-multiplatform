package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.NOTARIZATION_REQUEST_INFO_FILE_NAME
import org.jetbrains.compose.desktop.application.internal.NotarizationRequestInfo
import org.jetbrains.compose.desktop.application.internal.ioFile

abstract class AbstractCheckNotarizationStatusTask : AbstractNotarizationTask() {
    @get:Internal
    val requestDir: DirectoryProperty = objects.directoryProperty()

    @TaskAction
    fun run() {
        val notarization = validateNotarization()

        val requests = HashSet<NotarizationRequestInfo>()
        for (file in requestDir.ioFile.walk()) {
            if (file.isFile && file.name == NOTARIZATION_REQUEST_INFO_FILE_NAME) {
                try {
                    val status = NotarizationRequestInfo()
                    status.loadFrom(file)
                    requests.add(status)
                } catch (e: Exception) {
                    logger.error("Invalid notarization request status file: $file", e)
                }
            }
        }

        if (requests.isEmpty()) {
            logger.quiet("No existing notarization requests")
            return
        }

        for (request in requests.sortedBy { it.uploadTime }) {
            try {
                logger.quiet("Checking status of notarization request '${request.uuid}'")
                runExternalTool(
                    tool = MacUtils.xcrun,
                    args = listOf(
                        "altool",
                        "--notarization-info", request.uuid,
                        "--username", notarization.appleID,
                        "--password", notarization.password
                    ),
                    processStdout = { output ->
                        if (!verbose.get()) {
                            logger.quiet(output)
                        }
                    }
                )
            } catch (e: Exception) {
                logger.error("Could not check notarization request '${request.uuid}'", e)
            }
        }
    }
}