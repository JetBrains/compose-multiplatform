/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils.*
import org.junit.jupiter.api.*
import java.net.ServerSocket
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread

internal class PreviewHostTest {
    private lateinit var serverSocket: ServerSocket

    @BeforeEach
    internal fun setUp() {
        serverSocket = ServerSocket(0, 0, localhost)
        serverSocket.soTimeout = 10.secondsAsMillis
    }

    @AfterEach
    internal fun tearDown() {
        serverSocket.close()
    }

    @Test
    fun connectNormallyAndStop() {
        withPreviewHostConnection { connection ->
            connection.receiveCommand { command ->
                check(command.type == Command.Type.ATTACH) {
                    "First received command is not ${Command.Type.ATTACH}: ${command.asString()}"
                }
            }
        }
    }

    private fun withPreviewHostConnection(doWithConnection: (RemoteConnection) -> Unit) {
        val isServerConnectionClosed = AtomicBoolean(false)
        val serverThread = thread {
            val socket = serverSocket.accept()
            val logger = TestLogger()
            val connection = RemoteConnectionImpl(socket, logger, onClose = {
                isServerConnectionClosed.set(true)
            })
            doWithConnection(connection)
            connection.close()
        }
        val serverThreadFailure = AtomicReference<Throwable>(null)
        serverThread.setUncaughtExceptionHandler { t, e ->
            serverThreadFailure.set(e)
        }

        val previewHostProcess = TestPreviewProcess(serverSocket.localPort)
        previewHostProcess.start()

        serverThread.join(10L.secondsAsMillis)
        val serverFailure = serverThreadFailure.get()
        check(serverFailure == null) { "Unexpected server failure: $serverFailure" }
        check(!serverThread.isAlive) { "Server thread should not be alive at this point" }
        check(isServerConnectionClosed.get()) { "Server connection was not closed" }

        previewHostProcess.finish()
    }
}
