package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import org.w3c.files.Blob
import kotlin.js.Promise

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader = DefaultJsResourceReader

@ExperimentalResourceApi
internal object DefaultJsResourceReader : ResourceReader {
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
        val response =  ResourceWebCache.load(resPath) {
            // TODO: avoid js(...) calls here after https://github.com/Kotlin/kotlinx-browser/issues/24
            js("window.fetch(resPath)").unsafeCast<Promise<Response>>().await()
        }
        if (!response.ok) {
            throw MissingResourceException(resPath)
        }
        return response.blob().await()
    }

    private suspend fun Blob.asByteArray(): ByteArray {
        //https://developer.mozilla.org/en-US/docs/Web/API/Blob/arrayBuffer
        val buffer = asDynamic().arrayBuffer() as Promise<ArrayBuffer>
        return Int8Array(buffer.await()).unsafeCast<ByteArray>()
    }
}
