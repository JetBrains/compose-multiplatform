package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import kotlin.js.Promise
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val resPath = WebResourcesConfiguration.getResourcePath(path)
    val response = window.fetch(resPath).await<Response>()
    if (!response.ok) {
        throw MissingResourceException(resPath)
    }
    return response.arrayBuffer().await<ArrayBuffer>().toByteArray()
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

@OptIn(ExperimentalResourceApi::class)
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
@InternalResourceApi
actual fun getResourceAsFlow(path: String, byteCount: Int): Flow<ByteArray> {
    check(byteCount > 0) { "byteCount: $byteCount" }
    return flow {
        val resPath = WebResourcesConfiguration.getResourcePath(path)
        val response = window.fetch(resPath).await<Response>()
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        val body = response.body ?: throw MissingResourceException(resPath)
        val bodyReader = (body as ReadableStream).getBYOBReader()
        var buffer = ArrayBuffer(byteCount)
        while (true) {
            val readResult = try {
                bodyReader.read(Int8Array(buffer)).await<ReadableStreamBYOBReaderReadResult>()
            } catch (e: Throwable) {
                throw ResourceIOException(e)
            }
            val value = readResult.value
            if (value != null) {
                val array = jsInt8ArrayToKotlinByteArray(value as Int8Array)
                if (array.isNotEmpty()) {
                    emit(array)
                }
                buffer = value.buffer
            }
            if (readResult.done) {
                break
            }
        }
    }
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

    @OptIn(UnsafeWasmMemoryApi::class) return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}

/**
 * Exposes the JavaScript [ReadableStream](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream) to Kotlin
 */
private external interface ReadableStream : JsAny {
    fun <T : JsAny> getReader(options: JsAny): T
}

private fun byobReaderOption(): JsAny = js("""({ mode: "byob" })""")

private fun ReadableStream.getBYOBReader(): ReadableStreamBYOBReader {
    return getReader(byobReaderOption())
}

/**
 * Exposes the JavaScript [ReadableStreamBYOBReader](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamBYOBReader) to Kotlin
 */
private external interface ReadableStreamBYOBReader : JsAny {
    fun read(view: ArrayBufferView): Promise<ReadableStreamBYOBReaderReadResult>
    fun releaseLock()
}

private external interface ReadableStreamBYOBReaderReadResult : JsAny {
    val value: ArrayBufferView?
    val done: Boolean
}