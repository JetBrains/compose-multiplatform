/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "FunctionName")
package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.CSSKeyframesRuleExtension
import org.w3c.dom.css.CSSRule
import org.w3c.dom.css.CSSRuleList


internal external class CSSKeyframesRule: CSSRule {
    val name: String
    val cssRules: CSSRuleList
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
internal inline fun CSSKeyframesRule.appendRule(cssRule: String) {
    (this as CSSKeyframesRuleExtension).appendRule(cssRule)
}

//@Suppress("NOTHING_TO_INLINE")
//internal inline fun <T : Any> jsObject(): T =
//    js("({})").unsafeCast<T>()
//
//internal inline fun <T : Any> jsObject(builder: T.() -> Unit): T =
//    jsObject<T>().apply(builder)
