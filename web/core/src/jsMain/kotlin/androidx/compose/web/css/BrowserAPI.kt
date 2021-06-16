/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "FunctionName")
package org.jetbrains.compose.web.css

import org.w3c.dom.css.*
import org.w3c.dom.css.StyleSheet

inline val StyleSheet.cssRules
    get() = this.asDynamic().cssRules.unsafeCast<CSSRuleList>()


inline fun StyleSheet.deleteRule(index: Int) {
    this.asDynamic().deleteRule(index)
}

fun StyleSheet.insertRule(cssRule: String, index: Int? = null): Int {
    return if (index != null) {
        this.asDynamic().insertRule(cssRule, index).unsafeCast<Int>()
    } else {
        this.asDynamic().insertRule(cssRule).unsafeCast<Int>()
    }
}


inline operator fun CSSRuleList.get(index: Int): CSSRule {
    return this.asDynamic()[index].unsafeCast<CSSRule>()
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> jsObject(): T =
    js("({})")

inline fun <T : Any> jsObject(builder: T.() -> Unit): T =
    jsObject<T>().apply(builder)
