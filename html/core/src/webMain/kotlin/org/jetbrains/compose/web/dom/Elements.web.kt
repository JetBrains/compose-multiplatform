package org.jetbrains.compose.web.dom

import kotlinx.browser.document
import kotlinx.browser.dom.Element

private class BrowserElementBuilder<TElement : Element>(
    tagName: String
) : ElementBuilder<TElement> {
    private val prototype: Element by lazy {
        document.createElement(tagName)
    }

    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = prototype.cloneNode() as TElement
}

private val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

@Suppress("UNCHECKED_CAST")
internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> =
    buildersCache.getOrPut(tagName) {
        BrowserElementBuilder<Element>(tagName)
    } as ElementBuilder<TElement>
