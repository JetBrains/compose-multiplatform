package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import kotlin.js.Promise

@JsFun("(blob) => blob.arrayBuffer()")
private external fun jsExportBlobAsArrayBuffer(blob: Blob): Promise<ArrayBuffer>

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
            window.fetch(resPath).await()
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
