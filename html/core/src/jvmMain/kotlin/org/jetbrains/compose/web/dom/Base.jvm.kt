package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSRuleDeclarationList

internal actual val DefaultComposeHtmlContext: ComposeHtmlContext = object : ComposeHtmlContext {
    override val supportsDomElementAccess: Boolean = false

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
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        error("HTML rendering implementation is not provided")
    }

    @Composable
    override fun TextElement(value: String) {
        error("HTML rendering implementation is not provided")
    }

    @Composable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        error("HTML rendering implementation is not provided")
    }
}
