package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.ioFile

abstract class AbstractCheckNotarizationStatusTask : AbstractNotarizationTask() {
    @get:InputFile
    val requestIDFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun run() {
        val notarization = validateNotarization()

        val requestId = requestIDFile.ioFile.readText()
        execOperations.exec { exec ->
            exec.executable = MacUtils.xcrun.absolutePath
            exec.args(
                "altool",
                "--notarization-info", requestId,
                "--username", notarization.appleID,
                "--password", notarization.password
            )
        }
    }
}