package androidx.compose.web.css

import androidx.compose.web.css.selectors.CSSSelector

interface CSSStyleRuleBuilder : StyleBuilder

open class CSSRuleBuilderImpl : CSSStyleRuleBuilder, StyleBuilderImpl()

abstract class CSSRuleDeclaration {
    abstract val header: String

    abstract override fun equals(other: Any?): Boolean
}

data class CSSStyleRuleDeclaration(
    val selector: CSSSelector,
    val style: StyleHolder
) : CSSRuleDeclaration() {
    override val header
        get() = selector.toString()
}

abstract class CSSGroupingRuleDeclaration(
    val rules: CSSRuleDeclarationList
) : CSSRuleDeclaration()

typealias CSSRuleDeclarationList = List<CSSRuleDeclaration>
typealias MutableCSSRuleDeclarationList = MutableList<CSSRuleDeclaration>

fun buildCSSStyleRule(cssRule: CSSStyleRuleBuilder.() -> Unit): StyleHolder {
    val builder = CSSRuleBuilderImpl()
    builder.cssRule()
    return builder
}
