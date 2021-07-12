/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword
import kotlin.properties.ReadOnlyProperty

interface StylePropertyBuilder {
    fun property(propertyName: String, value: StylePropertyValue)

    fun property(propertyName: String, value: String) = property(propertyName, StylePropertyValue(value))
    fun property(propertyName: String, value: Number) = property(propertyName, StylePropertyValue(value))

    fun opacity(value: Number) {
        property("opacity", value)
    }

    fun opacity(value: CSSSizeValue<CSSUnit.percent>) {
        property("opacity", (value.value / 100))
    }

    fun display(displayStyle: DisplayStyle) {
        property("display", displayStyle.value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-attachment
    fun backgroundAttachment(value: String) {
        property("background-attachment", value)
    }

    fun backgroundClip(value: String) {
        property("background-clip", value)
    }

    fun backgroundColor(value: String) {
        property("background-color", value)
    }

    fun backgroundColor(value: CSSColorValue) {
        property("background-color", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-image
    fun backgroundImage(value: String) {
        property("background-image", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-origin
    fun backgroundOrigin(value: String) {
        property("background-origin", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-position
    fun backgroundPosition(value: String) {
        property("background-position", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-repeat
    fun backgroundRepeat(value: String) {
        property("background-repeat", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-size
    fun backgroundSize(value: String) {
        property("background-size", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background
    fun background(value: String) {
        property("background", value)
    }

    fun border(borderBuild: CSSBorder.() -> Unit) {
        property("border", CSSBorder().apply(borderBuild))
    }

    fun border(
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

    fun borderRadius(r: CSSNumeric) {
        property("border-radius", r)
    }

    fun borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
        property("border-radius", "$topLeft $bottomRight")
    }

    fun borderRadius(
        topLeft: CSSNumeric,
        topRightAndBottomLeft: CSSNumeric,
        bottomRight: CSSNumeric
    ) {
        property("border-radius", "$topLeft $topRightAndBottomLeft $bottomRight")
    }

    fun borderRadius(
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

    fun color(value: String) {
        property("color", value)
    }

    fun color(value: CSSColorValue) {
        // color hasn't Typed OM yet
        property("color", value)
    }

    fun flexDirection(flexDirection: FlexDirection) {
        property("flex-direction", flexDirection.value)
    }

    fun flexWrap(flexWrap: FlexWrap) {
        property("flex-wrap", flexWrap.value)
    }

    fun flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
        property(
            "flex-flow",
            "${flexDirection.value} ${flexWrap.value}"
        )
    }

    fun justifyContent(justifyContent: JustifyContent) {
        property(
            "justify-content",
            justifyContent.value
        )
    }
    fun alignSelf(alignSelf: AlignSelf) {
        property(
            "align-self",
            alignSelf.value
        )
    }

    fun alignItems(alignItems: AlignItems) {
        property(
            "align-items",
            alignItems.value
        )
    }

    fun alignContent(alignContent: AlignContent) {
        property(
            "align-content",
            alignContent.value
        )
    }

    fun order(value: Int) {
        property("order", value)
    }

    fun flexGrow(value: Number) {
        property("flex-grow", value)
    }

    fun flexShrink(value: Number) {
        property("flex-shrink", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-family
    fun fontFamily(vararg value: String) {
        property("font-family", value.joinToString(", ") { if (it.contains(" ")) "\"$it\"" else it })
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-size
    fun fontSize(value: CSSNumeric) {
        property("font-size", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-style
    fun fontStyle(value: String) {
        property("font-style", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
    fun fontWeight(value: String) {
        property("font-weight", value)
    }

    fun fontWeight(value: Int) {
        property("font-weight", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/line-height
    fun lineHeight(value: String) {
        property("line-height", value)
    }

    fun lineHeight(value: CSSNumeric) {
        property("line-height", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font
    fun font(value: String) {
        property("font", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-image
    fun listStyleImage(value: String) {
        property("list-style-image", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-position
    fun listStylePosition(value: String) {
        property("list-style-position", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-type
    fun listStyleType(value: String) {
        property("list-style-type", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style
    fun listStyle(value: String) {
        property("list-style", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin
    fun margin(vararg value: CSSNumeric) {
        // margin hasn't Typed OM yet
        property("margin", value.joinToString(" "))
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-bottom
    fun marginBottom(value: CSSNumeric) {
        property("margin-bottom", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-left
    fun marginLeft(value: CSSNumeric) {
        property("margin-left", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-right
    fun marginRight(value: CSSNumeric) {
        property("margin-right", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-top
    fun marginTop(value: CSSNumeric) {
        property("margin-top", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-x
    fun overflowX(value: String) {
        property("overflow-x", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-y
    fun overflowY(value: String) {
        property("overflow-y", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow
    fun overflow(value: String) {
        property("overflow", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding
    fun padding(vararg value: CSSNumeric) {
        // padding hasn't Typed OM yet
        property("padding", value.joinToString(" "))
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-bottom
    fun paddingBottom(value: CSSNumeric) {
        property("padding-bottom", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-left
    fun paddingLeft(value: CSSNumeric) {
        property("padding-left", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-right
    fun paddingRight(value: CSSNumeric) {
        property("padding-right", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-top
    fun paddingTop(value: CSSNumeric) {
        property("padding-top", value)
    }

    fun position(position: Position) {
        property(
            "position",
            position.value
        )
    }

    fun top(value: CSSLengthOrPercentageValue) {
        property("top", value)
    }

    fun top(value: CSSAutoKeyword) {
        property("top", value)
    }

    fun bottom(value: CSSLengthOrPercentageValue) {
        property("bottom", value)
    }

    fun bottom(value: CSSAutoKeyword) {
        property("bottom", value)
    }

    fun left(value: CSSLengthOrPercentageValue) {
        property("left", value)
    }

    fun left(value: CSSAutoKeyword) {
        property("left", value)
    }

    fun right(value: CSSLengthOrPercentageValue) {
        property("right", value)
    }

    fun right(value: CSSAutoKeyword) {
        property("right", value)
    }

    fun width(value: CSSNumeric) {
        property("width", value)
    }

    fun width(value: CSSAutoKeyword) {
        property("width", value)
    }

    fun height(value: CSSNumeric) {
        property("height", value)
    }

    fun height(value: CSSAutoKeyword) {
        property("height", value)
    }
}

interface StyleVariableBuilder {
    fun variable(variableName: String, value: StylePropertyValue)
    fun variable(variableName: String, value: String) = variable(variableName, StylePropertyValue(value))
    fun variable(variableName: String, value: Number) = variable(variableName, StylePropertyValue(value))

    operator fun <TValue: StylePropertyValue> CSSStyleVariable<TValue>.invoke(value: TValue) {
        variable(name, value.toString())
    }

    operator fun CSSStyleVariable<StylePropertyString>.invoke(value: String) {
        variable(name, value)
    }

    operator fun CSSStyleVariable<StylePropertyNumber>.invoke(value: Number) {
        variable(name, value)
    }
}

interface StyleBuilder : StylePropertyBuilder, StyleVariableBuilder

inline fun variableValue(variableName: String, fallback: StylePropertyValue? = null) =
    "var(--$variableName${fallback?.let { ", $it" } ?: ""})"

external interface CSSVariableValueAs<out T: StylePropertyValue>: StylePropertyValue

inline fun <TValue> CSSVariableValue(value: StylePropertyValue) =
    value.unsafeCast<TValue>()

inline fun <TValue> CSSVariableValue(value: String) =
    CSSVariableValue<TValue>(StylePropertyValue(value))

// after adding `variable` word `add` became ambiguous
@Deprecated(
    "use property instead, will remove it soon",
    ReplaceWith("property(propertyName, value)")
)
fun StylePropertyBuilder.add(
    propertyName: String,
    value: StylePropertyValue
) = property(propertyName, value)

interface CSSVariables

interface CSSVariable {
    val name: String
}

class CSSStyleVariable<out TValue: StylePropertyValue>(override val name: String) : CSSVariable

fun <TValue: StylePropertyValue> CSSStyleVariable<TValue>.value(fallback: TValue? = null) =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback
        )
    )

fun <TValue: CSSVariableValueAs<TValue>> CSSStyleVariable<TValue>.value(fallback: TValue? = null) =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback
        )
    )

fun <TValue: StylePropertyValue> CSSVariables.variable() =
    ReadOnlyProperty<Any?, CSSStyleVariable<TValue>> { _, property ->
        CSSStyleVariable(property.name)
    }

interface StyleHolder {
    val properties: StylePropertyList
    val variables: StylePropertyList
}

@Suppress("EqualsOrHashCode")
open class StyleBuilderImpl : StyleBuilder, StyleHolder {
    override val properties: MutableStylePropertyList = mutableListOf()
    override val variables: MutableStylePropertyList = mutableListOf()

    override fun property(propertyName: String, value: StylePropertyValue) {
        properties.add(StylePropertyDeclaration(propertyName, value))
    }

    override fun variable(variableName: String, value: StylePropertyValue) {
        variables.add(StylePropertyDeclaration(variableName, value))
    }

    // StylePropertyValue is js native object without equals
    override fun equals(other: Any?): Boolean {
        return if (other is StyleHolder) {
            properties.nativeEquals(other.properties) &&
                variables.nativeEquals(other.variables)
        } else false
    }

    internal fun copyFrom(sb: StyleBuilderImpl) {
        properties.addAll(sb.properties)
        variables.addAll(sb.variables)
    }
}

data class StylePropertyDeclaration(
    val name: String,
    val value: StylePropertyValue
) {
    constructor(name: String, value: String) : this(name, value.unsafeCast<StylePropertyValue>())
    constructor(name: String, value: Number) : this(name, value.unsafeCast<StylePropertyValue>())
}
typealias StylePropertyList = List<StylePropertyDeclaration>
typealias MutableStylePropertyList = MutableList<StylePropertyDeclaration>

fun StylePropertyList.nativeEquals(properties: StylePropertyList): Boolean {
    if (this.size != properties.size) return false

    var index = 0
    return all { prop ->
        val otherProp = properties[index++]
        prop.name == otherProp.name &&
            prop.value.toString() == otherProp.value.toString()
    }
}
