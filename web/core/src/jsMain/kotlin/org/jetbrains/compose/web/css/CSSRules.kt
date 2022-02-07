package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

interface CSSStyleRuleBuilder : StyleScope

open class CSSRuleBuilderImpl : CSSStyleRuleBuilder, StyleScopeBuilder()

@Suppress("EqualsOrHashCode")
interface CSSRuleDeclaration {
    val header: String

    override fun equals(other: Any?): Boolean
}

interface CSSStyledRuleDeclaration {
    val style: StyleHolder
}

data class CSSStyleRuleDeclaration(
    val selector: CSSSelector,
    override val style: StyleHolder
) : CSSRuleDeclaration, CSSStyledRuleDeclaration {
    override val header
        get() = selector.asString()
}

interface CSSGroupingRuleDeclaration: CSSRuleDeclaration {
    val rules: CSSRuleDeclarationList
}

typealias CSSRuleDeclarationList = List<CSSRuleDeclaration>
typealias MutableCSSRuleDeclarationList = MutableList<CSSRuleDeclaration>

internal fun buildCSSStyleRule(cssRule: CSSStyleRuleBuilder.() -> Unit): StyleHolder {
    val builder = CSSRuleBuilderImpl()
    builder.cssRule()
    return builder
}
