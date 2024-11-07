/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils.RingBuffer
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

data class FrameConfig(val width: Int, val height: Int, val scale: Double?) {
    val scaledWidth: Int get() = scaledValue(width)
    val scaledHeight: Int get() = scaledValue(height)

    private fun scaledValue(value: Int): Int =
        if (scale != null) (value.toDouble() * scale).toInt() else value
}

data class FrameRequest(
    val id: Long,
    val composableFqName: String,
    val frameConfig: FrameConfig
)

interface PreviewManager {
    val gradleCallbackPort: Int
    fun updateFrameConfig(frameConfig: FrameConfig)
    fun close()
}

private data class RunningPreview(
    val connection: RemoteConnection,
    val process: Process
) {
    val isAlive: Boolean
        get() = connection.isAlive && process.isAlive
}

class PreviewManagerImpl(
    private val previewListener: PreviewListener
) : PreviewManager {
    // todo: add quiet mode
    private val log = PrintStreamLogger("SERVER")
    private val previewSocket = newServerSocket()
    private val gradleCallbackSocket = newServerSocket()
    private val connectionNumber = AtomicLong(0)
    private val isAlive = AtomicBoolean(true)

    // todo: restart when configuration changes
    private val previewHostConfig = AtomicReference<PreviewHostConfig>(null)
    private val previewClasspath = AtomicReference<String>(null)
    private val previewFqName = AtomicReference<String>(null)
    private val previewFrameConfig = AtomicReference<FrameConfig>(null)
    private val inProcessRequest = AtomicReference<FrameRequest>(null)
    private val processedRequest = AtomicReference<FrameRequest>(null)
    private val userRequestCount = AtomicLong(0)
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
                redirectOutput(ProcessBuilder.Redirect.PIPE)
                redirectError(ProcessBuilder.Redirect.PIPE)
                redirectErrorStream(true)
            }.start()

        val runningPreview = runningPreview.get()
        val previewConfig = previewHostConfig.get()
        if (previewConfig != null && runningPreview?.isAlive != true) {
            val process = startPreviewProcess(previewConfig)
            val connection = tryAcceptConnection(previewSocket, "PREVIEW")
            connection?.receiveAttach(listener = previewListener) {
                this.runningPreview.set(RunningPreview(connection, process))
            }
            val processLogLines = RingBuffer<String>(512)
            val exception = StringBuilder()
            var exceptionMarker = false
            process.inputStream.bufferedReader().forEachLine { line ->
                if (exceptionMarker) {
                    exception.appendLine(line)
                } else {
                    if (line.startsWith(PREVIEW_START_OF_STACKTRACE_MARKER)) {
                        exceptionMarker = true
                    } else {
                        processLogLines.add(line)
                    }
                }
            }
            while (process.isAlive) {
                process.waitFor(5, TimeUnit.SECONDS)
                if (process.isAlive) {
                    process.destroyForcibly()
                    process.waitFor(5, TimeUnit.SECONDS)
                }
            }
            if (process.isAlive) error("Preview process does not finish!")

            val exitCode = process.exitValue()
            if (exitCode != ExitCodes.OK) {
                val errorMessage = buildString {
                    appendLine("Preview process exited unexpectedly: exitCode=$exitCode")
                    if (exceptionMarker) {
                        appendLine(exception)
                    }
                }
                onError(errorMessage)
            }
        }
    }

    private val sendPreviewRequestThread = repeatWhileAliveThread("sendPreviewRequest") {
        withLivePreviewConnection {
            val classpath = previewClasspath.get()
            val fqName = previewFqName.get()
            val frameConfig = previewFrameConfig.get()

            if (classpath != null && frameConfig != null && fqName != null) {
                val request = FrameRequest(userRequestCount.get(), fqName, frameConfig)
                val prevRequest = processedRequest.get()
                if (inProcessRequest.get() == null && request != prevRequest) {
                    if (inProcessRequest.compareAndSet(null, request)) {
                        previewListener.onNewRenderRequest(request)
                        sendPreviewRequest(classpath, request)
                    }
                }
            }
        }
    }

    private val receivePreviewResponseThread = repeatWhileAliveThread("receivePreviewResponse") {
        withLivePreviewConnection {
            receiveFrame(
                onFrame = { renderedFrame ->
                    inProcessRequest.get()?.let { request ->
                        processedRequest.set(request)
                        inProcessRequest.compareAndSet(request, null)
                    }
                    previewListener.onRenderedFrame(renderedFrame)
                },
                onError = { error ->
                    previewHostConfig.set(null)
                    previewClasspath.set(null)
                    inProcessRequest.set(null)
                    onError(error)
                }
            )
        }
    }

    private val gradleCallbackThread = repeatWhileAliveThread("gradleCallback") {
        tryAcceptConnection(gradleCallbackSocket, "GRADLE_CALLBACK")?.let { connection ->
            while (isAlive.get() && connection.isAlive) {
                val config = connection.receiveConfigFromGradle()
                if (config != null) {
                    previewClasspath.set(config.previewClasspath)
                    previewFqName.set(config.previewFqName)
                    previewHostConfig.set(config.previewHostConfig)
                    userRequestCount.incrementAndGet()
                    sendPreviewRequestThread.interrupt()
                }
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

    override fun updateFrameConfig(frameConfig: FrameConfig) {
        previewFrameConfig.set(frameConfig)
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
            }
        }
    }.also {
        it.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            onError(e)
        }
        threads.add(it)
        it.start()
    }

    private fun onError(e: Throwable) {
        onError(e.stackTraceString)
    }

    private fun onError(error: String) {
        log.error { error }
        previewListener.onError(error)
    }
}
