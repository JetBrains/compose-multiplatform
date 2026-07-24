@file:OptIn(ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.w3c.fetch.Response
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

@JsFun("(blob) => blob.arrayBuffer()")
private external fun jsExportBlobAsArrayBuffer(blob: Blob): Promise<ArrayBuffer>

private external interface AbortSignal
private external class AbortController {
    val signal: AbortSignal
    fun abort()
}

@JsFun("(url, signal) => window.fetch(url, { signal })")
private external fun jsFetchWithSignal(url: String, signal: AbortSignal): Promise<Response>

@Suppress("UNCHECKED_CAST")
private suspend fun <T> cancellableFetch(url: String): T = suspendCancellableCoroutine { cont ->
    val ac = AbortController()
    jsFetchWithSignal(url, ac.signal).then(
        onFulfilled = { cont.resume(it as T); null },
        onRejected = { cont.resumeWithException(it.toThrowableOrNull() ?: error("Unexpected non-Kotlin exception $it")); null }
    )
    cont.invokeOnCancellation { ac.abort() }
}

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader = DefaultWasmResourceReader

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
            try {
                cancellableFetch(resPath)
            } catch (_: Throwable) {
                throw MissingResourceException(resPath)
            }
        }
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        return response.blob().await()
    }

    private suspend fun Blob.asByteArray(): ByteArray {
        val buffer: ArrayBuffer = jsExportBlobAsArrayBuffer(this).await()
        return fastArrayBufferToByteArray(buffer)
    }
}
