package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.ioFile
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import javax.inject.Inject

abstract class AbstractCheckNotarizationStatusTask : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory
    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Input
    val username: Property<String?> = objects.nullableProperty()

    @get:Input
    val password: Property<String?> = objects.nullableProperty()

    @get:InputFile
    val requestIDFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun run() {
        val requestId = requestIDFile.ioFile.readText()
        execOperations.exec { exec ->
            exec.executable = MacUtils.xcrun.absolutePath
            exec.args(
                "altool",
                "--notarization-info", requestId,
                "--username", username.get(),
                "--password", password.get()
            )
        }
    }
}