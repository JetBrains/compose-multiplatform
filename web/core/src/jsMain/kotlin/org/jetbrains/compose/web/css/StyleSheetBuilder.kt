package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

interface CSSRulesHolder {
    val cssRules: CSSRuleDeclarationList
    fun add(cssRule: CSSRuleDeclaration)
    fun add(selector: CSSSelector, style: StyleHolder) {
        add(CSSStyleRuleDeclaration(selector, style))
    }
}

interface GenericStyleSheetBuilder<TBuilder> : CSSRulesHolder, SelectorsScope {
    fun buildRules(
        rulesBuild: GenericStyleSheetBuilder<TBuilder>.() -> Unit
    ): CSSRuleDeclarationList

    fun style(selector: CSSSelector, cssRule: TBuilder.() -> Unit)

    operator fun CSSSelector.invoke(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    infix fun CSSSelector.style(cssRule: TBuilder.() -> Unit) {
        style(this, cssRule)
    }

    operator fun String.invoke(cssRule: TBuilder.() -> Unit) {
        style(CSSSelector.Raw(this), cssRule)
    }

    infix fun String.style(cssRule: TBuilder.() -> Unit) {
        style(CSSSelector.Raw(this), cssRule)
    }
}

interface SelectorsScope {
    fun selector(selector: String): CSSSelector = CSSSelector.Raw(selector)
    fun combine(vararg selectors: CSSSelector): CSSSelector = CSSSelector.Combine(selectors.toMutableList())

    operator fun CSSSelector.plus(selector: CSSSelector): CSSSelector {
        if (this is CSSSelector.Combine) {
            this.selectors.add(selector)
            return this
        }
        return if (selector is CSSSelector.Combine) {
            selector.selectors.add(0, this)
            selector
        } else {
            combine(this, selector)
        }
    }

    operator fun CSSSelector.plus(selector: String): CSSSelector {
        if (this is CSSSelector.Combine) {
            this.selectors.add(selector(selector))
            return this
        }

        return combine(this, selector(selector))
    }

    fun universal(): CSSSelector = CSSSelector.Universal
    fun type(type: String): CSSSelector = CSSSelector.Type(type)
    fun className(className: String): CSSSelector = CSSSelector.CSSClass(className)
    fun id(id: String): CSSSelector = CSSSelector.Id(id)
    fun attr(
        name: String,
        value: String? = null,
        operator: CSSSelector.Attribute.Operator = CSSSelector.Attribute.Operator.Equals,
        caseSensitive: Boolean = true
    ): CSSSelector = CSSSelector.Attribute(name, value, operator, caseSensitive)
    fun group(vararg selectors: CSSSelector): CSSSelector = CSSSelector.Group(selectors.toList())

    @Deprecated("Replaced with `desc`", ReplaceWith("desc(parent, selected)"))
    fun descendant(parent: CSSSelector, selected: CSSSelector): CSSSelector = desc(parent, selected)
    fun desc(parent: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Descendant(parent, selected)
    fun desc(parent: CSSSelector, selected: String): CSSSelector = desc(parent, selector(selected))
    fun desc(parent: String, selected: CSSSelector): CSSSelector = desc(selector(parent), selected)
    fun desc(parent: String, selected: String): CSSSelector = desc(selector(parent), selector(selected))

    fun child(parent: CSSSelector, selected: CSSSelector): CSSSelector =
        CSSSelector.Child(parent, selected)
    fun sibling(sibling: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Sibling(sibling, selected)
    fun adjacent(sibling: CSSSelector, selected: CSSSelector): CSSSelector = CSSSelector.Adjacent(sibling, selected)

    fun not(selector: CSSSelector): CSSSelector = CSSSelector.PseudoClass.Not(selector)
    fun hover(): CSSSelector = CSSSelector.PseudoClass.hover
    fun hover(selector: CSSSelector): CSSSelector = selector + hover()
}

interface StyleSheetBuilder : CSSRulesHolder, GenericStyleSheetBuilder<CSSStyleRuleBuilder> {
    override fun style(selector: CSSSelector, cssRule: CSSStyleRuleBuilder.() -> Unit) {
        add(selector, buildCSSStyleRule(cssRule))
    }
}

open class StyleSheetBuilderImpl : StyleSheetBuilder {
    override val cssRules: MutableCSSRuleDeclarationList = mutableListOf()

    override fun add(cssRule: CSSRuleDeclaration) {
        cssRules.add(cssRule)
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSStyleRuleBuilder>.() -> Unit) =
        StyleSheetBuilderImpl().apply(rulesBuild).cssRules
}
