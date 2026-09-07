package org.jetbrains.compose.web.dom

import kotlinx.browser.document
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.internal.unsafeCast

private class BrowserElementBuilder<TElement : Element>(
    tagName: String
) : ElementBuilder<TElement> {
    private val prototype: Element by lazy {
        document.createElement(tagName)
    }

    override fun create(): TElement = prototype.cloneNode().unsafeCast<TElement>()
}

private val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

internal actual val platformElementBuildersCache: Map<String, ElementBuilder<*>>
    get() = buildersCache

internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> =
    buildersCache.getOrPut(tagName) {
        BrowserElementBuilder<Element>(tagName)
    }.unsafeCast<ElementBuilder<TElement>>()
