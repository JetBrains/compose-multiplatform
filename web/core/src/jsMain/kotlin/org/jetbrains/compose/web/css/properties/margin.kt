/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-bottom
fun StyleBuilder.margin(vararg value: CSSNumeric) {
    // margin hasn't Typed OM yet
    property("margin", value.joinToString(" "))
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-bottom
fun StyleBuilder.marginBottom(value: CSSNumeric) {
    property("margin-bottom", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-left
fun StyleBuilder.marginLeft(value: CSSNumeric) {
    property("margin-left", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-right
fun StyleBuilder.marginRight(value: CSSNumeric) {
    property("margin-right", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-top
fun StyleBuilder.marginTop(value: CSSNumeric) {
    property("margin-top", value)
}



