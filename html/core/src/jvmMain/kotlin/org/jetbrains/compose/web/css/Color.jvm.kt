/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

actual interface CSSColorValue : StylePropertyValue, CSSVariableValueAs<CSSColorValue>

@JvmInline
private value class JvmCSSColorValue(
    private val value: String,
) : CSSColorValue {
    override fun toString(): String = value
}

internal actual fun createCSSColorValue(value: String): CSSColorValue = JvmCSSColorValue(value)
