/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

actual interface CSSKeywordValue : CSSStyleValue

@JvmInline
private value class JvmCSSKeywordValue(
    private val value: String,
) : CSSKeywordValue {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createCSSKeywordValue(value: String): CSSKeywordValue = JvmCSSKeywordValue(value)
