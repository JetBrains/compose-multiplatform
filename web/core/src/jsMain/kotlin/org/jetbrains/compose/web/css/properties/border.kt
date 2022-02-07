/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

@Suppress("EqualsOrHashCode")
class CSSBorder : CSSStyleValue {
    var width: CSSNumeric? = null
    var style: LineStyle? = null
    var color: CSSColorValue? = null

    override fun equals(other: Any?): Boolean {
        return if (other is CSSBorder) {
            width == other.width && style == other.style && color == other.color
        } else false
    }

    override fun toString(): String {
        val values = listOfNotNull(width, style, color)
        return values.joinToString(" ")
    }
}

fun CSSBorder.width(size: CSSNumeric) {
    width = size
}

fun CSSBorder.style(style: LineStyle) {
    this.style = style
}

fun CSSBorder.color(color: CSSColorValue) {
    this.color = color
}

fun StyleScope.border(borderBuild: CSSBorder.() -> Unit) {
    property("border", CSSBorder().apply(borderBuild))
}

fun StyleScope.border(
    width: CSSLengthValue? = null,
    style: LineStyle? = null,
    color: CSSColorValue? = null
) {
    border {
        width?.let { width(it) }
        style?.let { style(it) }
        color?.let { color(it) }
    }
}

fun StyleScope.borderRadius(r: CSSNumeric) {
    property("border-radius", r)
}

fun StyleScope.borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    property("border-radius", "$topLeft $bottomRight")
}

fun StyleScope.borderRadius(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    property("border-radius", "$topLeft $topRightAndBottomLeft $bottomRight")
}

fun StyleScope.borderRadius(
    topLeft: CSSNumeric,
    topRight: CSSNumeric,
    bottomRight: CSSNumeric,
    bottomLeft: CSSNumeric
) {
    property(
        "border-radius",
        "$topLeft $topRight $bottomRight $bottomLeft"
    )
}

fun StyleScope.borderWidth(width: CSSNumeric) {
    property("border-width", width)
}

fun StyleScope.borderWidth(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    property("border-width", "$topLeft $bottomRight")
}

fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    property("border-width", "$topLeft $topRightAndBottomLeft $bottomRight")
}

fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRight: CSSNumeric,
    bottomRight: CSSNumeric,
    bottomLeft: CSSNumeric
) {
    property(
        "border-width",
        "$topLeft $topRight $bottomRight $bottomLeft"
    )
}
