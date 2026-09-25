/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.fetch.Response
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.asJsException

private external interface AbortSignal
private external class AbortController {
    val signal: AbortSignal
    fun abort()
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url, signal) => window.fetch(url, { signal })")
private external fun jsFetchWithSignal(url: String, signal: AbortSignal): Promise<Response>

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun cancellableFetch(url: String): Response = suspendCancellableCoroutine { cont ->
    val ac = AbortController()
    jsFetchWithSignal(url, ac.signal).then(
        onFulfilled = { cont.resume(it); null },
        onRejected = { cont.resumeWithException(it.asJsException()); null }
    )
    cont.invokeOnCancellation { ac.abort() }
}

// The only platform-specific part of the web resource reader
internal expect suspend fun Blob.asByteArray(): ByteArray

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader = DefaultWebResourceReader

@ExperimentalResourceApi
@OptIn(ExperimentalWasmJsInterop::class)
internal object DefaultWebResourceReader : ResourceReader {
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
}
