package org.jetbrains.compose.web.dom

import org.w3c.dom.css.CSSGroupingRule
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleRule
import org.jetbrains.compose.web.css.*
import org.w3c.dom.css.CSSStyleSheet

internal fun clearCSSRules(sheet: CSSStyleSheet) {
    repeat(sheet.cssRules.length) {
        sheet.deleteRule(0)
    }
}

internal fun setCSSRules(sheet: CSSStyleSheet, cssRules: CSSRuleDeclarationList) {
    cssRules.forEach { cssRule ->
        sheet.addRule(cssRule)
    }
}

private fun CSSStyleSheet.addRule(cssRule: String): CSSRule? {
    val cssRuleIndex = this.insertRule(cssRule, this.cssRules.length)
    return this.cssRules.item(cssRuleIndex)
}

private fun CSSKeyframesRule.addRule(cssRule: String): CSSRule? {
    appendRule(cssRule)
    return this.cssRules.item(this.cssRules.length - 1)
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

private fun CSSKeyframesRule.addRule(cssRuleDeclaration: CSSKeyframeRuleDeclaration) {
    addRule("${cssRuleDeclaration.header} {}")?.let { cssRule ->
        fillRule(cssRuleDeclaration, cssRule)
    }
}

private fun fillRule(
    cssRuleDeclaration: CSSRuleDeclaration,
    cssRule: CSSRule
) {
    when (cssRuleDeclaration) {
        is CSSStyledRuleDeclaration -> {
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
        is CSSKeyframesRuleDeclaration -> {
            val cssGroupingRule = cssRule.unsafeCast<CSSKeyframesRule>()
            cssRuleDeclaration.keys.forEach { childRuleDeclaration ->
                cssGroupingRule.addRule(childRuleDeclaration)
            }
        }
    }
}

internal fun setProperty(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty(name, value.toString())
}

internal fun setVariable(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty("--$name", value.toString())
}
