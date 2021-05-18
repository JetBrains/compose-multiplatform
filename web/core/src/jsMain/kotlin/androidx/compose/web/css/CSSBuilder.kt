/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.web.css

import androidx.compose.web.css.selectors.CSSSelector

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
        add(selector, style)
    }

    override fun buildRules(rulesBuild: GenericStyleSheetBuilder<CSSBuilder>.() -> Unit) =
        CSSBuilderImpl(root, self, StyleSheetBuilderImpl()).apply(rulesBuild).cssRules
}
