package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

fun interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    companion object {
        fun <TElement : Element> createBuilder(
            tagName: String
        ): ElementBuilder<TElement> =
            createPlatformElementBuilder(tagName.lowercase())
    }
}

internal expect fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement>
