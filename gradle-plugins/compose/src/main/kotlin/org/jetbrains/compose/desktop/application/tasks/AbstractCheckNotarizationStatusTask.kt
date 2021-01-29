package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.ioFile
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import org.jetbrains.compose.desktop.application.internal.validateNotarizationSettings
import javax.inject.Inject

abstract class AbstractCheckNotarizationStatusTask : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory
    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:InputFile
    val requestIDFile: RegularFileProperty = objects.fileProperty()

    @get:Nested
    @get:Optional
    val notarizationSettings: Property<MacOSNotarizationSettings?> = objects.nullableProperty()

    @TaskAction
    fun run() {
        val requestId = requestIDFile.ioFile.readText()
        val notarization = validateNotarizationSettings(notarizationSettings)
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