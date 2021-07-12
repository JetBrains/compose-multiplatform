/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StylePropertyBuilder.opacity(value: Number) {
    property("opacity", value)
}

fun StylePropertyBuilder.opacity(value: CSSSizeValue<CSSUnit.percent>) {
    property("opacity", (value.value / 100))
}

fun StylePropertyBuilder.display(displayStyle: DisplayStyle) {
    property("display", displayStyle.value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-attachment
fun StylePropertyBuilder.backgroundAttachment(value: String) {
    property("background-attachment", value)
}

fun StylePropertyBuilder.backgroundClip(value: String) {
    property("background-clip", value)
}

fun StylePropertyBuilder.backgroundColor(value: String) {
    property("background-color", value)
}

fun StylePropertyBuilder.backgroundColor(value: CSSColorValue) {
    property("background-color", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-image
fun StylePropertyBuilder.backgroundImage(value: String) {
    property("background-image", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-origin
fun StylePropertyBuilder.backgroundOrigin(value: String) {
    property("background-origin", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-position
fun StylePropertyBuilder.backgroundPosition(value: String) {
    property("background-position", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-repeat
fun StylePropertyBuilder.backgroundRepeat(value: String) {
    property("background-repeat", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background-size
fun StylePropertyBuilder.backgroundSize(value: String) {
    property("background-size", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/background
fun StylePropertyBuilder.background(value: String) {
    property("background", value)
}

inline fun StylePropertyBuilder.border(crossinline borderBuild: CSSBorder.() -> Unit) {
    property("border", CSSBorder().apply(borderBuild))
}

fun StylePropertyBuilder.border(
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

fun StylePropertyBuilder.borderRadius(r: CSSNumeric) {
    property("border-radius", r)
}

fun StylePropertyBuilder.borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    property("border-radius", "$topLeft $bottomRight")
}

fun StylePropertyBuilder.borderRadius(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    property("border-radius", "$topLeft $topRightAndBottomLeft $bottomRight")
}

fun StylePropertyBuilder.borderRadius(
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

fun StylePropertyBuilder.color(value: String) {
    property("color", value)
}

fun StylePropertyBuilder.color(value: CSSColorValue) {
    // color hasn't Typed OM yet
    property("color", value)
}

fun StylePropertyBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection.value)
}

fun StylePropertyBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap.value)
}

fun StylePropertyBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection.value} ${flexWrap.value}"
    )
}

fun StylePropertyBuilder.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        justifyContent.value
    )
}
fun StylePropertyBuilder.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        alignSelf.value
    )
}

fun StylePropertyBuilder.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        alignItems.value
    )
}

fun StylePropertyBuilder.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        alignContent.value
    )
}

fun StylePropertyBuilder.order(value: Int) {
    property("order", value)
}

fun StylePropertyBuilder.flexGrow(value: Number) {
    property("flex-grow", value)
}

fun StylePropertyBuilder.flexShrink(value: Number) {
    property("flex-shrink", value)
}

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

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-image
fun StylePropertyBuilder.listStyleImage(value: String) {
    property("list-style-image", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-position
fun StylePropertyBuilder.listStylePosition(value: String) {
    property("list-style-position", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-type
fun StylePropertyBuilder.listStyleType(value: String) {
    property("list-style-type", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/list-style
fun StylePropertyBuilder.listStyle(value: String) {
    property("list-style", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin
fun StylePropertyBuilder.margin(vararg value: CSSNumeric) {
    // margin hasn't Typed OM yet
    property("margin", value.joinToString(" "))
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-bottom
fun StylePropertyBuilder.marginBottom(value: CSSNumeric) {
    property("margin-bottom", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-left
fun StylePropertyBuilder.marginLeft(value: CSSNumeric) {
    property("margin-left", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-right
fun StylePropertyBuilder.marginRight(value: CSSNumeric) {
    property("margin-right", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/margin-top
fun StylePropertyBuilder.marginTop(value: CSSNumeric) {
    property("margin-top", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-x
fun StylePropertyBuilder.overflowX(value: String) {
    property("overflow-x", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-y
fun StylePropertyBuilder.overflowY(value: String) {
    property("overflow-y", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/overflow
fun StylePropertyBuilder.overflow(value: String) {
    property("overflow", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding
fun StylePropertyBuilder.padding(vararg value: CSSNumeric) {
    // padding hasn't Typed OM yet
    property("padding", value.joinToString(" "))
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-bottom
fun StylePropertyBuilder.paddingBottom(value: CSSNumeric) {
    property("padding-bottom", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-left
fun StylePropertyBuilder.paddingLeft(value: CSSNumeric) {
    property("padding-left", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-right
fun StylePropertyBuilder.paddingRight(value: CSSNumeric) {
    property("padding-right", value)
}

// https://developer.mozilla.org/en-US/docs/Web/CSS/padding-top
fun StylePropertyBuilder.paddingTop(value: CSSNumeric) {
    property("padding-top", value)
}

fun StylePropertyBuilder.position(position: Position) {
    property(
        "position",
        position.value
    )
}

fun StylePropertyBuilder.top(value: CSSLengthOrPercentageValue) {
    property("top", value)
}

fun StylePropertyBuilder.top(value: CSSAutoKeyword) {
    property("top", value)
}

fun StylePropertyBuilder.bottom(value: CSSLengthOrPercentageValue) {
    property("bottom", value)
}

fun StylePropertyBuilder.bottom(value: CSSAutoKeyword) {
    property("bottom", value)
}

fun StylePropertyBuilder.left(value: CSSLengthOrPercentageValue) {
    property("left", value)
}

fun StylePropertyBuilder.left(value: CSSAutoKeyword) {
    property("left", value)
}

fun StylePropertyBuilder.right(value: CSSLengthOrPercentageValue) {
    property("right", value)
}

fun StylePropertyBuilder.right(value: CSSAutoKeyword) {
    property("right", value)
}

fun StylePropertyBuilder.width(value: CSSNumeric) {
    property("width", value)
}

fun StylePropertyBuilder.width(value: CSSAutoKeyword) {
    property("width", value)
}

fun StylePropertyBuilder.height(value: CSSNumeric) {
    property("height", value)
}

fun StylePropertyBuilder.height(value: CSSAutoKeyword) {
    property("height", value)
}
