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

internal class PreviewHost(connection: RemoteConnection) {
    private val previewClasspath = AtomicReference<String>(null)
    private val previewRequest = AtomicReference<FrameRequest>(null)

    init {
        connection.sendAttach()
    }

    private val senderThread = thread {
        while (connection.isAlive) {
            try {
                val classpath = previewClasspath.get()
                val request = previewRequest.get()
                if (classpath != null && request != null) {
                    if (previewRequest.compareAndExchange(request, null) === request) {
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
        val classpath = classpath.split(File.pathSeparator)
        val urls = Array(classpath.size) { File(classpath[it]).toURI().toURL() }
        return URLClassLoader(urls).use { classloader ->
            val previewFacade = classloader.loadClass(PREVIEW_FACADE_CLASS_NAME)
            val render = previewFacade.getMethod("render", String::class.java, Int::class.java, Int::class.java)
            val (fqName, w, h) = request
            render.invoke(previewFacade, fqName, w, h) as ByteArray
        }
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
