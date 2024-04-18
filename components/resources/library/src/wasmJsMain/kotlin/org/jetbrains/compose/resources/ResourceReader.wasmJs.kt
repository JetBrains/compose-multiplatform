package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import org.w3c.files.Blob
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

internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
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
        val response = window.fetch(resPath).await<Response>()
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