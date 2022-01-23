/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.jetbrains.compose.web.css

import kotlin.properties.ReadOnlyProperty
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

/**
 * StyleBuilder serves for two main purposes. Passed as a builder context (in [AttrsScope]), it
 * makes it possible to:
 * 1. Add inlined css properties to the element (@see [property])
 * 2. Set values to CSS variables (@see [variable])
 */
interface StyleBuilder {
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
    fun variable(variableName: String, value: StylePropertyValue)

    fun property(propertyName: String, value: String) = property(propertyName, StylePropertyValue(value))
    fun property(propertyName: String, value: Number) = property(propertyName, StylePropertyValue(value))
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
fun StyleBuilder.add(
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

interface StyleScope : StyleBuilder, StyleHolder {
    @ComposeWebInternalApi
    fun copyFrom(sb: StyleScope)
}

@Deprecated(
    message = "Renamed to StyleScopeBuilder",
    replaceWith = ReplaceWith("StyleScopeBuilder", "org.jetbrains.compose.web.css.StyleScopeBuilder")
)
typealias StyleBuilderImpl = StyleScopeBuilder


@OptIn(ComposeWebInternalApi::class)
@Suppress("EqualsOrHashCode")
open class StyleScopeBuilder : StyleScope {
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

    override fun copyFrom(sb: StyleScope) {
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

internal fun StylePropertyList.nativeEquals(properties: StylePropertyList): Boolean {
    if (this.size != properties.size) return false

    var index = 0
    return all { prop ->
        val otherProp = properties[index++]
        prop.name == otherProp.name &&
                prop.value.toString() == otherProp.value.toString()
    }
}
