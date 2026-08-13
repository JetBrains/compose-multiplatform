package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.attributes.AttrsScope

typealias AttrBuilderContext<T> = AttrsScope<T>.() -> Unit
typealias ContentBuilder<T> = @Composable ElementScope<T>.() -> Unit

@Composable
fun Div(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null,
) {
    val context = LocalComposeHtmlContext.current
    context.TagElement(
        elementBuilder = context.elementBuilder("div"),
        applyAttrs = attrs,
        content = content,
    )
}

@Composable
fun Span(
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>? = null,
) {
    val context = LocalComposeHtmlContext.current
    context.TagElement(
        elementBuilder = context.elementBuilder("span"),
        applyAttrs = attrs,
        content = content,
    )
}

@Composable
fun Text(value: String) {
    LocalComposeHtmlContext.current.TextElement(value)
}
