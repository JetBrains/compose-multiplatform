/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StyleBuilder.position(position: Position) {
    property(
        "position",
        position.value
    )
}

fun StyleBuilder.top(value: CSSLengthOrPercentageValue) {
    property("top", value)
}

fun StyleBuilder.top(value: CSSAutoKeyword) {
    property("top", value)
}

fun StyleBuilder.bottom(value: CSSLengthOrPercentageValue) {
    property("bottom", value)
}

fun StyleBuilder.bottom(value: CSSAutoKeyword) {
    property("bottom", value)
}

fun StyleBuilder.left(value: CSSLengthOrPercentageValue) {
    property("left", value)
}

fun StyleBuilder.left(value: CSSAutoKeyword) {
    property("left", value)
}

fun StyleBuilder.right(value: CSSLengthOrPercentageValue) {
    property("right", value)
}

fun StyleBuilder.right(value: CSSAutoKeyword) {
    property("right", value)
}

