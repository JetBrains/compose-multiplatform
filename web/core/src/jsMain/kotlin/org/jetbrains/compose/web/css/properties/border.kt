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

/**
 * https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Deprecated(
    message = "This function has misleading parameter names. Please use explicit parameter names (or use `Replace with` in IDE).",
    replaceWith = ReplaceWith(
        expression = "borderWidth(vertical = topLeft, horizontal = bottomRight)"
    )
)
fun StyleScope.borderWidth(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    borderWidth(vertical = topLeft, horizontal = bottomRight)
}

/**
 * @param unused will be removed in 1.4. It helps to fix the borderWidth overloads with wrong parameters names.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Suppress("UNUSED_PARAMETER")
fun StyleScope.borderWidth(vertical: CSSNumeric, horizontal: CSSNumeric, unused: Unit? = null) {
    property("border-width", "$vertical $horizontal")
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Deprecated(
    message = "This function has misleading parameter names. Please use explicit parameter names (or use `Replace with` in IDE).",
    replaceWith = ReplaceWith(
        expression = "borderWidth(top = topLeft, horizontal = topRightAndBottomLeft, bottom = bottomRight)"
    )
)
fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    borderWidth(top = topLeft, horizontal = topRightAndBottomLeft, bottom = bottomRight)
}

/**
 * @param unused will be removed in 1.4. It helps to fix the borderWidth overloads with wrong parameters names.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Suppress("UNUSED_PARAMETER")
fun StyleScope.borderWidth(top: CSSNumeric, horizontal: CSSNumeric, bottom: CSSNumeric, unused: Unit? = null) {
    property("border-width", "$top $horizontal $bottom")
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Deprecated(
    message = "This function has misleading parameter names. Please use explicit parameter names (or use `Replace with` in IDE).",
    replaceWith = ReplaceWith(
        expression = "borderWidth(top = topLeft, right = topRight, bottom = bottomRight, left = bottomLeft)"
    )
)
fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRight: CSSNumeric,
    bottomRight: CSSNumeric,
    bottomLeft: CSSNumeric
) {
    borderWidth(top = topLeft, right = topRight, bottom = bottomRight, left = bottomLeft)
}

/**
 * @param unused will be removed in 1.4. It helps to fix the borderWidth overloads with wrong parameters names.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 */
@Suppress("UNUSED_PARAMETER")
fun StyleScope.borderWidth(
    top: CSSNumeric,
    right: CSSNumeric,
    bottom: CSSNumeric,
    left: CSSNumeric,
    unused: Unit? = null
) {
    property("border-width", "$top $right $bottom $left")
}
