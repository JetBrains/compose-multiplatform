package org.jetbrains.compose.web.dom

import kotlinx.browser.document
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.internal.unsafeCast

private class BrowserElementBuilder<TElement : Element>(
    tagName: String,
    namespace: String? = null,
) : ElementBuilder<TElement> {
    private val prototype: Element by lazy {
        if (namespace == null) {
            document.createElement(tagName)
        } else {
            document.createElementNS(namespace, tagName)
        }
    }

    override fun create(): TElement = prototype.cloneNode().unsafeCast<TElement>()
}

private val htmlBuildersCache = mutableMapOf<String, ElementBuilder<*>>()
private val namespacedBuildersCache = mutableMapOf<Pair<String, String>, ElementBuilder<*>>()

internal actual val platformElementBuildersCache: Map<String, ElementBuilder<*>>
    get() = htmlBuildersCache

internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> =
    htmlBuildersCache.getOrPut(tagName) {
        BrowserElementBuilder<Element>(tagName)
    }.unsafeCast<ElementBuilder<TElement>>()

internal actual fun <TElement : Element> createPlatformElementBuilderNS(
    tagName: String,
    namespace: String,
): ElementBuilder<TElement> =
    namespacedBuildersCache.getOrPut(namespace to tagName) {
        BrowserElementBuilder<Element>(tagName, namespace)
    }.unsafeCast<ElementBuilder<TElement>>()
