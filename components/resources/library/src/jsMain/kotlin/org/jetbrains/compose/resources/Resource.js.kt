/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.ElementImpl
import org.jetbrains.compose.resources.vector.xmldom.MalformedXMLException
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.parsing.DOMParser
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

/**
 * WebResourcesConfiguration is used in [configureWebResources].
 */
@Suppress("unused")
@ExperimentalResourceApi
object WebResourcesConfiguration {

    @ExperimentalResourceApi
    internal var jsResourceImplFactory: (path: String) -> Resource = { urlResource("./$it") }

    /**
     * See [configureWebResources] for more details.
     */
    @ExperimentalResourceApi
    fun setResourceImplFactory(factory: (path: String) -> Resource) {
        jsResourceImplFactory = factory
    }
}

/**
 * `configureWebResources` can be used to override the default configuration.
 *
 * Usage example 1:
 * ```
 *  configureWebResources {
 *     setResourceImplFactory { path -> urlResource("/myApp1/resources/$path") }
 *  }
 * ```
 *  Usage example 2:
 * ```
 *  configureWebResources {
 *     setResourceImplFactory { path -> urlResource("https://mycdn.com/myApp1/res/$path") }
 *  }
 * ```
 *
 * The default resource implementation factory is: `{ urlResource("./$it") }`
 */
@Suppress("unused")
@ExperimentalResourceApi
fun configureWebResources(configure: WebResourcesConfiguration.() -> Unit) {
    WebResourcesConfiguration.configure()
}

/**
 * Creates [Resource] instance. The [Resource] implementation can be changed by using [configureWebResources].
 * By default, it uses [urlResource] under the hood where [path] is relative to the current url segment: `urlResource("./$path")`
 */
@ExperimentalResourceApi
actual fun resource(path: String): Resource = WebResourcesConfiguration.jsResourceImplFactory(path)

/**
 * Creates [Resource] instance accessible by [url]
 */
@ExperimentalResourceApi
fun urlResource(url: String): Resource = JSUrlResourceImpl(url)

@ExperimentalResourceApi
private class JSUrlResourceImpl(url: String) : AbstractResourceImpl(url) {
    override suspend fun readBytes(): ByteArray {
        val response = window.fetch(path).await()
        if (!response.ok) {
            throw MissingResourceException(path)
        }
        return response.arrayBuffer().await().toByteArray()
    }
}

private fun ArrayBuffer.toByteArray() = Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

internal actual fun parseXML(byteArray: ByteArray): Element {
    val xmlString = byteArray.decodeToString()
    val xmlDom = DOMParser().parseFromString(xmlString, "application/xml")
    val domElement = xmlDom.documentElement ?: throw MalformedXMLException("missing documentElement")
    return ElementImpl(domElement)
}

internal actual fun isSyncResourceLoadingSupported() = false

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = throw UnsupportedOperationException()