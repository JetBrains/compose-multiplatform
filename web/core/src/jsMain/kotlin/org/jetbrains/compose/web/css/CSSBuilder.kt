package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.selectors.CSSSelector
import org.jetbrains.compose.web.css.selectors.desc

interface CSSBuilder : CSSStyleRuleBuilder, GenericStyleSheetBuilder<CSSBuilder> {
    val root: CSSSelector
    val self: CSSSelector
}

class CSSBuilderImpl(
    override val root: CSSSelector,
    override val self: CSSSelector,
    rulesHolder: CSSRulesHolder
) : CSSRuleBuilderImpl(), CSSBuilder, CSSRulesHolder by rulesHolder {
    override fun style(selector: CSSSelector, cssRule: CSSBuilder.() -> Unit) {
        val resolvedSelector = if (selector.contains(self, true) || selector.contains(root, true)) {
            selector
        } else {
            desc(self, selector)
        }
        val (style, rules) = buildCSS(root, resolvedSelector, cssRule)
        rules.forEach { add(it) }
        add(resolvedSelector, style)
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSBuilder>.() -> Unit) =
        CSSBuilderImpl(root, self, StyleSheetBuilderImpl()).apply(rulesBuild).cssRules
}
