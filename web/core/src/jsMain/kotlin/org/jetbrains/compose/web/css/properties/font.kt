/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-family
fun StylePropertyBuilder.fontFamily(vararg value: String) {
    property("font-family", value.joinToString(", ") { if (it.contains(" ")) "\"$it\"" else it })
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-size
fun StylePropertyBuilder.fontSize(value: CSSNumeric) {
    property("font-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-style
fun StylePropertyBuilder.fontStyle(value: String) {
    property("font-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
fun StylePropertyBuilder.fontWeight(value: String) {
    property("font-weight", value)
}

fun StylePropertyBuilder.fontWeight(value: Int) {
    property("font-weight", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/line-height
fun StylePropertyBuilder.lineHeight(value: String) {
    property("line-height", value)
}

fun StylePropertyBuilder.lineHeight(value: CSSNumeric) {
    property("line-height", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font
fun StylePropertyBuilder.font(value: String) {
    property("font", value)
}