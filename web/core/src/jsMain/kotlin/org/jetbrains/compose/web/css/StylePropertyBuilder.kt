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

    fun opacity(value: Number)
    fun opacity(value: CSSSizeValue<CSSUnit.percent>)

    fun display(displayStyle: DisplayStyle)

    fun backgroundAttachment(value: String)

    fun backgroundClip(value: String)

    fun backgroundColor(value: String)

    fun backgroundColor(value: CSSColorValue)

    fun backgroundImage(value: String)

    fun backgroundOrigin(value: String)

    fun backgroundPosition(value: String)

    fun backgroundRepeat(value: String)

    fun backgroundSize(value: String)

    fun background(value: String)

    fun border(borderBuild: CSSBorder.() -> Unit)

    fun border(
        width: CSSLengthValue? = null,
        style: LineStyle? = null,
        color: CSSColorValue? = null
    )

    fun borderRadius(r: CSSNumeric)

    fun borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric)

    fun borderRadius(
        topLeft: CSSNumeric,
        topRightAndBottomLeft: CSSNumeric,
        bottomRight: CSSNumeric
    )

    fun borderRadius(
        topLeft: CSSNumeric,
        topRight: CSSNumeric,
        bottomRight: CSSNumeric,
        bottomLeft: CSSNumeric
    )

    fun color(value: String)

    fun color(value: CSSColorValue)

    fun flexDirection(flexDirection: FlexDirection)

    fun flexWrap(flexWrap: FlexWrap)

    fun flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap)

    fun justifyContent(justifyContent: JustifyContent)
    fun alignSelf(alignSelf: AlignSelf)

    fun alignItems(alignItems: AlignItems)

    fun alignContent(alignContent: AlignContent)

    fun order(value: Int)

    fun flexGrow(value: Number)

    fun flexShrink(value: Number)

    fun fontFamily(vararg value: String)

    fun fontSize(value: CSSNumeric)

    fun fontStyle(value: String)

    fun fontWeight(value: String)

    fun fontWeight(value: Int)

    fun lineHeight(value: String)

    fun lineHeight(value: CSSNumeric)

    fun font(value: String)

    fun listStyleImage(value: String)

    fun listStylePosition(value: String)

    fun listStyleType(value: String)

    fun listStyle(value: String)

    fun margin(vararg value: CSSNumeric)

    fun marginBottom(value: CSSNumeric)

    fun marginLeft(value: CSSNumeric)

    fun marginRight(value: CSSNumeric)

    fun marginTop(value: CSSNumeric)

    fun overflowX(value: String)

    fun overflowY(value: String)

    fun overflow(value: String)

    fun padding(vararg value: CSSNumeric)

    fun paddingBottom(value: CSSNumeric)

    fun paddingLeft(value: CSSNumeric)

    fun paddingRight(value: CSSNumeric)

    fun paddingTop(value: CSSNumeric)

    fun position(position: Position)

    fun top(value: CSSLengthOrPercentageValue)

    fun top(value: CSSAutoKeyword)

    fun bottom(value: CSSLengthOrPercentageValue)

    fun bottom(value: CSSAutoKeyword)

    fun left(value: CSSLengthOrPercentageValue)

    fun left(value: CSSAutoKeyword)

    fun right(value: CSSLengthOrPercentageValue)

    fun right(value: CSSAutoKeyword)

    fun width(value: CSSNumeric)

    fun width(value: CSSAutoKeyword)

    fun height(value: CSSNumeric)

    fun height(value: CSSAutoKeyword)
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

    override fun opacity(value: Number) {
        property("opacity", value)
    }

    override fun opacity(value: CSSSizeValue<CSSUnit.percent>) {
        property("opacity", (value.value / 100))
    }

    override fun display(displayStyle: DisplayStyle) {
        property("display", displayStyle.value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-attachment
    override fun backgroundAttachment(value: String) {
        property("background-attachment", value)
    }

    override fun backgroundClip(value: String) {
        property("background-clip", value)
    }

    override fun backgroundColor(value: String) {
        property("background-color", value)
    }

    override fun backgroundColor(value: CSSColorValue) {
        property("background-color", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-image
    override fun backgroundImage(value: String) {
        property("background-image", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-origin
    override fun backgroundOrigin(value: String) {
        property("background-origin", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-position
    override fun backgroundPosition(value: String) {
        property("background-position", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-repeat
    override fun backgroundRepeat(value: String) {
        property("background-repeat", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background-size
    override fun backgroundSize(value: String) {
        property("background-size", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/background
    override fun background(value: String) {
        property("background", value)
    }

    override fun border(borderBuild: CSSBorder.() -> Unit) {
        property("border", CSSBorder().apply(borderBuild))
    }

    override fun border(
        width: CSSLengthValue?,
        style: LineStyle?,
        color: CSSColorValue?
    ) {
        border {
            width?.let { width(it) }
            style?.let { style(it) }
            color?.let { color(it) }
        }
    }

    override fun borderRadius(r: CSSNumeric) {
        property("border-radius", r)
    }

    override fun borderRadius(topLeft: CSSNumeric, bottomRight: CSSNumeric) {
        property("border-radius", "$topLeft $bottomRight")
    }

    override fun borderRadius(
        topLeft: CSSNumeric,
        topRightAndBottomLeft: CSSNumeric,
        bottomRight: CSSNumeric
    ) {
        property("border-radius", "$topLeft $topRightAndBottomLeft $bottomRight")
    }

    override fun borderRadius(
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

    override fun color(value: String) {
        property("color", value)
    }

    override fun color(value: CSSColorValue) {
        // color hasn't Typed OM yet
        property("color", value)
    }

    override fun flexDirection(flexDirection: FlexDirection) {
        property("flex-direction", flexDirection.value)
    }

    override fun flexWrap(flexWrap: FlexWrap) {
        property("flex-wrap", flexWrap.value)
    }

    override fun flexFlow(flexDirection: FlexDirection, flexWrap: FlexWrap) {
        property(
            "flex-flow",
            "${flexDirection.value} ${flexWrap.value}"
        )
    }

    override fun justifyContent(justifyContent: JustifyContent) {
        property(
            "justify-content",
            justifyContent.value
        )
    }
    override fun alignSelf(alignSelf: AlignSelf) {
        property(
            "align-self",
            alignSelf.value
        )
    }

    override fun alignItems(alignItems: AlignItems) {
        property(
            "align-items",
            alignItems.value
        )
    }

    override fun alignContent(alignContent: AlignContent) {
        property(
            "align-content",
            alignContent.value
        )
    }

    override fun order(value: Int) {
        property("order", value)
    }

    override fun flexGrow(value: Number) {
        property("flex-grow", value)
    }

    override fun flexShrink(value: Number) {
        property("flex-shrink", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-family
    override fun fontFamily(vararg value: String) {
        property("font-family", value.joinToString(", ") { if (it.contains(" ")) "\"$it\"" else it })
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-size
    override fun fontSize(value: CSSNumeric) {
        property("font-size", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-style
    override fun fontStyle(value: String) {
        property("font-style", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-weight
    override fun fontWeight(value: String) {
        property("font-weight", value)
    }

    override fun fontWeight(value: Int) {
        property("font-weight", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/line-height
    override fun lineHeight(value: String) {
        property("line-height", value)
    }

    override fun lineHeight(value: CSSNumeric) {
        property("line-height", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/font
    override fun font(value: String) {
        property("font", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-image
    override fun listStyleImage(value: String) {
        property("list-style-image", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-position
    override fun listStylePosition(value: String) {
        property("list-style-position", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-type
    override fun listStyleType(value: String) {
        property("list-style-type", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/list-style
    override fun listStyle(value: String) {
        property("list-style", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin
    override fun margin(vararg value: CSSNumeric) {
        // margin hasn't Typed OM yet
        property("margin", value.joinToString(" "))
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-bottom
    override fun marginBottom(value: CSSNumeric) {
        property("margin-bottom", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-left
    override fun marginLeft(value: CSSNumeric) {
        property("margin-left", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-right
    override fun marginRight(value: CSSNumeric) {
        property("margin-right", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/margin-top
    override fun marginTop(value: CSSNumeric) {
        property("margin-top", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-x
    override fun overflowX(value: String) {
        property("overflow-x", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow-y
    override fun overflowY(value: String) {
        property("overflow-y", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/overflow
    override fun overflow(value: String) {
        property("overflow", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding
    override fun padding(vararg value: CSSNumeric) {
        // padding hasn't Typed OM yet
        property("padding", value.joinToString(" "))
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-bottom
    override fun paddingBottom(value: CSSNumeric) {
        property("padding-bottom", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-left
    override fun paddingLeft(value: CSSNumeric) {
        property("padding-left", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-right
    override fun paddingRight(value: CSSNumeric) {
        property("padding-right", value)
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/padding-top
    override fun paddingTop(value: CSSNumeric) {
        property("padding-top", value)
    }

    override fun position(position: Position) {
        property(
            "position",
            position.value
        )
    }

    override fun top(value: CSSLengthOrPercentageValue) {
        property("top", value)
    }

    override fun top(value: CSSAutoKeyword) {
        property("top", value)
    }

    override fun bottom(value: CSSLengthOrPercentageValue) {
        property("bottom", value)
    }

    override fun bottom(value: CSSAutoKeyword) {
        property("bottom", value)
    }

    override fun left(value: CSSLengthOrPercentageValue) {
        property("left", value)
    }

    override fun left(value: CSSAutoKeyword) {
        property("left", value)
    }

    override fun right(value: CSSLengthOrPercentageValue) {
        property("right", value)
    }

    override fun right(value: CSSAutoKeyword) {
        property("right", value)
    }

    override fun width(value: CSSNumeric) {
        property("width", value)
    }

    override fun width(value: CSSAutoKeyword) {
        property("width", value)
    }

    override fun height(value: CSSNumeric) {
        property("height", value)
    }

    override fun height(value: CSSAutoKeyword) {
        property("height", value)
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
