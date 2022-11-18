/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "FunctionName")
package org.jetbrains.compose.web.css

actual interface StylePropertyValue

actual interface StylePropertyNumber: StylePropertyValue
actual interface StylePropertyString: StylePropertyValue

actual interface CSSStyleValue: StylePropertyValue {
    actual override fun toString(): String
}