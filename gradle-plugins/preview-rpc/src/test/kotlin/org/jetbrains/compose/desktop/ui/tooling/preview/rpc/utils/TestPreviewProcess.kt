/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils

import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PREVIEW_HOST_CLASS_NAME
import java.util.concurrent.TimeUnit

class TestPreviewProcess(private val port: Int) {
    private var myProcess: Process? = null

    fun start() {
        if (myProcess != null) error("Process was started already")

        myProcess = runJava(
            headless = true,
            classpath = previewTestClaspath,
            args = listOf(PREVIEW_HOST_CLASS_NAME, port.toString())
        ).start()
    }

    fun finish() {
        val process = myProcess
        check(process != null) { "Process was not started" }

        process.waitFor(10, TimeUnit.SECONDS)
        if (process.isAlive) {
            val jstackOutput = runJStackAndGetOutput(process.pid())
            val message = buildString {
                appendLine("Preview host process did not stop:")
                jstackOutput.splitToSequence("\n").forEach {
                    appendLine("> $it")
                }
            }
            process.destroyForcibly()
            error(message)
        }
        val exitCode = process.exitValue()
        check(exitCode == 0) { "Non-zero exit code: $exitCode" }
    }
}