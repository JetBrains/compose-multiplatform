/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.ARRAYBUFFER
import org.khronos.webgl.get
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ExperimentalResourceApi
actual fun resource(path: String): Resource = JSResourceImpl(path)

@ExperimentalResourceApi
private class JSResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ByteArray {
        return suspendCoroutine { continuation ->
            val req = XMLHttpRequest()
            req.open("GET", "/$path", true)
            req.responseType = "arraybuffer".asDynamic().unsafeCast<XMLHttpRequestResponseType>()

            req.onload = { _ ->
                val arrayBuffer = req.response
                if (arrayBuffer is ArrayBuffer) {
                    val size = arrayBuffer.byteLength
                    continuation.resume(arrayBuffer.toByteArray())
                } else {
                    continuation.resumeWithException(MissingResourceException(path))
                }
                null
            }
            req.send(null)
        }
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return kotlin.ByteArray(byteLength) {
        source.get(it)
    }
}

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")
