/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-family
fun StyleBuilder.fontFamily(vararg value: String) {
    property("font-family", value.joinToString(", ") { if (it.contains(" ")) "\"$it\"" else it })
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-size
fun StyleBuilder.fontSize(value: CSSNumeric) {
    property("font-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-style
fun StyleBuilder.fontStyle(value: String) {
    property("font-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
fun StyleBuilder.fontWeight(value: String) {
    property("font-weight", value)
}

fun StyleBuilder.fontWeight(value: Int) {
    property("font-weight", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/line-height
fun StyleBuilder.lineHeight(value: String) {
    property("line-height", value)
}

fun StyleBuilder.lineHeight(value: CSSNumeric) {
    property("line-height", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/font
fun StyleBuilder.font(value: String) {
    property("font", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/letter-spacing
fun StyleBuilder.letterSpacing(value: String) {
    property("letter-spacing", value)
}

fun StyleBuilder.letterSpacing(value: CSSNumeric) {
    property("letter-spacing", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-align
fun StyleBuilder.textAlign(value: String) {
    property("text-align", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-color
fun StyleBuilder.textDecorationColor(value: String) {
    property("text-decoration-color", value)
}

fun StyleBuilder.textDecorationColor(value: CSSColorValue) {
    property("text-decoration-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-style
fun StyleBuilder.textDecorationStyle(value: String) {
    property("text-decoration-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-thickness
fun StyleBuilder.textDecorationThickness(value: String) {
    property("text-decoration-thickness", value)
}

fun StyleBuilder.textDecorationThickness(value: CSSNumeric) {
    property("text-decoration-thickness", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration-line
fun StyleBuilder.textDecorationLine(value: String) {
    property("text-decoration-line", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/text-decoration
fun StyleBuilder.textDecoration(value: String) {
    property("text-decoration", value)
}