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
        val (style, rules) = buildCSS(root, selector, cssRule)
        rules.forEach { add(it) }
        if (selector.contains(self, true) || selector.contains(root, true)) {
            add(selector, style)
        } else {
            add(desc(self, selector), style)
        }
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSBuilder>.() -> Unit) =
        CSSBuilderImpl(root, self, StyleSheetBuilderImpl()).apply(rulesBuild).cssRules
}
