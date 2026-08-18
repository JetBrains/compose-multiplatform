package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.builders.*
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleSheetBuilder
import org.jetbrains.compose.web.css.StyleSheetBuilderImpl
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet

internal val Style: ElementBuilder<HTMLStyleElement> = ElementBuilder.createBuilder("style")

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param cssRules - is a list of style rules.
 * Usually, it's [androidx.compose.web.css.StyleSheet] instance
 */
@Composable
fun Style(
    applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)? = null,
    cssRules: CSSRuleDeclarationList
) {
    TagElement(
        elementBuilder = Style,
        applyAttrs = {
            if (applyAttrs != null) {
                applyAttrs()
            }
        },
    ) {
        DisposableEffect(cssRules, cssRules.size) {
            val cssStylesheet = scopeElement.sheet as? CSSStyleSheet
            cssStylesheet?.setCSSRules(cssRules)
            onDispose {
                cssStylesheet?.clearCSSRules()
            }
        }
    }
}

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param rulesBuild allows to define the style rules using [StyleSheetBuilder]
 */
@Composable
inline fun Style(
    noinline applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)? = null,
    rulesBuild: StyleSheetBuilder.() -> Unit
) {
    val builder = StyleSheetBuilderImpl()
    builder.rulesBuild()
    Style(applyAttrs, builder.cssRules)
}

private fun CSSStyleSheet.clearCSSRules() {
    repeat(cssRules.length) {
        deleteRule(0)
    }
}
