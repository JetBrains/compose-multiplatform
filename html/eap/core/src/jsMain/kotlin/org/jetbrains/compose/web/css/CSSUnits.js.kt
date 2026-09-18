/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")

package org.jetbrains.compose.web.css

actual external interface CSSNumericValue<T : CSSUnit> :
    StylePropertyValue,
    CSSVariableValueAs<CSSNumericValue<T>>

actual external interface CSSSizeValue<T : CSSUnit> : CSSNumericValue<T> {
    actual val value: Float
    actual val unit: T
}
