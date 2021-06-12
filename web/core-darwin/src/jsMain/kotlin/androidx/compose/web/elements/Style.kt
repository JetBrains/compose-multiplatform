package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Tag
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSGroupingRule
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleRule
import org.w3c.dom.css.StyleSheet
import org.jetbrains.compose.web.css.*

/**
 * Use this function to mount the <style> tag into the DOM tree.
 *
 * @param rulesBuild allows to define the style rules using [StyleSheetBuilder]
 */
@Composable
inline fun Style(
    crossinline applyAttrs: AttrsBuilder<Tag.Style>.() -> Unit = {},
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
 * Usually, it's [org.jetbrains.compose.web.css.StyleSheet] instance
 */
@Composable
inline fun Style(
    crossinline applyAttrs: AttrsBuilder<Tag.Style>.() -> Unit = {},
    cssRules: CSSRuleDeclarationList
) {
    TagElement<Tag.Style, HTMLStyleElement>(
        tagName = "style",
        applyAttrs = {
            applyAttrs()
        },
    ) {
        DomSideEffect(cssRules) { style ->
            style.sheet?.let { sheet ->
                setCSSRules(sheet, cssRules)
                onDispose {
                    clearCSSRules(sheet)
                }
            }
        }
    }
}

fun clearCSSRules(sheet: StyleSheet) {
    repeat(sheet.cssRules.length) {
        sheet.deleteRule(0)
    }
}

fun setCSSRules(sheet: StyleSheet, cssRules: CSSRuleDeclarationList) {
    cssRules.forEach { cssRule ->
        sheet.addRule(cssRule)
    }
}

private fun StyleSheet.addRule(cssRule: String): CSSRule {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules[cssRuleIndex]
}

private fun CSSGroupingRule.addRule(cssRule: String): CSSRule {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules[cssRuleIndex]
}

private fun StyleSheet.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    val cssRule = addRule("${cssRuleDeclaration.header} {}")
    fillRule(cssRuleDeclaration, cssRule)
}

private fun CSSGroupingRule.addRule(cssRuleDeclaration: CSSRuleDeclaration) {
    val cssRule = addRule("${cssRuleDeclaration.header} {}")
    fillRule(cssRuleDeclaration, cssRule)
}

private fun fillRule(
    cssRuleDeclaration: CSSRuleDeclaration,
    cssRule: CSSRule
) {
    when (cssRuleDeclaration) {
        is CSSStyleRuleDeclaration -> {
            val cssStyleRule = cssRule.unsafeCast<CSSStyleRule>()
            cssRuleDeclaration.style.properties.forEach { (name, value) ->
                setProperty(cssStyleRule.styleMap, name, value)
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
    styleMap: StylePropertyMap,
    name: String,
    value: StylePropertyValue
) {
    styleMap.set(name, value)
}

fun setVariable(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty("--$name", value.toString())
}
