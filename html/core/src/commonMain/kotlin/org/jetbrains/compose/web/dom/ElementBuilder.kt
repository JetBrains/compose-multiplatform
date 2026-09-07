package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

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
    }
}

internal expect val platformElementBuildersCache: Map<String, ElementBuilder<*>>

internal expect fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement>
