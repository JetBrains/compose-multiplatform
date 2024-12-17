/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.io.File
import java.lang.RuntimeException
import java.net.SocketTimeoutException
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

val PREVIEW_HOST_CLASS_NAME: String
    get() = PreviewHost::class.java.canonicalName

private class PreviewClassloaderProvider {
    private var currentClasspath = arrayOf<File>()
    private var currentSnapshots = hashSetOf<Snapshot>()
    private var currentClassloader = URLClassLoader(emptyArray())

    // todo: read in memory on Windows
    fun getClassloader(classpathString: String): ClassLoader {
        val newClasspath = classpathString.split(File.pathSeparator)
            .map { File(it) }
            .toTypedArray()
        val newSnapshots = newClasspath.mapTo(HashSet()) { Snapshot(it) }
        if (!currentClasspath.contentEquals(newClasspath) || newSnapshots != currentSnapshots) {
            currentClasspath = newClasspath
            currentSnapshots = newSnapshots

            currentClassloader.close()
            currentClassloader = URLClassLoader(Array(newClasspath.size) { newClasspath[it].toURI().toURL() })
        }

        return currentClassloader
    }

    private data class Snapshot(val file: File, val lastModified: Long, val size: Long) {
        constructor(file: File) : this(file, file.lastModified(), file.length())
    }
}

internal class PreviewHost(private val log: PreviewLogger, connection: RemoteConnection) {
    private val previewClasspath = AtomicReference<String>(null)
    private val previewRequest = AtomicReference<FrameRequest>(null)
    private val classloaderProvider = PreviewClassloaderProvider()

    init {
        connection.sendAttach()
    }

    private val senderThread = thread {
        while (connection.isAlive) {
            try {
                val classpath = previewClasspath.get()
                val request = previewRequest.get()
                if (classpath != null && request != null) {
                    if (previewRequest.compareAndSet(request, null)) {
                        val bytes = renderFrame(classpath, request)
                        val config = request.frameConfig
                        val frame = RenderedFrame(bytes, width = config.width, height = config.height)
                        connection.sendFrame(frame)
                    }
                }
                Thread.sleep(DEFAULT_SLEEP_DELAY_MS)
            } catch (e: InterruptedException) {
                continue
            } catch (e: Exception) {
                if (connection.isAlive) {
                    connection.sendError(e)
                } else {
                    throw IllegalStateException("Could not report an exception: IDE connection is not alive", e)
                }
            }
        }
    }.setUpUnhandledExceptionHandler(ExitCodes.SENDER_FATAL_ERROR)

    val receiverThread = thread {
        while (connection.isAlive) {
            try {
                connection.receivePreviewRequest(
                    onPreviewClasspath = {
                        previewClasspath.set(it)
                        senderThread.interrupt()
                    },
                    onFrameRequest = {
                        previewRequest.set(it)
                        senderThread.interrupt()
                    }
                )
            } catch (e: SocketTimeoutException) {
                continue
            } catch (e: InterruptedException) {
                continue
            }
        }
    }.setUpUnhandledExceptionHandler(ExitCodes.RECEIVER_FATAL_ERROR)

    private fun Thread.setUpUnhandledExceptionHandler(exitCode: Int): Thread = apply {
        uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            try {
                System.err.println()
                System.err.println(PREVIEW_START_OF_STACKTRACE_MARKER)
                e.printStackTrace(System.err)
            } finally {
                exitProcess(exitCode)
            }
        }
    }

    fun join() {
        senderThread.join()
        receiverThread.join()
    }

    private fun renderFrame(
        classpath: String,
        request: FrameRequest
    ): ByteArray {
        val classloader = classloaderProvider.getClassloader(classpath)
        val thread = Thread.currentThread()
        val prevContextClassloader = thread.contextClassLoader
        thread.contextClassLoader = classloader
        return try {
            renderFrame(classloader, request)
        } finally {
            thread.contextClassLoader = prevContextClassloader
        }
    }

    private fun renderFrame(
        classloader: ClassLoader,
        request: FrameRequest
    ): ByteArray {
        val previewFacade = classloader.loadClass(PREVIEW_FACADE_CLASS_NAME)
        val renderArgsClasses = arrayOf(
            String::class.java,
            Int::class.java,
            Int::class.java,
            java.lang.Double::class.java
        )
        val render = try {
            previewFacade.getMethod("render", *renderArgsClasses)
        } catch (e: NoSuchMethodException) {
            val signature =
                "${previewFacade.canonicalName}#render(${renderArgsClasses.joinToString(", ") { it.simpleName }})"
            val possibleCandidates = previewFacade.methods.filter { it.name == "render" }
            throw RuntimeException("Could not find method '$signature'. Possible candidates: \n${possibleCandidates.joinToString("\n") { "* ${it}" }}", e)
        }
        val (_, fqName, frameConfig) = request
        val scaledWidth = frameConfig.scaledWidth
        val scaledHeight = frameConfig.scaledHeight
        val scale = frameConfig.scale
        log { "RENDERING '$fqName' ${scaledWidth}x$scaledHeight@${scale ?: 1f}" }
        var bytes: ByteArray
        val ms = measureTimeMillis {
            bytes = render.invoke(previewFacade, fqName, scaledWidth, scaledHeight, scale) as ByteArray
        }
        log { "RENDERED [${bytes.size}] in $ms ms" }
        return bytes
    }

    companion object {
        private const val PREVIEW_FACADE_CLASS_NAME =
            "androidx.compose.desktop.ui.tooling.preview.runtime.NonInteractivePreviewFacade"

        @JvmStatic
        fun main(args: Array<String>) {
            val port = args[0].toInt()
            val logger = PrintStreamLogger("PREVIEW_HOST")
            val onClose = { exitProcess(ExitCodes.OK) }
            val connection = getLocalConnectionOrNull(port, logger, onClose = onClose)
            if (connection != null) {
                PreviewHost(logger, connection).join()
            } else {
                exitProcess(ExitCodes.COULD_NOT_CONNECT_TO_PREVIEW_MANAGER)
            }
        }
    }
}
