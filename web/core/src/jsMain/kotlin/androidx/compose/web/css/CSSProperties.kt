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

fun StyleBuilder.backgroundColor(value: CSSColorValue) {
    property("background-color", value)
}

fun StyleBuilder.backgroundColor(value: String) {
    property("background-color", value)
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

inline fun StyleBuilder.border(crossinline borderBuild: CSSBorder.() -> Unit) {
    property("border", CSSBorder().apply(borderBuild))
}

fun StyleBuilder.border(
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

fun StyleBuilder.borderRadius(r: CSSNumeric) {
    property("border-radius", r)
}

fun StyleBuilder.borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
    property("border-radius", "$topLeft $bottomRight")
}

fun StyleBuilder.borderRadius(
    topLeft: CSSNumeric,
    topRightAndBottomLeft: CSSNumeric,
    bottomRight: CSSNumeric
) {
    property("border-radius", "$topLeft $topRightAndBottomLeft $bottomRight")
}

fun StyleBuilder.borderRadius(
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

fun StyleBuilder.fontSize(value: CSSNumeric) {
    property("font-size", value)
}

fun StyleBuilder.margin(value: CSSNumeric) {
    // marign hasn't Typed OM yet
    property("margin", value)
}

fun StyleBuilder.marginLeft(value: CSSNumeric) {
    property("margin-left", value)
}

fun StyleBuilder.marginTop(value: CSSNumeric) {
    property("margin-top", value)
}

fun StyleBuilder.padding(value: CSSNumeric) {
    // padding hasn't Typed OM yet
    property("padding", value)
}

@Suppress("EqualsOrHashCode")
data class CSSAnimation(
    val keyframesName: String,
    var duration: List<CSSSizeValue<out CSSUnitTime>>? = null,
    var timingFunction: List<AnimationTimingFunction>? = null,
    var delay: List<CSSSizeValue<out CSSUnitTime>>? = null,
    var iterationCount: List<Int?>? = null,
    var direction: List<AnimationDirection>? = null,
    var fillMode: List<AnimationFillMode>? = null,
    var playState: List<AnimationPlayState>? = null
) : CSSStyleValue {
    override fun toString(): String {
        val values = listOfNotNull(
            keyframesName,
            duration?.joinToString(", "),
            timingFunction?.joinToString(", "),
            delay?.joinToString(", "),
            iterationCount?.joinToString(", ") { it?.toString() ?: "infinite" },
            direction?.joinToString(", "),
            fillMode?.joinToString(", "),
            playState?.joinToString(", ")
        )
        return values.joinToString(" ")
    }
}

inline fun CSSAnimation.duration(vararg values: CSSSizeValue<out CSSUnitTime>) {
    this.duration = values.toList()
}

inline fun CSSAnimation.timingFunction(vararg values: AnimationTimingFunction) {
    this.timingFunction = values.toList()
}

inline fun CSSAnimation.delay(vararg values: CSSSizeValue<out CSSUnitTime>) {
    this.delay = values.toList()
}

inline fun CSSAnimation.iterationCount(vararg values: Int?) {
    this.iterationCount = values.toList()
}

inline fun CSSAnimation.direction(vararg values: AnimationDirection) {
    this.direction = values.toList()
}

inline fun CSSAnimation.fillMode(vararg values: AnimationFillMode) {
    this.fillMode = values.toList()
}

inline fun CSSAnimation.playState(vararg values: AnimationPlayState) {
    this.playState = values.toList()
}

fun StyleBuilder.animation(
    keyframesName: String,
    builder: CSSAnimation.() -> Unit
) {
    val animation = CSSAnimation(keyframesName).apply(builder)
    property("animation", animation)
}

inline fun StyleBuilder.animation(
    keyframes: CSSNamedKeyframes,
    noinline builder: CSSAnimation.() -> Unit
) = animation(keyframes.name, builder)
