/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
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
import kotlin.wasm.unsafe.*
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@ExperimentalResourceApi
actual fun resource(path: String): Resource = JSResourceImpl(path)

@ExperimentalResourceApi
private class JSResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ByteArray {
        return suspendCoroutine { continuation ->
            val req = XMLHttpRequest()
            req.open("GET", "/$path", true)
            req.responseType = "arraybuffer".toJsString().unsafeCast<XMLHttpRequestResponseType>()

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

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

internal actual fun isSyncResourceLoadingSupported(): Boolean = false

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = throw UnsupportedOperationException()

internal actual fun parseXML(byteArray: ByteArray): Element {
    throw UnsupportedOperationException("XML Vector Drawables are not supported for Web target")
}


private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
internal external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

internal fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}
