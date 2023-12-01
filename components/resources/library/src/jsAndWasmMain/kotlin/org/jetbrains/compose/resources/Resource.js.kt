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

/**
 * Represents the configuration object for web resources.
 *
 * @see configureWebResources - for overriding the default configuration.
 */
@Suppress("unused")
@ExperimentalResourceApi
object WebResourcesConfiguration {

    /**
     * An internal default factory method for creating [Resource] from a given path.
     * It can be changed at runtime by using [setResourceFactory].
     */
    @ExperimentalResourceApi
    internal var jsResourceImplFactory: (path: String) -> Resource = { urlResource("./$it") }

    /**
     * Sets a custom factory for the [resource] function to create [Resource] instances.
     * Once set, the [factory] will effectively define the implementation of the [resource] function.
     *
     * @param factory A lambda that accepts a path and produces a [Resource] instance.
     * @see configureWebResources for examples on how to use this function.
     */
    @ExperimentalResourceApi
    fun setResourceFactory(factory: (path: String) -> Resource) {
        jsResourceImplFactory = factory
    }
}

/**
 * Configures the web resources behavior.
 *
 * Allows users to override default behavior and provide custom logic for generating [Resource] instances.
 *
 * @param configure Configuration lambda applied to [WebResourcesConfiguration].
 * @see WebResourcesConfiguration For detailed configuration options.
 *
 * Examples:
 * ```
 *  configureWebResources {
 *     setResourceFactory { path -> urlResource("/myApp1/resources/$path") }
 *  }
 *  configureWebResources {
 *     setResourceFactory { path -> urlResource("https://mycdn.com/myApp1/res/$path") }
 *  }
 * ```
 */
@Suppress("unused")
@ExperimentalResourceApi
fun configureWebResources(configure: WebResourcesConfiguration.() -> Unit) {
    WebResourcesConfiguration.configure()
}

/**
 * Generates a [Resource] instance based on the provided [path].
 *
 * By default, the path is treated as relative to the current URL segment.
 * The default behaviour can be overridden by using [configureWebResources].
 *
 * @param path The path or resource id used to generate the [Resource] instance.
 * @return A [Resource] instance corresponding to the provided path.
 */
@ExperimentalResourceApi
actual fun resource(path: String): Resource = WebResourcesConfiguration.jsResourceImplFactory(path)

/**
 * Creates a [Resource] instance based on the provided [url].
 *
 * @param url The URL used to access the [Resource].
 * @return A [Resource] instance accessible by the given URL.
 */
@ExperimentalResourceApi
fun urlResource(url: String): Resource = JSUrlResourceImpl(url)

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

internal actual fun isSyncResourceLoadingSupported() = false

@OptIn(ExperimentalResourceApi::class)
internal actual fun Resource.readBytesSync(): ByteArray = throw UnsupportedOperationException()

@ExperimentalResourceApi
private class JSUrlResourceImpl(url: String) : AbstractResourceImpl(url) {
    override suspend fun readBytes(): ByteArray {
        val response = window.fetch(path).await<org.w3c.fetch.Response>()
        if (!response.ok) {
            throw MissingResourceException(path)
        }
        return response.arrayBuffer().await<ArrayBuffer>().toByteArray()
    }
}

internal expect fun ArrayBuffer.toByteArray(): ByteArray