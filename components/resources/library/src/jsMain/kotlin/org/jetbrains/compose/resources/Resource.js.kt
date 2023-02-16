/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
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
            req.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

            req.onload = { event ->
                val arrayBuffer = req.response
                if (arrayBuffer is ArrayBuffer) {
                    continuation.resume(arrayBuffer.toByteArray())
                } else {
                    continuation.resumeWithException(MissingResourceException(path))
                }
            }
            req.send(null)
        }
    }
}

private fun ArrayBuffer.toByteArray() = Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

internal actual fun parseXML(byteArray: ByteArray): Element {
    throw UnsupportedOperationException("XML Vector Drawables are not supported for Web target")
}

internal actual fun isSyncResourceLoadingSupported() = false

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = throw UnsupportedOperationException()