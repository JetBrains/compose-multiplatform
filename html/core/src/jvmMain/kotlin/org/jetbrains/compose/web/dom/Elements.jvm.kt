package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

private val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

internal actual val platformElementBuildersCache: Map<String, ElementBuilder<*>>
    get() = buildersCache

@Suppress("UNCHECKED_CAST")
internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> = buildersCache.getOrPut(tagName) {
    ElementBuilder<Element> {
        throw UnsupportedOperationException(
            "DOM element creation for <$tagName> is not available on JVM"
        )
    }
} as ElementBuilder<TElement>
