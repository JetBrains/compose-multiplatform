package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

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

@OptIn(ExperimentalResourceApi::class)
internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        return readAsArrayBuffer(path).let { buffer ->
            buffer.toByteArray(0, buffer.byteLength)
        }
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        return readAsArrayBuffer(path).toByteArray(offset.toInt(), size.toInt())
    }

    private suspend fun readAsArrayBuffer(path: String): ArrayBuffer {
        val resPath = WebResourcesConfiguration.getResourcePath(path)
        val response = window.fetch(resPath).await<Response>()
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        return response.arrayBuffer().await()
    }

    private fun ArrayBuffer.toByteArray(offset: Int, size: Int): ByteArray  {
        val source = Int8Array(this, offset, size)
        return jsInt8ArrayToKotlinByteArray(source)
    }
}