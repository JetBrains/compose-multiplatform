package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope


internal interface ComposeHtmlContext {
    val supportsDomElementAccess: Boolean

    fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement>

    @Composable
    fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    )

    @Composable
    fun TextElement(value: String)
}

// Define default per platform, e.g. [BrowserComposeHtmlContext] for web
internal expect val DefaultComposeHtmlContext: ComposeHtmlContext

internal val LocalComposeHtmlContext = staticCompositionLocalOf<ComposeHtmlContext> {
    DefaultComposeHtmlContext
}

@Composable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    LocalComposeHtmlContext.current.TagElement(
        elementBuilder = elementBuilder,
        applyAttrs = applyAttrs,
        content = content,
    )
}

/**
 * @param tagName - the name of the tag that needs to be created.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div", ...) else TagElement("span", ...)
 * ```
 */
@Composable
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    val context = LocalComposeHtmlContext.current

    key(tagName) {
        context.TagElement(
            elementBuilder = context.elementBuilder(tagName),
            applyAttrs = applyAttrs,
            content = content,
        )
    }
}
