package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

internal const val HtmlNamespace = "http://www.w3.org/1999/xhtml"

internal fun normalizeElementTagName(tagName: String, namespace: String): String =
    if (namespace == HtmlNamespace) tagName.lowercase() else tagName

fun interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    companion object {
        // Internal access for platform-parity tests.
        internal val buildersCache: Map<String, ElementBuilder<*>>
            get() = platformElementBuildersCache

        fun <TElement : Element> createBuilder(
            tagName: String
        ): ElementBuilder<TElement> =
            createPlatformElementBuilder(tagName.lowercase())

        @ComposeWebInternalApi
        fun <TElement : Element> createBuilder(
            tagName: String,
            namespace: String,
        ): ElementBuilder<TElement> =
            createPlatformElementBuilderNS(
                tagName = normalizeElementTagName(tagName, namespace),
                namespace = namespace,
            )
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
