/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils

import java.io.File
import java.util.concurrent.TimeUnit

internal fun runJava(
    headless: Boolean = true,
    debugPort: Int? = null,
    classpath: String = "",
    args: List<String> = emptyList()
): ProcessBuilder {
    val javaExec = javaToolPath("java")
    val cmd = arrayListOf(
        javaExec,
        "-classpath",
        classpath
    )
    if (headless) {
        cmd.add("-Djava.awt.headless=true")
    }
    if (debugPort != null) {
        cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
    }
    cmd.addAll(args)
    println("Starting process: [${cmd.joinToString(",") { "\n  $it" } }\n]")
    return ProcessBuilder(cmd).apply {
        redirectError(ProcessBuilder.Redirect.INHERIT)
        redirectOutput(ProcessBuilder.Redirect.INHERIT)
    }
}

internal fun runJStackAndGetOutput(
    pid: Long
): String {
    val jstack = javaToolPath("jstack")
    val stdoutFile = File.createTempFile("jstack-stdout", ".txt").apply { deleteOnExit() }
    val stderrFile = File.createTempFile("jstack-stderr", ".txt").apply { deleteOnExit() }

    try {
        val process = ProcessBuilder(jstack, pid.toString()).apply {
            redirectOutput(stdoutFile)
            redirectError(stderrFile)
        }.start()
        process.waitFor(10, TimeUnit.SECONDS)
        if (process.isAlive) {
            process.destroyForcibly()
            error("jstack did not finish")
        }
        val exitCode = process.exitValue()
        check(exitCode == 0) {
            buildString {
                appendLine("jstack finished with error: $exitCode")
                appendLine("  output:")
                stdoutFile.readLines().forEach {
                    appendLine("  >")
                }
                appendLine("  err:")
                stderrFile.readLines().forEach {
                    appendLine("  >")
                }
            }
            " $exitCode\n${stderrFile.readText()}"
        }
        return stdoutFile.readText()
    } finally {
        stdoutFile.delete()
        stderrFile.delete()
    }
}

private fun javaToolPath(toolName: String): String {
    val javaHome = File(systemProperty("java.home"))
    val toolExecutableName = if (isWindows) "$toolName.exe" else toolName
    val executable = javaHome.resolve("bin/$toolExecutableName")
    check(executable.isFile) { "Could not find tool '$toolName' at specified path: $executable" }
    return executable.absolutePath
}