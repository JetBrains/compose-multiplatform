/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css.keywords

import org.jetbrains.compose.web.css.CSSKeywordValue

interface CSSAutoKeyword : CSSKeywordValue

@PublishedApi
internal expect fun createCSSAutoKeyword(value: String): CSSAutoKeyword

inline val auto: CSSAutoKeyword
    get() = createCSSAutoKeyword("auto")
