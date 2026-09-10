/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

internal const val HtmlNamespace = "http://www.w3.org/1999/xhtml"

internal fun normalizeElementTagName(tagName: String, namespace: String): String =
    if (namespace == HtmlNamespace) tagName.asciiLowercase() else tagName

fun interface ElementBuilder<TElement : Element> {
    /** The HTML tag name used for string rendering. DOM-only lambda builders may omit it. */
    val tagName: String
        get() = error(
            "String rendering requires a tag name. " +
                "Override ElementBuilder.tagName or use TagElement(tagName, ...)."
        )

    fun create(): TElement

    companion object {
        // HTML-only cache inspection for tests; namespace caching is tested through createBuilder.
        internal val buildersCache: Map<String, ElementBuilder<*>>
            get() = platformElementBuildersCache

        fun <TElement : Element> createBuilder(
            tagName: String
        ): ElementBuilder<TElement> =
            createPlatformElementBuilder(tagName.asciiLowercase())

        @ComposeWebInternalApi
        fun <TElement : Element> createBuilder(
            tagName: String,
            namespace: String,
        ): ElementBuilder<TElement> =
            if (namespace == HtmlNamespace) {
                createBuilder(tagName)
            } else {
                createPlatformElementBuilderNS(tagName, namespace)
            }
    }
}

internal expect val platformElementBuildersCache: Map<String, ElementBuilder<*>>

internal expect fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement>

internal expect fun <TElement : Element> createPlatformElementBuilderNS(
    tagName: String,
    namespace: String,
): ElementBuilder<TElement>
