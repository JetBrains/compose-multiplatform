/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class ExternalToolRunner(
    private val verbose: Property<Boolean>,
    private val logsDir: Provider<Directory>,
    private val execOperations: ExecOperations
) {
    operator fun invoke(
        tool: File,
        args: Collection<String>,
        environment: Map<String, Any> = emptyMap(),
        workingDir: File? = null,
        checkExitCodeIsNormal: Boolean = true,
        processStdout: Function1<String, Unit>? = null,
        forceLogToFile: Boolean = false
    ): ExecResult {
        val logsDir = logsDir.ioFile
        logsDir.mkdirs()

        val toolName = tool.nameWithoutExtension
        val logToConsole = verbose.get() && !forceLogToFile
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
                appendLine("External tool execution failed:")
                val cmd = (listOf(tool.absolutePath) + args).joinToString(", ")
                appendLine("* Command: [$cmd]")
                appendLine("* Working dir: [${workingDir?.absolutePath.orEmpty()}]")
                appendLine("* Exit code: ${result.exitValue}")
                appendLine("* Standard output log: ${outFile.absolutePath}")
                appendLine("* Error log: ${errFile.absolutePath}")
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