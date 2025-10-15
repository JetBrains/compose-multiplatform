package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Promise
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

@JsFun("(blob) => blob.arrayBuffer()")
private external fun jsExportBlobAsArrayBuffer(blob: Blob): Promise<ArrayBuffer>

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader {
    if (isInTestEnvironment()) return TestWasmResourceReader
    return DefaultWasmResourceReader
}

@ExperimentalResourceApi
@OptIn(ExperimentalWasmJsInterop::class)
internal object DefaultWasmResourceReader : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        return readAsBlob(path).asByteArray()
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        val part = readAsBlob(path).slice(offset.toInt(), (offset + size).toInt())
        return part.asByteArray()
    }

    override fun getUri(path: String): String {
        val location = window.location
        return getResourceUrl(location.origin, location.pathname, path)
    }

    private suspend fun readAsBlob(path: String): Blob {
        val resPath = WebResourcesConfiguration.getResourcePath(path)
        val response = ResourceWebCache.load(resPath) {
            window.fetch(resPath).await()
        }
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        return response.blob().await()
    }

    private suspend fun Blob.asByteArray(): ByteArray {
        val buffer: ArrayBuffer = jsExportBlobAsArrayBuffer(this).await()
        return Int8Array(buffer).asByteArray()
    }

    private fun Int8Array.asByteArray(): ByteArray {
        val array = this
        val size = array.length

        @OptIn(UnsafeWasmMemoryApi::class)
        return withScopedMemoryAllocator { allocator ->
            val memBuffer = allocator.allocate(size)
            val dstAddress = memBuffer.address.toInt()
            jsExportInt8ArrayToWasm(array, size, dstAddress)
            ByteArray(size) { i -> (memBuffer + i).loadByte() }
        }
    }
}

// It uses a synchronous XmlHttpRequest (blocking!!!)
private object TestWasmResourceReader : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        return readByteArray(path)
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        return readByteArray(path).sliceArray(offset.toInt() until (offset + size).toInt())
    }

    override fun getUri(path: String): String {
        val location = window.location
        return getResourceUrl(location.origin, location.pathname, path)
    }

    private fun readByteArray(path: String): ByteArray {
        val resPath = WebResourcesConfiguration.getResourcePath(path)
        val request = XMLHttpRequest()
        request.open("GET", resPath, false)
        request.overrideMimeType("text/plain; charset=x-user-defined")
        request.send()
        if (request.status == 200.toShort()) {
            return requestResponseAsByteArray(request).asByteArray()
        }
        println("Request status is not 200 - $resPath, status: ${request.status}")
        throw MissingResourceException(resPath)
    }

    private fun Int8Array.asByteArray(): ByteArray {
        val array = this
        val size = array.length

        @OptIn(UnsafeWasmMemoryApi::class)
        return withScopedMemoryAllocator { allocator ->
            val memBuffer = allocator.allocate(size)
            val dstAddress = memBuffer.address.toInt()
            jsExportInt8ArrayToWasm(array, size, dstAddress)
            ByteArray(size) { i -> (memBuffer + i).loadByte() }
        }
    }
}

// For blocking XmlHttpRequest the response can be only in text form, so we convert it to bytes manually
private fun requestResponseAsByteArray(req: XMLHttpRequest): Int8Array =
    js(""" {
        var text = req.responseText;
        var int8Arr = new Int8Array(text.length);
        for (var i = 0; i < text.length; i++) {
            int8Arr[i] = text.charCodeAt(i) & 0xFF;
        }
        return int8Arr;
    }""")

private fun isInTestEnvironment(): Boolean =
    js("window.composeResourcesTesting == true")
