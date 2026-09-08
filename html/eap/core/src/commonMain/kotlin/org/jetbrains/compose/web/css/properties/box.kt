/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StyleScope.width(value: CSSNumeric) {
    property("width", value)
}

fun StyleScope.width(value: CSSAutoKeyword) {
    property("width", value)
}

fun StyleScope.height(value: CSSNumeric) {
    property("height", value)
}

fun StyleScope.height(value: CSSAutoKeyword) {
    property("height", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing
fun StyleScope.boxSizing(value: String) {
    property("box-sizing", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-width
fun StyleScope.outlineWidth(value: String) {
    property("outline-width", value)
}

fun StyleScope.outlineWidth(value: CSSNumeric) {
    property("outline-width", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-color
fun StyleScope.outlineColor(value: CSSColorValue) {
    property("outline-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline-style
fun StyleScope.outlineStyle(value: String) {
    property("outline-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/outline
fun StyleScope.outline(style: String) {
    property("outline", style)
}

fun StyleScope.outline(colorOrStyle: String, styleOrWidth: String) {
    property("outline", "$colorOrStyle $styleOrWidth")
}

fun StyleScope.outline(style: String, width: CSSNumeric) {
    property("outline", "$style $width")
}

fun StyleScope.outline(color: CSSColorValue, style: String, width: String) {
    property("outline", "$color $style $width")
}

fun StyleScope.outline(color: CSSColorValue, style: String, width: CSSNumeric) {
    property("outline", "$color $style $width")
}

fun StyleScope.outline(color: String, style: String, width: String) {
    property("outline", "$color $style $width")
}

fun StyleScope.outline(color: String, style: String, width: CSSNumeric) {
    property("outline", "$color $style $width")
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/min-width
fun StyleScope.minWidth(value: String) {
    property("min-width", value)
}

fun StyleScope.minWidth(value: CSSNumeric) {
    property("min-width", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/max-width
fun StyleScope.maxWidth(value: String) {
    property("max-width", value)
}

fun StyleScope.maxWidth(value: CSSNumeric) {
    property("max-width", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/min-height
fun StyleScope.minHeight(value: String) {
    property("min-height", value)
}

fun StyleScope.minHeight(value: CSSNumeric) {
    property("min-height", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/max-height
fun StyleScope.maxHeight(value: String) {
    property("max-height", value)
}

fun StyleScope.maxHeight(value: CSSNumeric) {
    property("max-height", value)
}