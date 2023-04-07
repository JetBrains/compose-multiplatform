/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-family
fun StyleScope.fontFamily(vararg value: String) {
    property("font-family", value.joinToString(", ") { if (it.contains(" ")) "\"$it\"" else it })
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-size
fun StyleScope.fontSize(value: CSSNumeric) {
    property("font-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-style
fun StyleScope.fontStyle(value: String) {
    property("font-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
fun StyleScope.fontWeight(value: String) {
    property("font-weight", value)
}

fun StyleScope.fontWeight(value: Int) {
    property("font-weight", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/line-height
fun StyleScope.lineHeight(value: String) {
    property("line-height", value)
}

fun StyleScope.lineHeight(value: CSSNumeric) {
    property("line-height", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font
fun StyleScope.font(value: String) {
    property("font", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/letter-spacing
fun StyleScope.letterSpacing(value: String) {
    property("letter-spacing", value)
}

fun StyleScope.letterSpacing(value: CSSNumeric) {
    property("letter-spacing", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-align
fun StyleScope.textAlign(value: String) {
    property("text-align", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-color
fun StyleScope.textDecorationColor(value: CSSColorValue) {
    property("text-decoration-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-style
fun StyleScope.textDecorationStyle(value: String) {
    property("text-decoration-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-thickness
fun StyleScope.textDecorationThickness(value: String) {
    property("text-decoration-thickness", value)
}

fun StyleScope.textDecorationThickness(value: CSSNumeric) {
    property("text-decoration-thickness", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-line
fun StyleScope.textDecorationLine(value: String) {
    property("text-decoration-line", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration
fun StyleScope.textDecoration(value: String) {
    property("text-decoration", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/white-space
fun StyleScope.whiteSpace(value: String) {
    property("white-space", value)
}