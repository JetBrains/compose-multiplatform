package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

internal const val HtmlNamespace = "http://www.w3.org/1999/xhtml"

internal fun normalizeElementTagName(tagName: String, namespace: String): String =
    if (namespace == HtmlNamespace) tagName.asciiLowercase() else tagName

internal fun String.asciiLowercase(): String = buildString(length) {
    this@asciiLowercase.forEach { character ->
        append(if (character in 'A'..'Z') character.lowercaseChar() else character)
    }
}

fun interface ElementBuilder<TElement : Element> {
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
