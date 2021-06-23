package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSGroupingRule
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleRule
import org.w3c.dom.css.StyleSheet
import org.jetbrains.compose.web.css.*
import org.w3c.dom.css.CSSStyleSheet

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param rulesBuild allows to define the style rules using [StyleSheetBuilder]
 */
@Composable
inline fun Style(
    crossinline applyAttrs: AttrsBuilder<HTMLStyleElement>.() -> Unit = {},
    rulesBuild: StyleSheetBuilder.() -> Unit
) {
    val builder = StyleSheetBuilderImpl()
    builder.rulesBuild()
    Style(applyAttrs, builder.cssRules)
}

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param cssRules - is a list of style rules.
 * Usually, it's [androidx.compose.web.css.StyleSheet] instance
 */
@Composable
inline fun Style(
    crossinline applyAttrs: AttrsBuilder<HTMLStyleElement>.() -> Unit = {},
    cssRules: CSSRuleDeclarationList
) {
    TagElement(
        elementBuilder = ElementBuilder.Style,
        applyAttrs = {
            applyAttrs()
        },
    ) {
        DomSideEffect(cssRules) { style ->
            (style.sheet as? CSSStyleSheet)?.let { cssStylesheet ->
                setCSSRules(cssStylesheet, cssRules)
                onDispose {
                    clearCSSRules(cssStylesheet)
                }
            }
        }
    }
}

fun clearCSSRules(sheet: CSSStyleSheet) {
    repeat(sheet.cssRules.length) {
        sheet.deleteRule(0)
    }
}

fun setCSSRules(sheet: CSSStyleSheet, cssRules: CSSRuleDeclarationList) {
    cssRules.forEach { cssRule ->
        sheet.addRule(cssRule)
    }
}

private fun CSSStyleSheet.addRule(cssRule: String): CSSRule? {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules.item(cssRuleIndex)
}

private fun CSSStyleSheet.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun CSSGroupingRule.addRule(cssRule: String): CSSRule? {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules.item(cssRuleIndex)
}

private fun CSSGroupingRule.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun fillRule(
    cssRuleDeclaration: CSSRuleDeclaration,
    cssRule: CSSRule
) {
    when (cssRuleDeclaration) {
        is CSSStyleRuleDeclaration -> {
            val cssStyleRule = cssRule.unsafeCast<CSSStyleRule>()
            cssRuleDeclaration.style.properties.forEach { (name, value) ->
                setProperty(cssStyleRule.style, name, value)
            }
            cssRuleDeclaration.style.variables.forEach { (name, value) ->
                setVariable(cssStyleRule.style, name, value)
            }
        }
        is CSSGroupingRuleDeclaration -> {
            val cssGroupingRule = cssRule.unsafeCast<CSSGroupingRule>()
            cssRuleDeclaration.rules.forEach { childRuleDeclaration ->
                cssGroupingRule.addRule(childRuleDeclaration)
            }
        }
    }
}

fun setProperty(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty(name, value.toString())
}

fun setVariable(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty("--$name", value.toString())
}
