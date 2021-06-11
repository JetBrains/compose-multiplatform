/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.compose.desktop.ide.preview.remote.PreviewClient
import org.jetbrains.compose.desktop.ide.preview.remote.PreviewProcessBuilder
import org.jetbrains.compose.desktop.ide.preview.remote.PreviewState
import org.jetbrains.compose.desktop.ide.preview.remote.PreviewStateUpdaterListener
import java.io.*
import java.net.InetAddress.getByName
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

@Service
class PreviewStateService(private val myProject: Project) : Disposable {
    private val isAlive = AtomicBoolean(true)
    private var preview: PreviewClient? = null

    // todo: handle ipv6
    private val gradleCallbackSocket =
        ServerSocket(0, 0, getByName("127.0.0.1"))
            .apply { reuseAddress = true }

    val gradleCallbackPort: Int
        get() = gradleCallbackSocket.localPort

    private val gradleCallbackThread = thread {
        while (isAlive.get()) {
            // todo: catch interrupted?
            gradleCallbackSocket.accept().getInputStream().use { ins ->
                ins.reader().buffered().use { reader ->
                    val javaHome = reader.readLine()
                    val serverCP = reader.readLine()
                    val target = reader.readLine()
                    val cp = reader.readLine()

                    synchronized(this@PreviewStateService) {
                        preview?.let { Disposer.dispose(it) }
                        preview = PreviewProcessBuilder(
                            File(javaHome),
                            serverCP,
                            target,
                            cp
                        ).start()

                        Disposer.register(this@PreviewStateService, preview!!)

                        // todo: Show after getting first screenshot
                        SwingUtilities.invokeLater {
                            ToolWindowManager.getInstance(myProject)
                                .getToolWindow("Desktop Preview")
                                ?.let { it.activate {} }
                        }
                    }
                }
            }
        }
    }

    private val idePreviewState = PreviewState()
    private val idePreviewStateUpdater = PreviewStateUpdaterListener(idePreviewState)

    private val stateSyncThread = thread {
        while (isAlive.get()) {
            try {
                Thread.sleep(16)
                preview?.syncState(idePreviewState)
            } catch (e: InterruptedException) {
            }
        }
    }

    override fun dispose() {
        isAlive.set(false)

        // todo: correct
        gradleCallbackThread.join(1000)
        if (gradleCallbackThread.isAlive) {
            gradleCallbackThread.interrupt()
        }
        gradleCallbackSocket.close()

        stateSyncThread.join(1000)
        if (stateSyncThread.isAlive) {
            stateSyncThread.interrupt()
        }
    }

    fun registerPreviewPanel(panel: JPanel) {
        val window = SwingUtilities.getWindowAncestor(panel)
        Disposer.register(this) {
            panel.removeAncestorListener(idePreviewStateUpdater)
            window.removeWindowFocusListener(idePreviewStateUpdater)
        }

        panel.addAncestorListener(idePreviewStateUpdater)
        idePreviewState.updatePreviewPanelState(panel)

        window.addWindowFocusListener(idePreviewStateUpdater)
        idePreviewState.updateIdeWindowState(window)
    }
}

