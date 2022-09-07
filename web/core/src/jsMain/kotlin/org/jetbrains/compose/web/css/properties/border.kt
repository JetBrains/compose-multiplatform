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
 *
 * First argument sets the vertical borders width.
 * Second argument sets the horizontal borders width.
 *
 * NOTE: Temporary deprecation until 2.0 because of wrong parameters names.
 * In 2.0 the parameter names should be fixed. Despite the wrong parameter names, the function behaves correctly.
 */
@Deprecated(
    message = "This function has misleading parameter names. " +
            "Despite that, it behaves correctly (see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width)." +
            "Therefore this function still can be used without specifying parameter names. Replacement is not necessary." +
            " It'll remain Deprecated until 2.0, and in 2.0 parameter names should be changed to: " +
            "`vertical: CSSNumeric` and `horizontal: CSSNumeric`",
    replaceWith = ReplaceWith(
        expression = "borderWidth2(vertical = topLeft, horizontal = bottomRight)"
    )
)
fun StyleScope.borderWidth(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    borderWidth2(topLeft, bottomRight)
}

/**
 * This function is a temporary replacement for `borderWidth(topLeft: CSSNumeric, bottomRight: CSSNumeric)`.
 * After a fix for parameters names in `borderWidth`,
 * this function will be Deprecated with a replaceWith `borderWidth`. Later `borderWidth2` will be Removed.
 * @see borderWidth
 */
fun StyleScope.borderWidth2(vertical: CSSNumeric, horizontal: CSSNumeric) {
    property("border-width", "$vertical $horizontal")
}


/**
 * https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 *
 * First argument sets the top border width.
 * Second argument sets the horizontal borders width.
 * Third argument sets the bottom border width.
 *
 * NOTE: Temporary deprecation until 2.0 because of wrong parameters names.
 * In 2.0 the parameter names should be fixed. Despite the wrong parameter names, the function behaves correctly.
 */
@Deprecated(
    message = "This function has misleading parameter names. " +
            "Despite that, it behaves correctly (see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width)." +
            "Therefore this function still can be used without specifying parameter names. Replacement is not necessary." +
            " It'll remain Deprecated until 2.0, and in 2.0 parameter names should be changed to: " +
            "`top: CSSNumeric`, `horizontal: CSSNumeric`, `bottom: CSSNumeric`",
    replaceWith = ReplaceWith(
        expression = "borderWidth3(top = topLeft, horizontal = topRightAndBottomLeft, bottom = bottomRight)"
    )
)
fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    borderWidth3(topLeft, topRightAndBottomLeft, bottomRight)
}

/**
 * This function is a temporary replacement for `borderWidth(topLeft: CSSNumeric, topRightAndBottomLeft: CSSNumeric, bottomRight: CSSNumeric)`.
 * After a fix for parameters names in `borderWidth`,
 * this function will be Deprecated with a replaceWith `borderWidth`. Later `borderWidth3` will be Removed.
 * @see borderWidth
 */
fun StyleScope.borderWidth3(top: CSSNumeric, horizontal: CSSNumeric, bottom: CSSNumeric) {
    property("border-width", "$top $horizontal $bottom")
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/CSS/border-width
 *
 * First argument sets the top border width.
 * Second argument sets the right border width.
 * Third argument sets the bottom border width.
 * Fourth argument sets the left border width.
 *
 * NOTE: Temporary deprecation until 2.0 because of wrong parameters names.
 * In 2.0 the parameter names should be fixed. Despite the wrong parameter names, the function behaves correctly.
 */
@Deprecated(
    message = "This function has misleading parameter names. " +
            "Despite that, it behaves correctly (see https://developer.mozilla.org/en-US/docs/Web/CSS/border-width)." +
            "Therefore this function still can be used without specifying parameter names. Replacement is not necessary." +
            " It'll remain Deprecated until 2.0, and in 2.0 parameter names should be changed to: " +
            "`top: CSSNumeric`, `right: CSSNumeric`, `bottom: CSSNumeric`, `left: CSSNumeric`",
    replaceWith = ReplaceWith(
        expression = "borderWidth4(top = topLeft, right = topRight, bottom = bottomRight, left = bottomLeft)"
    )
)
fun StyleScope.borderWidth(
    topLeft: CSSNumeric,
    topRight: CSSNumeric,
    bottomRight: CSSNumeric,
    bottomLeft: CSSNumeric
) {
    borderWidth4(topLeft, topRight, bottomRight, bottomLeft)
}

/**
 * This function is a temporary replacement for `borderWidth(topLeft: CSSNumeric, topRight: CSSNumeric, bottomRight: CSSNumeric, bottomLeft: CSSNumeric)`.
 * After a fix for parameters names in `borderWidth`,
 * this function will be Deprecated with a replaceWith `borderWidth`. Later `borderWidth4` will be Removed.
 * @see borderWidth
 */
fun StyleScope.borderWidth4(top: CSSNumeric, right: CSSNumeric, bottom: CSSNumeric, left: CSSNumeric) {
    property("border-width", "$top $right $bottom $left")
}
