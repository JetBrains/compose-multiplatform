package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> = ElementBuilder {
    throw UnsupportedOperationException(
        "DOM element creation for <$tagName> is not available on JVM"
    )
}