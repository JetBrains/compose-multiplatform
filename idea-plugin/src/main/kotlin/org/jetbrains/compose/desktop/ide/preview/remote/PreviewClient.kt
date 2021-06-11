/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.remote

import com.intellij.openapi.Disposable
import java.io.BufferedWriter
import java.util.concurrent.TimeUnit

internal class PreviewClient(
    private val args: Array<String>
) : Disposable {
    private val process =
        ProcessBuilder(*args).run {
            start()
        }
    private var remoteWriter: BufferedWriter = process.outputStream.bufferedWriter()
    private var remoteState: PreviewState? = null

    @Synchronized
    fun syncState(newState: PreviewState) {
        val oldState = remoteState
        val newState = PreviewState(newState)
        if (oldState == null || oldState.viewPort != newState.viewPort) {
            val vp = newState.viewPort
            sendCommand("VIEW_PORT ${vp.x} ${vp.y} ${vp.width} ${vp.height}")
        }
        if (oldState == null || oldState.isPanelShown != newState.isPanelShown) {
            sendCommand("PANEL_SHOWN ${newState.isPanelShown}")
        }
        if (oldState == null || oldState.isIdeInFocus != newState.isIdeInFocus) {
            sendCommand("IDE_FOCUS ${newState.isIdeInFocus}")
        }
       if (oldState == null) {
           remoteState = newState
       }
    }

    @Synchronized
    private fun sendCommand(command: String) {
        remoteWriter.write(command)
        remoteWriter.write("\n")
        remoteWriter.flush()
    }

    override fun dispose() {
        remoteWriter.flush()
        remoteWriter.close()
        process.outputStream.close()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreviewClient

        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int =
        args.contentHashCode()
}