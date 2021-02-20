package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.alsoOutputTo
import org.jetbrains.compose.desktop.application.internal.ioFile
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

abstract class AbstractComposeDesktopTask : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val fileOperations: FileOperations

    @get:LocalState
    protected val logsDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/logs/$name")

    @get:Internal
    val verbose: Property<Boolean> = objects.notNullProperty<Boolean>().apply {
        set(providers.provider {
            logger.isDebugEnabled || ComposeProperties.isVerbose(providers).get()
        })
    }

    internal fun runExternalTool(
        tool: File,
        args: Collection<String>,
        environment: Map<String, Any> = emptyMap(),
        workingDir: File? = null,
        checkExitCodeIsNormal: Boolean = true,
        processStdout: Function1<String, Unit>? = null
    ): ExecResult {
        val logsDir = logsDir.ioFile
        logsDir.mkdirs()

        val toolName = tool.nameWithoutExtension
        val logToConsole = verbose.get()
        val outFile = logsDir.resolve("${toolName}-${currentTimeStamp()}-out.txt")
        val errFile = logsDir.resolve("${toolName}-${currentTimeStamp()}-err.txt")

        val result = outFile.outputStream().buffered().use { outFileStream ->
            errFile.outputStream().buffered().use { errFileStream ->
                execOperations.exec { spec ->
                    spec.executable = tool.absolutePath
                    spec.args(*args.toTypedArray())
                    workingDir?.let { wd -> spec.workingDir(wd) }
                    spec.environment(environment)
                    // check exit value later
                    spec.isIgnoreExitValue = true

                    if (logToConsole) {
                        spec.standardOutput = spec.standardOutput.alsoOutputTo(outFileStream)
                        spec.errorOutput = spec.errorOutput.alsoOutputTo(errFileStream)
                    } else {
                        spec.standardOutput = outFileStream
                        spec.errorOutput = errFileStream
                    }
                }
            }
        }

        if (checkExitCodeIsNormal && result.exitValue != 0) {
            val errMsg = buildString {
                appendln("External tool execution failed:")
                val cmd = (listOf(tool.absolutePath) + args).joinToString(", ")
                appendln("* Command: [$cmd]")
                appendln("* Working dir: [${workingDir?.absolutePath.orEmpty()}]")
                appendln("* Exit code: ${result.exitValue}")
                appendln("* Standard output log: ${outFile.absolutePath}")
                appendln("* Error log: ${errFile.absolutePath}")
            }

            error(errMsg)
        }

        if (processStdout != null) {
            processStdout(outFile.readText())
        }

        if (result.exitValue == 0) {
            outFile.delete()
            errFile.delete()
        }

        return result
    }

    private fun currentTimeStamp() =
        LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
}