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

// https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing
fun StyleBuilder.boxSizing(value: String) {
    property("box-sizing", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-width
fun StyleBuilder.outlineWidth(value: String) {
    property("outline-width", value)
}

fun StyleBuilder.outlineWidth(value: CSSNumeric) {
    property("outline-width", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-color
fun StyleBuilder.outlineColor(value: String) {
    property("outline-color", value)
}

fun StyleBuilder.outlineColor(value: CSSColorValue) {
    property("outline-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-style
fun StyleBuilder.outlineStyle(value: String) {
    property("outline-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline
fun StyleBuilder.outline(style: String) {
    property("outline", style)
}

fun StyleBuilder.outline(colorOrStyle: String, styleOrWidth: String) {
    property("outline", "$colorOrStyle $styleOrWidth")
}

fun StyleBuilder.outline(style: String, width: CSSNumeric) {
    property("outline", "$style $width")
}

fun StyleBuilder.outline(color: CSSColorValue, style: String, width: String) {
    property("outline", "$color $style $width")
}

fun StyleBuilder.outline(color: CSSColorValue, style: String, width: CSSNumeric) {
    property("outline", "$color $style $width")
}

fun StyleBuilder.outline(color: String, style: String, width: String) {
    property("outline", "$color $style $width")
}

fun StyleBuilder.outline(color: String, style: String, width: CSSNumeric) {
    property("outline", "$color $style $width")
}