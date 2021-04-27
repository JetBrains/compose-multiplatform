/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.awt.Dimension
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class PreviewHostConfig(
    val javaExecutable: String,
    val hostClasspath: String
)

data class FrameRequest(
    val composableFqName: String,
    val width: Int,
    val height: Int
)

interface PreviewManager {
    val gradleCallbackPort: Int
    fun updateFrameSize(width: Int, height: Int)
    fun close()
}

private data class RunningPreview(
    val connection: RemoteConnection,
    val process: Process
) {
    val isAlive: Boolean
        get() = connection.isAlive && process.isAlive
}

class PreviewManagerImpl(private val onNewFrame: (ByteArray) -> Unit) : PreviewManager {
    private val log = PrintStreamLogger("SERVER")
    private val previewSocket = newServerSocket()
    private val gradleCallbackSocket = newServerSocket()
    private val connectionNumber = AtomicLong(0)
    private val isAlive = AtomicBoolean(true)

    // todo: restart when configuration changes
    private val previewHostConfig = AtomicReference<PreviewHostConfig>(null)
    private val previewClasspath = AtomicReference<String>(null)
    private val previewFqName = AtomicReference<String>(null)
    private val previewFrameSize = AtomicReference<Dimension>(null)
    private val frameRequest = AtomicReference<FrameRequest>(null)
    private val shouldRequestFrame = AtomicBoolean(false)
    private val runningPreview = AtomicReference<RunningPreview>(null)
    private val threads = arrayListOf<Thread>()

    private val runPreviewThread = repeatWhileAliveThread {
        fun startPreviewProcess(config: PreviewHostConfig): Process =
            ProcessBuilder(
                config.javaExecutable,
                "-Djava.awt.headless=true",
                "-classpath",
                config.hostClasspath,
                PREVIEW_HOST_CLASS_NAME,
                previewSocket.localPort.toString()
            ).apply {
                // todo: non verbose mode
                redirectOutput(ProcessBuilder.Redirect.INHERIT)
                redirectError(ProcessBuilder.Redirect.INHERIT)
            }.start()

        val runningPreview = runningPreview.get()
        val previewConfig = previewHostConfig.get()
        if (previewConfig != null && runningPreview?.isAlive != true) {
            val process = startPreviewProcess(previewConfig)
            val connection = tryAcceptConnection(previewSocket, "PREVIEW")
            connection?.receiveAttach {
                this.runningPreview.set(RunningPreview(connection, process))
            }
        }
    }

    private val sendPreviewRequestThread = repeatWhileAliveThread {
        withLivePreviewConnection {
            val classpath = previewClasspath.get()
            val fqName = previewFqName.get()
            val frameSize = previewFrameSize.get()

            if (classpath != null && frameSize != null && fqName != null) {
                val request = FrameRequest(fqName, frameSize.width, frameSize.height)
                if (shouldRequestFrame.get() && frameRequest.get() == null) {
                    if (shouldRequestFrame.compareAndSet(true, false)) {
                        if (frameRequest.compareAndSet(null, request)) {
                            sendPreviewRequest(classpath, request)
                        } else {
                            shouldRequestFrame.compareAndSet(false, true)
                        }
                    }
                }
            }
        }
    }

    private val receivePreviewResponseThread = repeatWhileAliveThread {
        withLivePreviewConnection {
            receiveFrame { bytes ->
                frameRequest.get()?.let { request ->
                    frameRequest.compareAndSet(request, null)
                }
                onNewFrame(bytes)
            }
        }
    }

    private val gradleCallbackThread = repeatWhileAliveThread {
        tryAcceptConnection(gradleCallbackSocket, "GRADLE_CALLBACK")?.let { connection ->
            while (isAlive.get() && connection.isAlive) {
                connection.receiveConfigFromGradle(
                    onPreviewClasspath = { previewClasspath.set(it) },
                    onPreviewHostConfig = { previewHostConfig.set(it) },
                    onPreviewFqName = { previewFqName.set(it) }
                )
                shouldRequestFrame.set(true)
                sendPreviewRequestThread.interrupt()
            }
        }
    }

    override fun close() {
        if (isAlive.compareAndSet(true, false)) {
            closeImpl()
        }
    }

    override fun updateFrameSize(width: Int, height: Int) {
        previewFrameSize.set(Dimension(width, height))
        shouldRequestFrame.set(true)
        sendPreviewRequestThread.interrupt()
    }

    override val gradleCallbackPort: Int
        get() = gradleCallbackSocket.localPort

    private fun closeImpl() {
        log { "CLOSING THREADS" }
        threads.forEach { it.interrupt() }
        threads.forEach {
            it.join(1000)
        }
        log { "CLOSING PREVIEW HOST" }
        runningPreview.get()?.let { (connection, process) ->
            connection.close()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                // todo: check exit code
                process.destroyForcibly()
            }

        }
        log { "CLOSING PREVIEW SOCKET" }
        previewSocket.close()
        log { "CLOSING GRADLE SOCKET" }
        gradleCallbackSocket.close()
        log { "CLOSED" }
    }

    private fun tryAcceptConnection(
        serverSocket: ServerSocket, socketType: String
    ): RemoteConnection? {
        while (isAlive.get()) {
            try {
                val socket = serverSocket.accept()
                return RemoteConnectionImpl(
                    socket = socket,
                    log = PrintStreamLogger("CONNECTION ($socketType) #${connectionNumber.incrementAndGet()}"),
                    onClose = {
                        // todo
                    }
                )
            } catch (e: IOException) {
                if (e !is SocketTimeoutException) {
                    log.error { e.stackTraceToString() }
                }
            }
        }

        return null
    }

    private inline fun withLivePreviewConnection(fn: RemoteConnection.() -> Unit) {
        val runningPreview = runningPreview.get() ?: return
        if (runningPreview.isAlive) {
            runningPreview.connection.fn()
        }
    }

    private inline fun repeatWhileAliveThread(
        sleepDelayMs: Long = DEFAULT_SLEEP_DELAY_MS,
        crossinline fn: () -> Unit
    ) = thread(start = false) {
        while (isAlive.get()) {
            try {
                fn()
                Thread.sleep(sleepDelayMs)
            } catch (e: InterruptedException) {
                continue
            } catch (e: Throwable) {
                e.printStackTrace(System.err)
                break
            }
        }
    }.also {
        threads.add(it)
        it.start()
    }
}
