/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.compose.web.css

external interface CSSKeywordValue : CSSStyleValue

inline fun CSSKeywordValue(value: String): CSSKeywordValue = CSSStyleValue(value).unsafeCast<CSSKeywordValue>()
