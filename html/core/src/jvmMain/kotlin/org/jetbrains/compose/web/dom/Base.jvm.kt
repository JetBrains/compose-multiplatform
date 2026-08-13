package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = object : ComposeHtmlContext {
    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        ElementBuilder.createBuilder(tagName)

    @Composable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        error("HTML rendering implementation is not provided")
    }

    @Composable
    override fun TextElement(value: String) {
        error("HTML rendering implementation is not provided")
    }
}
