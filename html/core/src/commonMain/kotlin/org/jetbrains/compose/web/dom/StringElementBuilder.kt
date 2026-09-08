package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

internal class StringElementBuilder<TElement : Element>(
    tagName: String,
    val namespace: String = HtmlNamespace,
) : ElementBuilder<TElement> {
    val tagName: String = normalizeElementTagName(tagName, namespace)

    override fun create(): TElement {
        throw UnsupportedOperationException(
            "String element builder for <$tagName> cannot create a DOM element"
        )
    }
}
