/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "FunctionName")
package org.jetbrains.compose.web.css

expect interface StylePropertyValue

expect interface StylePropertyNumber: StylePropertyValue
expect interface StylePropertyString: StylePropertyValue

@PublishedApi
internal expect fun createStylePropertyString(value: String): StylePropertyString

@PublishedApi
internal expect fun createStylePropertyNumber(value: Number): StylePropertyNumber

internal expect fun createCSSVariableReference(cssText: String): StylePropertyValue

inline fun StylePropertyValue(value: String): StylePropertyString = createStylePropertyString(value)
inline fun StylePropertyValue(value: Number): StylePropertyNumber = createStylePropertyNumber(value)

expect interface CSSStyleValue: StylePropertyValue {
    override fun toString(): String
}

@PublishedApi
internal expect fun createCSSStyleValue(value: String): CSSStyleValue

inline fun CSSStyleValue(value: String): CSSStyleValue = createCSSStyleValue(value)
