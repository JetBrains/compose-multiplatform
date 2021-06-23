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
import kotlin.system.measureTimeMillis

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

    private val runPreviewThread = repeatWhileAliveThread("runPreview") {
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

    private val sendPreviewRequestThread = repeatWhileAliveThread("sendPreviewRequest") {
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

    private val receivePreviewResponseThread = repeatWhileAliveThread("receivePreviewResponse") {
        withLivePreviewConnection {
            receiveFrame { bytes ->
                frameRequest.get()?.let { request ->
                    frameRequest.compareAndSet(request, null)
                }
                onNewFrame(bytes)
            }
        }
    }

    private val gradleCallbackThread = repeatWhileAliveThread("gradleCallback") {
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
        if (!isAlive.compareAndSet(true, false)) return

        closeService("PREVIEW MANAGER") {
            val runningPreview = runningPreview.getAndSet(null)
            val previewConnection = runningPreview?.connection
            val previewProcess = runningPreview?.process
            threads.forEach { it.interrupt() }

            closeService("PREVIEW HOST CONNECTION") { previewConnection?.close() }
            closeService("PREVIEW SOCKET") { previewSocket.close() }
            closeService("GRADLE SOCKET") { gradleCallbackSocket.close() }
            closeService("THREADS") {
                for (i in 0..3) {
                    var aliveThreads = 0
                    for (t in threads) {
                        if (t.isAlive) {
                            aliveThreads++
                            t.interrupt()
                        }
                    }
                    if (aliveThreads == 0) break
                    else Thread.sleep(300)
                }
                val aliveThreads = threads.filter { it.isAlive }
                if (aliveThreads.isNotEmpty()) {
                    error("Could not stop threads: ${aliveThreads.joinToString(", ") { it.name }}")
                }
            }
            closeService("PREVIEW HOST PROCESS") {
                previewProcess?.let { process ->
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        log { "FORCIBLY DESTROYING PREVIEW HOST PROCESS" }
                        // todo: check exit code
                        process.destroyForcibly()
                    }
                }
            }
        }
    }

    private inline fun closeService(name: String, doClose: () -> Unit) {
        try {
            log { "CLOSING $name" }
            val ms = measureTimeMillis {
                doClose()
            }
            log { "CLOSED $name in $ms ms" }
        } catch (e: Exception) {
            log.error { "ERROR CLOSING $name: ${e.stackTraceString}" }
        }
    }

    override fun updateFrameSize(width: Int, height: Int) {
        previewFrameSize.set(Dimension(width, height))
        shouldRequestFrame.set(true)
        sendPreviewRequestThread.interrupt()
    }

    override val gradleCallbackPort: Int
        get() = gradleCallbackSocket.localPort

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
                    if (isAlive.get()) {
                        log.error { e.stackTraceToString() }
                    }
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
        name: String,
        sleepDelayMs: Long = DEFAULT_SLEEP_DELAY_MS,
        crossinline fn: () -> Unit
    ) = thread(name = name, start = false) {
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
