package org.jetbrains.compose.web.dom

import org.w3c.dom.css.CSSGroupingRule
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleRule
import org.jetbrains.compose.web.css.*
import org.w3c.dom.css.CSSStyleSheet


internal fun CSSStyleSheet.setCSSRules(cssRules: CSSRuleDeclarationList) {
    cssRules.forEach { cssRule ->
        addRule(cssRule)
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
            cssRuleDeclaration.style.properties.forEach { (name, value, important) ->
                setProperty(cssStyleRule.style, name, value, important)
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

fun CSSRuleDeclaration.stringPresentation(
    baseIndent: String = "",
    indent: String = "    ",
    delimiter: String = "\n"
): String {
    val cssRuleDeclaration = this
    val strings = mutableListOf<String>()
    strings.add("$baseIndent${cssRuleDeclaration.header} {")
    when (cssRuleDeclaration) {
        is CSSStyledRuleDeclaration -> {
            cssRuleDeclaration.style.properties.forEach { (name, value, important) ->
                strings.add("$baseIndent$indent$name: $value${if (important) " !important" else ""};")
            }
            cssRuleDeclaration.style.variables.forEach { (name, value) ->
                strings.add("$baseIndent$indent--$name: $value;")
            }
        }
        is CSSGroupingRuleDeclaration -> {
            cssRuleDeclaration.rules.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
        is CSSKeyframesRuleDeclaration -> {
            cssRuleDeclaration.keys.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
    }
    strings.add("$baseIndent}")
    return strings.joinToString(delimiter)
}

internal fun setProperty(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue,
    important: Boolean
) {
    style.setProperty(name, value.toString(), if (important) "important" else "")
}

internal fun setVariable(
    style: CSSStyleDeclaration,
    name: String,
    value: StylePropertyValue
) {
    style.setProperty("--$name", value.toString())
}
