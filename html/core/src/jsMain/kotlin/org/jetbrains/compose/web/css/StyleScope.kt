/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.attributes.HtmlAttrMarker
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.properties.ReadOnlyProperty

@Deprecated(
    message = "Renamed to StyleScope",
    replaceWith = ReplaceWith("StyleScope", "org.jetbrains.compose.web.css.StyleScope")
)
typealias StyleBuilder = StyleScope

/**
 * StyleScope serves for two main purposes. Passed as a builder context (in [AttrsScope]), it
 * makes it possible to:
 * 1. Add inlined css properties to the element (@see [property])
 * 2. Set values to CSS variables (@see [variable])
 */
@HtmlAttrMarker
interface StyleScope {
    /**
     * Adds arbitrary CSS property to the inline style of the element
     * @param propertyName - the name of css property as [per spec](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference)
     * @param value - the value, it can be either String or specialized type like [CSSNumeric] or [CSSColorValue]
     *
     * Most frequent CSS property values can be set via specialized methods, like [width], [display] etc.
     *
     * Example:
     * ```
     * Div({
     *  style {
     *      property("some-exotic-css-property", "I am a string value")
     *      property("some-exotic-css-property-width", 5.px)
     *  }
     * })
     * ```
     */
    fun property(propertyName: String, value: StylePropertyValue)
    /**
     * Adds arbitrary CSS property to the inline style of the element. By default throws an error for backward compatibility
     * @param propertyName - the name of css property as [per spec](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference)
     * @param value - the value, it can be either String or specialized type like [CSSNumeric] or [CSSColorValue]
     * @param important - the flag which will be passed to property call in CSS
     *
     * Most frequent CSS property values can be set via specialized methods, like [width], [display] etc.
     *
     * Example:
     * ```
     * Div({
     *  style {
     *      property("some-exotic-css-property", "I am a string value", true)
     *      property("some-exotic-css-property-width", 5.px, false)
     *  }
     * })
     * ```
     */
    fun property(propertyName: String, value: StylePropertyValue, important: Boolean): Unit = error("!important is not supported by this implementation")
    fun variable(variableName: String, value: StylePropertyValue)

    fun property(propertyName: String, value: String) = property(propertyName, StylePropertyValue(value))
    fun property(propertyName: String, value: String, important: Boolean) = property(propertyName, StylePropertyValue(value), important)
    fun property(propertyName: String, value: Number) = property(propertyName, StylePropertyValue(value))
    fun property(propertyName: String, value: Number, important: Boolean) = property(propertyName, StylePropertyValue(value), important)
    fun variable(variableName: String, value: String) = variable(variableName, StylePropertyValue(value))
    fun variable(variableName: String, value: Number) = variable(variableName, StylePropertyValue(value))

    operator fun <TValue : StylePropertyValue> CSSStyleVariable<TValue>.invoke(value: TValue) {
        variable(name, value.toString())
    }

    operator fun CSSStyleVariable<StylePropertyString>.invoke(value: String) {
        variable(name, value)
    }

    operator fun CSSStyleVariable<StylePropertyNumber>.invoke(value: Number) {
        variable(name, value)
    }
}

internal inline fun variableValue(variableName: String, fallback: StylePropertyValue? = null) =
    "var(--$variableName${fallback?.let { ", $it" } ?: ""})"

external interface CSSVariableValueAs<out T : StylePropertyValue>

inline fun <TValue> CSSVariableValue(value: StylePropertyValue) =
    value.unsafeCast<TValue>()

inline fun <TValue> CSSVariableValue(value: String) =
    CSSVariableValue<TValue>(StylePropertyValue(value))

// after adding `variable` word `add` became ambiguous
@Deprecated(
    "use property instead, will remove it soon",
    ReplaceWith("property(propertyName, value)")
)
fun StyleScope.add(
    propertyName: String,
    value: StylePropertyValue
) = property(propertyName, value)

interface CSSVariable {
    val name: String
}

class CSSStyleVariable<out TValue : StylePropertyValue>(override val name: String) : CSSVariable

fun <TValue : StylePropertyValue> CSSStyleVariable<TValue>.value(fallback: TValue? = null) =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback
        )
    )

fun <TValue> CSSStyleVariable<TValue>.value(fallback: TValue? = null)
        where TValue : CSSVariableValueAs<TValue>,
              TValue : StylePropertyValue =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback
        )
    )

/**
 * Introduces CSS variable that can be later referred anywhere in [StyleSheet]
 *
 * Example:
 * ```
 * object AppCSSVariables {
 *  val width by variable<CSSUnitValue>()
 *  val stringHeight by variable<StylePropertyString>()
 *  val order by variable<StylePropertyNumber>()
 * }
 *
 * object AppStylesheet : StyleSheet() {
 *    val classWithProperties by style {
 *     AppCSSVariables.width(100.px)
 *     property("width", AppCSSVariables.width.value())
 * }
 *```
 *
 */
fun <TValue : StylePropertyValue> variable() =
    ReadOnlyProperty<Any?, CSSStyleVariable<TValue>> { _, property ->
        CSSStyleVariable(property.name)
    }

interface StyleHolder {
    @ComposeWebInternalApi
    val properties: StylePropertyList
    @ComposeWebInternalApi
    val variables: StylePropertyList
}

@Deprecated(
    message = "Renamed to StyleScopeBuilder",
    replaceWith = ReplaceWith("StyleScopeBuilder", "org.jetbrains.compose.web.css.StyleScopeBuilder")
)
typealias StyleBuilderImpl = StyleScopeBuilder

@Suppress("EqualsOrHashCode")
open class StyleScopeBuilder : StyleScope, StyleHolder {
    override val properties: MutableStylePropertyList = mutableListOf()
    override val variables: MutableStylePropertyList = mutableListOf()

    override fun property(propertyName: String, value: StylePropertyValue, important: Boolean) {
        properties.add(StylePropertyDeclaration(propertyName, value, important))
    }

    override fun property(propertyName: String, value: StylePropertyValue) {
        property(propertyName = propertyName, value = value, important = false)
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

    @ComposeWebInternalApi
    internal fun copyFrom(sb: StyleHolder) {
        properties.addAll(sb.properties)
        variables.addAll(sb.variables)
    }
}

data class StylePropertyDeclaration(
    val name: String,
    val value: StylePropertyValue,
    val important: Boolean,
) {
    constructor(name: String, value: StylePropertyValue) : this(name, value, false)

    constructor(name: String, value: String, important: Boolean) : this(name, value.unsafeCast<StylePropertyValue>(), important)
    constructor(name: String, value: String) : this(name, value, false)

    constructor(name: String, value: Number, important: Boolean) : this(name, value.unsafeCast<StylePropertyValue>(), important)
    constructor(name: String, value: Number) : this(name, value, false)
}
typealias StylePropertyList = List<StylePropertyDeclaration>
typealias MutableStylePropertyList = MutableList<StylePropertyDeclaration>

internal fun StylePropertyList.nativeEquals(properties: StylePropertyList): Boolean {
    if (this.size != properties.size) return false

    var index = 0
    return all { prop ->
        val otherProp = properties[index++]
        prop.name == otherProp.name &&
            prop.important == otherProp.important && prop.value.toString() == otherProp.value.toString()
                
    }
}
