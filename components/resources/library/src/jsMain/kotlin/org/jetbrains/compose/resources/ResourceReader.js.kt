package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

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
        val response = window.fetch(resPath).await()
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        return response.arrayBuffer().await()
    }

    private fun ArrayBuffer.toByteArray(offset: Int, size: Int): ByteArray =
        Int8Array(this, offset, size).unsafeCast<ByteArray>()
}