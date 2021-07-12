package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

interface CSSStyleRuleBuilder : StylePropertyBuilder {
    fun variable(variableName: String, value: StylePropertyValue)
    fun variable(variableName: String, value: String) = variable(variableName, StylePropertyValue(value))
    fun variable(variableName: String, value: Number) = variable(variableName, StylePropertyValue(value))

    operator fun <TValue: StylePropertyValue> CSSStyleVariable<TValue>.invoke(value: TValue) {
        variable(name, value.toString())
    }

    operator fun CSSStyleVariable<StylePropertyString>.invoke(value: String) {
        variable(name, value)
    }

    operator fun CSSStyleVariable<StylePropertyNumber>.invoke(value: Number) {
        variable(name, value)
    }
}

open class CSSRuleBuilderImpl : CSSStyleRuleBuilder, StyleBuilderImpl()

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
        get() = selector.toString()
}

interface CSSGroupingRuleDeclaration: CSSRuleDeclaration {
    val rules: CSSRuleDeclarationList
}

typealias CSSRuleDeclarationList = List<CSSRuleDeclaration>
typealias MutableCSSRuleDeclarationList = MutableList<CSSRuleDeclaration>

fun buildCSSStyleRule(cssRule: CSSStyleRuleBuilder.() -> Unit): StyleHolder {
    val builder = CSSRuleBuilderImpl()
    builder.cssRule()
    return builder
}
