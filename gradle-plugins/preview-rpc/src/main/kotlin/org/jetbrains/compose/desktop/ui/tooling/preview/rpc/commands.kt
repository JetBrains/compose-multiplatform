/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.net.URLDecoder
import java.net.URLEncoder

internal fun RemoteConnection.sendAttach() {
    sendCommand(Command.Type.ATTACH, PROTOCOL_VERSION.toString())
}

internal fun RemoteConnection.receiveAttach(
    listener: PreviewListener? = null,
    fn: () -> Unit
) {
    receiveCommand { (type, args) ->
        if (type == Command.Type.ATTACH) {
            val version = args.firstOrNull()?.toIntOrNull() ?: 0
            if (PROTOCOL_VERSION != version) {
                listener?.onError(
                    "Compose Multiplatform Gradle plugin version is not compatible with Intellij plugin version"
                )
            }
            fn()
        }
    }
}

internal fun RemoteConnection.sendFrame(frame: RenderedFrame) {
    sendCommand(Command.Type.FRAME, frame.width.toString(), frame.height.toString())
    sendData(frame.bytes)
}

internal fun RemoteConnection.sendError(e: Exception) {
    sendCommand(Command.Type.ERROR)
    sendUtf8StringData(e.stackTraceString)
}

internal fun RemoteConnection.receiveFrame(
    onFrame: (RenderedFrame) -> Unit,
    onError: (String) -> Unit
) {
    receiveCommand { (type, args) ->
        when (type) {
            Command.Type.FRAME -> {
                receiveData { bytes ->
                    val (w, h) = args
                    val frame = RenderedFrame(bytes, width = w.toInt(), height = h.toInt())
                    onFrame(frame)
                }
            }
            Command.Type.ERROR -> {
                receiveUtf8StringData { stacktrace ->
                    onError(stacktrace)
                }
            }
            else -> error("Received unexpected command type: $type")
        }
    }
}

fun RemoteConnection.sendConfigFromGradle(
    config: PreviewHostConfig,
    previewClasspath: String,
    previewFqName: String
) {
    sendCommand(Command.Type.PREVIEW_CONFIG, URLEncoder.encode(config.javaExecutable, Charsets.UTF_8))
    sendUtf8StringData(config.hostClasspath)
    sendCommand(Command.Type.PREVIEW_CLASSPATH)
    sendUtf8StringData(previewClasspath)
    sendCommand(Command.Type.PREVIEW_FQ_NAME)
    sendUtf8StringData(previewFqName)
}

data class ConfigFromGradle(
    val previewClasspath: String,
    val previewFqName: String,
    val previewHostConfig: PreviewHostConfig
)

fun RemoteConnection.receiveConfigFromGradle(): ConfigFromGradle? {
    var previewClasspath: String? = null
    var previewFqName: String? = null
    var previewHostConfig: PreviewHostConfig? = null

    receiveCommand { (type, args) ->
        check(type == Command.Type.PREVIEW_CONFIG) {
            "Expected ${Command.Type.PREVIEW_CONFIG}, got $type"
        }
        val javaExecutable = URLDecoder.decode(args[0], Charsets.UTF_8)
        receiveUtf8StringData { hostClasspath ->
            previewHostConfig = PreviewHostConfig(javaExecutable = javaExecutable, hostClasspath = hostClasspath)
        }
    }
    receiveCommand { (type, _) ->
        check(type == Command.Type.PREVIEW_CLASSPATH) {
            "Expected ${Command.Type.PREVIEW_CLASSPATH}, got $type"
        }
        receiveUtf8StringData { previewClasspath = it }
    }
    receiveCommand { (type, _) ->
        check(type == Command.Type.PREVIEW_FQ_NAME) {
            "Expected ${Command.Type.PREVIEW_FQ_NAME}, got $type"
        }
        receiveUtf8StringData { previewFqName = it }
    }

    return if (previewClasspath != null && previewFqName != null && previewHostConfig != null) {
        ConfigFromGradle(
            previewClasspath = previewClasspath!!,
            previewFqName = previewFqName!!,
            previewHostConfig = previewHostConfig!!
        )
    } else null
}

internal fun RemoteConnection.sendPreviewRequest(
    previewClasspath: String,
    request: FrameRequest
) {
    sendCommand(Command.Type.PREVIEW_CLASSPATH)
    sendData(previewClasspath.toByteArray(Charsets.UTF_8))
    val (id, fqName, frameConfig) = request
    val (w, h, scale) = frameConfig
    val args = arrayListOf(fqName, id.toString(), w.toString(), h.toString())
    if (scale != null) {
        val scaleLong = java.lang.Double.doubleToRawLongBits(scale)
        args.add(scaleLong.toString())
    }
    sendCommand(Command.Type.FRAME_REQUEST, *args.toTypedArray())
}

internal fun RemoteConnection.receivePreviewRequest(
    onPreviewClasspath: (String) -> Unit,
    onFrameRequest: (FrameRequest) -> Unit
) {
    receiveCommand { (type, args) ->
        when (type) {
            Command.Type.PREVIEW_CLASSPATH -> {
                receiveUtf8StringData { onPreviewClasspath(it) }
            }
            Command.Type.FRAME_REQUEST -> {
                val fqName = args.getOrNull(0)
                val id = args.getOrNull(1)?.toLongOrNull()
                val w = args.getOrNull(2)?.toIntOrNull()
                val h = args.getOrNull(3)?.toIntOrNull()
                val scale = args.getOrNull(4)?.toLongOrNull()?.let { java.lang.Double.longBitsToDouble(it) }
                if (
                    fqName != null && fqName.isNotEmpty()
                        && id != null
                        && w != null && w > 0
                        && h != null && h > 0
                ) {
                    onFrameRequest(FrameRequest(id, fqName, FrameConfig(width = w, height = h, scale = scale)))
                }
            }
            else -> {
                // todo
            }
        }
    }
}