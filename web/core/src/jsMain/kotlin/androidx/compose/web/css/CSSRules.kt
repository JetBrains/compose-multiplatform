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
