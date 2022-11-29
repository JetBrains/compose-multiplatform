/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import java.io.File

data class ProcessRunResult(val exitCode: Int, val out: String, val err: String)

fun runProcess(
    tool: File,
    args: Collection<String>,
    checkExitCodeIsNormal: Boolean = true
): ProcessRunResult {
    val outFile = File.createTempFile("run-process-compose-tests-out.txt", null).apply { deleteOnExit() }
    val errFile = File.createTempFile("run-process-compose-tests-err.txt", null).apply { deleteOnExit() }
    return try {
        val cmd = arrayOf(tool.absolutePath, *args.toTypedArray())
        val process = ProcessBuilder().run {
            redirectError(errFile)
            redirectOutput(outFile)
            command(*cmd)
            start()
        }
        val exitCode = process.waitFor()
        if (checkExitCodeIsNormal) {
            check(exitCode == 0) {
                buildString {
                    appendLine("Non-zero exit code: $exitCode")
                    appendLine("Command: ${cmd.joinToString(", ")}")
                    appendLine("Out:")
                    outFile.forEachLine { line ->
                        appendLine("  >$line")
                    }
                    appendLine("Err:")
                    errFile.forEachLine { line ->
                        appendLine("  >$line")
                    }
                }
            }
        }
        ProcessRunResult(exitCode = exitCode, out = outFile.readText(), err = errFile.readText())
    } finally {
        outFile.delete()
        errFile.delete()
    }
}

val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
