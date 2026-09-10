package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector

interface CSSBuilder : CSSStyleRuleBuilder, GenericStyleSheetBuilder<CSSBuilder> {
    val self: CSSSelector
}

class CSSBuilderImpl(
    private val currentRoot: CSSSelector,
    override val self: CSSSelector,
    rulesHolder: CSSRulesHolder
) : CSSRuleBuilderImpl(), CSSBuilder, CSSRulesHolder by rulesHolder {
    override fun style(selector: CSSSelector, cssRule: CSSBuilder.() -> Unit) {
        val resolvedSelector = if (selector.contains(self) || selector.contains(currentRoot)) {
            selector
        } else {
            desc(self, selector)
        }
        val (style, rules) = buildCSS(currentRoot, resolvedSelector, cssRule)
        rules.forEach { add(it) }
        add(resolvedSelector, style)
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSBuilder>.() -> Unit) =
        CSSBuilderImpl(currentRoot, self, StyleSheetBuilderImpl()).apply(rulesBuild).cssRules
}
