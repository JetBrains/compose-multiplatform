package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Promise

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader {
    if (isInTestEnvironment()) return TestJsResourceReader
    return DefaultJsResourceReader
}

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

// It uses a synchronous XmlHttpRequest (blocking!!!)
private object TestJsResourceReader : ResourceReader {
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
            // For blocking XmlHttpRequest the response can be only in text form, so we convert it to bytes manually
            val text = request.responseText
            val bytes = Uint8Array(text.length)
            js("for (var i = 0; i < text.length; i++) { bytes[i] = text.charCodeAt(i) & 0xFF; }")
            return bytes.unsafeCast<ByteArray>()
        }
        throw MissingResourceException("$resPath")
    }
}

private fun isInTestEnvironment(): Boolean =
    js("window.composeResourcesTesting == true")