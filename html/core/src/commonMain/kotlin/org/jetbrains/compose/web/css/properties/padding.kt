/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding
fun StyleScope.padding(vararg value: CSSNumeric) {
    // padding hasn't Typed OM yet
    property("padding", value.joinToString(" "))
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-bottom
fun StyleScope.paddingBottom(value: CSSNumeric) {
    property("padding-bottom", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-left
fun StyleScope.paddingLeft(value: CSSNumeric) {
    property("padding-left", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-right
fun StyleScope.paddingRight(value: CSSNumeric) {
    property("padding-right", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-top
fun StyleScope.paddingTop(value: CSSNumeric) {
    property("padding-top", value)
}