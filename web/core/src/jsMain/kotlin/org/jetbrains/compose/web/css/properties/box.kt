/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StyleBuilder.width(value: CSSNumeric) {
    property("width", value)
}

fun StyleBuilder.width(value: CSSAutoKeyword) {
    property("width", value)
}

fun StyleBuilder.height(value: CSSNumeric) {
    property("height", value)
}

fun StyleBuilder.height(value: CSSAutoKeyword) {
    property("height", value)
}