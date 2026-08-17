package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

internal class StringElementBuilder<TElement : Element>(
    tagName: String
) : ElementBuilder<TElement> {
    val tagName: String = tagName.lowercase()

    override fun create(): TElement {
        throw UnsupportedOperationException(
            "String element builder for <$tagName> cannot create a DOM element"
        )
    }
}
