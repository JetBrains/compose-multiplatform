/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StyleScope.position(position: Position) {
    property(
        "position",
        position.value
    )
}

fun StyleScope.top(value: CSSLengthOrPercentageValue) {
    property("top", value)
}

fun StyleScope.top(value: CSSAutoKeyword) {
    property("top", value)
}

fun StyleScope.bottom(value: CSSLengthOrPercentageValue) {
    property("bottom", value)
}

fun StyleScope.bottom(value: CSSAutoKeyword) {
    property("bottom", value)
}

fun StyleScope.left(value: CSSLengthOrPercentageValue) {
    property("left", value)
}

fun StyleScope.left(value: CSSAutoKeyword) {
    property("left", value)
}

fun StyleScope.right(value: CSSLengthOrPercentageValue) {
    property("right", value)
}

fun StyleScope.right(value: CSSAutoKeyword) {
    property("right", value)
}

