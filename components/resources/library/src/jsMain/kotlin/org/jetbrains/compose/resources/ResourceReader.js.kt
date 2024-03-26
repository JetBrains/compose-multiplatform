package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Int8Array
import kotlin.js.Promise

private fun ArrayBuffer.toByteArray(): ByteArray =
    Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val resPath = WebResourcesConfiguration.getResourcePath(path)
    val response = window.fetch(resPath).await()
    if (!response.ok) {
        throw MissingResourceException(resPath)
    }
    return response.arrayBuffer().await().toByteArray()
}

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual fun getResourceAsFlow(path: String, byteCount: Int): Flow<ByteArray> {
    check(byteCount > 0) { "byteCount: $byteCount" }
    return flow {
        val resPath = WebResourcesConfiguration.getResourcePath(path)
        val response = window.fetch(resPath).await()
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        val body = response.body ?: throw MissingResourceException(resPath)
        val bodyReader = body.getReader(js("""({ mode: "byob" })""")).unsafeCast<ReadableStreamBYOBReader>()
        var buffer = ArrayBuffer(byteCount)
        while (true) {
            val readResult = try {
                bodyReader.read(Int8Array(buffer)).await()
            } catch (e: Throwable) {
                throw ResourceIOException(e)
            }
            val value = readResult.value
            if (value != null) {
                val array = value.unsafeCast<ByteArray>()
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

/**
 * Exposes the JavaScript [ReadableStreamBYOBReader](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamBYOBReader) to Kotlin
 */
private external interface ReadableStreamBYOBReader {
    fun read(view: ArrayBufferView): Promise<ReadableStreamBYOBReaderReadResult>
}

private external interface ReadableStreamBYOBReaderReadResult {
    val value: ArrayBufferView?
    val done: Boolean
}