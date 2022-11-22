/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalResourceApi
actual fun resource(path: String): Resource = JSResourceImpl(path)

@ExperimentalResourceApi
private class JSResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): Result<ByteArray> {
        return suspendCoroutine { continuation ->
            val req = XMLHttpRequest()
            req.open("GET", "/$path", true)
            req.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

            req.onload = { event ->
                val arrayBuffer = req.response
                if (arrayBuffer is ArrayBuffer) {
                    continuation.resume(Result.success(arrayBuffer.toByteArray()))
                } else {
                    continuation.resume(Result.failure(MissingResourceException(path)))
                }
            }
            req.send(null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is JSResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

private fun ArrayBuffer.toByteArray() = Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("missing resource with path: $path")
