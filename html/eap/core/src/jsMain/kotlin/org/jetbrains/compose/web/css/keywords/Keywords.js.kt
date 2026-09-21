/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css.keywords

import org.jetbrains.compose.web.internal.unsafeCast

actual external interface CSSAutoKeyword : org.jetbrains.compose.web.css.CSSKeywordValue

@PublishedApi
internal actual fun createCSSAutoKeyword(value: String): CSSAutoKeyword = value.unsafeCast<CSSAutoKeyword>()
