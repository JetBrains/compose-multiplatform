/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.io.File
import java.net.SocketTimeoutException
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess

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

internal class PreviewHost(connection: RemoteConnection) {
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
                        val frame = renderFrame(classpath, request)
                        connection.sendFrame(frame)
                    }
                }
                Thread.sleep(DEFAULT_SLEEP_DELAY_MS)
            } catch (e: InterruptedException) {
                continue
            }
        }
    }

    val receiverThread = thread {
        try {
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
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace(System.err)
            exitProcess(1)
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
        val previewFacade = classloader.loadClass(PREVIEW_FACADE_CLASS_NAME)
        val render = previewFacade.getMethod("render", String::class.java, Int::class.java, Int::class.java)
        val (fqName, w, h) = request
        return render.invoke(previewFacade, fqName, w, h) as ByteArray
    }

    companion object {
        private const val PREVIEW_FACADE_CLASS_NAME =
            "androidx.compose.desktop.ui.tooling.preview.runtime.NonInteractivePreviewFacade"

        @JvmStatic
        fun main(args: Array<String>) {
            val port = args[0].toInt()
            val connection =
                getLocalConnectionOrNull(
                    port,
                    logger = PrintStreamLogger("PREVIEW_HOST"),
                    onClose = { exitProcess(ExitCodes.OK) }
                )
            if (connection != null) {
                PreviewHost(connection).join()
            } else {
                exitProcess(ExitCodes.COULD_NOT_CONNECT_TO_PREVIEW_MANAGER)
            }
        }
    }
}
