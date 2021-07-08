@file:Suppress("Unused", "NOTHING_TO_INLINE")

package org.jetbrains.compose.web.css

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