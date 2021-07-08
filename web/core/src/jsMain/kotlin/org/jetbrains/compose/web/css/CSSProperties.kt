@file:Suppress("Unused", "NOTHING_TO_INLINE")

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword

fun StyleBuilder.opacity(value: Number) {
    property("opacity", value)
}

fun StyleBuilder.order(value: Int) {
    property("order", value)
}

fun StyleBuilder.flexGrow(value: Number) {
    property("flex-grow", value)
}

fun StyleBuilder.flexShrink(value: Number) {
    property("flex-shrink", value)
}

fun StyleBuilder.opacity(value: CSSSizeValue<CSSUnit.percent>) {
    property("opacity", (value.value / 100))
}

fun StyleBuilder.color(value: String) {
    property("color", value)
}

fun StyleBuilder.color(value: CSSColorValue) {
    // color hasn't Typed OM yet
    property("color", value)
}

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

inline fun CSSBorder.width(size: CSSNumeric) {
    width = size
}

inline fun CSSBorder.style(style: LineStyle) {
    this.style = style
}

inline fun CSSBorder.color(color: CSSColorValue) {
    this.color = color
}

fun StyleBuilder.display(displayStyle: DisplayStyle) {
    property("display", displayStyle.value)
}

fun StyleBuilder.flexDirection(flexDirection: FlexDirection) {
    property("flex-direction", flexDirection.value)
}

fun StyleBuilder.flexWrap(flexWrap: FlexWrap) {
    property("flex-wrap", flexWrap.value)
}

fun StyleBuilder.flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
    property(
        "flex-flow",
        "${flexDirection.value} ${flexWrap.value}"
    )
}

fun StyleBuilder.justifyContent(justifyContent: JustifyContent) {
    property(
        "justify-content",
        justifyContent.value
    )
}
fun StyleBuilder.alignSelf(alignSelf: AlignSelf) {
    property(
        "align-self",
        alignSelf.value
    )
}

fun StyleBuilder.alignItems(alignItems: AlignItems) {
    property(
        "align-items",
        alignItems.value
    )
}

fun StyleBuilder.alignContent(alignContent: AlignContent) {
    property(
        "align-content",
        alignContent.value
    )
}

fun StyleBuilder.position(position: Position) {
    property(
        "position",
        position.value
    )
}

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

fun StyleBuilder.top(value: CSSLengthOrPercentageValue) {
    property("top", value)
}

fun StyleBuilder.top(value: CSSAutoKeyword) {
    property("top", value)
}

fun StyleBuilder.bottom(value: CSSLengthOrPercentageValue) {
    property("bottom", value)
}

fun StyleBuilder.bottom(value: CSSAutoKeyword) {
    property("bottom", value)
}

fun StyleBuilder.left(value: CSSLengthOrPercentageValue) {
    property("left", value)
}

fun StyleBuilder.left(value: CSSAutoKeyword) {
    property("left", value)
}

fun StyleBuilder.right(value: CSSLengthOrPercentageValue) {
    property("right", value)
}

fun StyleBuilder.right(value: CSSAutoKeyword) {
    property("right", value)
}