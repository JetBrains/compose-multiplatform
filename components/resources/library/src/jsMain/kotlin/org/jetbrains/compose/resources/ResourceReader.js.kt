package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

private fun ArrayBuffer.toByteArray(): ByteArray =
    Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

@OptIn(ExperimentalResourceApi::class)
actual suspend fun readResourceBytes(path: String): ByteArray {
    val resPath = WebResourcesConfiguration.getResourcePath(path)
    val response = window.fetch(resPath).await()
    if (!response.ok) {
        throw MissingResourceException(resPath)
    }
    return response.arrayBuffer().await().toByteArray()
}