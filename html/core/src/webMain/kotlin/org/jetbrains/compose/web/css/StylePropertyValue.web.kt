/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.unsafeCast

actual external interface StylePropertyValue

actual external interface StylePropertyNumber : StylePropertyValue
actual external interface StylePropertyString : StylePropertyValue

actual external interface CSSStyleValue : StylePropertyValue {
    actual override fun toString(): String
}

actual external interface CSSVariableValueAs<out T : StylePropertyValue>

@PublishedApi
internal actual fun createStylePropertyString(value: String): StylePropertyString =
    value.unsafeCast<StylePropertyString>()

@PublishedApi
internal actual fun createStylePropertyNumber(value: Number): StylePropertyNumber =
    formatCssNumber(value).unsafeCast<StylePropertyNumber>()

internal actual fun createCSSVariableReference(cssText: String): StylePropertyValue =
    cssText.unsafeCast<StylePropertyValue>()

@PublishedApi
internal actual fun createCSSStyleValue(value: String): CSSStyleValue =
    value.unsafeCast<CSSStyleValue>()
