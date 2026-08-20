/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

actual interface StylePropertyValue

actual interface StylePropertyNumber : StylePropertyValue
actual interface StylePropertyString : StylePropertyValue

actual interface CSSStyleValue : StylePropertyValue {
    actual override fun toString(): String
}

actual interface CSSVariableValueAs<out T : StylePropertyValue>

@JvmInline
private value class JvmStylePropertyString(
    private val value: String,
) : StylePropertyString {
    override fun toString(): String = value
}

@JvmInline
private value class JvmStylePropertyNumber(
    private val value: Number,
) : StylePropertyNumber {
    override fun toString(): String = value.toString()
}

@JvmInline
private value class JvmCSSStyleValue(
    private val value: String,
) : CSSStyleValue {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createStylePropertyString(value: String): StylePropertyString =
    JvmStylePropertyString(value)

@PublishedApi
internal actual fun createStylePropertyNumber(value: Number): StylePropertyNumber =
    JvmStylePropertyNumber(value)

@PublishedApi
internal actual fun createCSSStyleValue(value: String): CSSStyleValue =
    JvmCSSStyleValue(value)
