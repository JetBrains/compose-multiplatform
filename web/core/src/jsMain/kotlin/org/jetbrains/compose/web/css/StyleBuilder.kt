/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.jetbrains.compose.web.css

import kotlin.properties.ReadOnlyProperty


interface StyleBuilder {
    fun property(propertyName: String, value: StylePropertyValue)
    fun variable(variableName: String, value: StylePropertyValue)

    operator fun <TValue: StylePropertyValue> CSSVariable<TValue>.invoke(value: TValue) {
        variable(name, value.toString())
    }

    operator fun CSSVariable<StylePropertyString>.invoke(value: String) {
        variable(name, value)
    }

    operator fun CSSVariable<StylePropertyNumber>.invoke(value: Number) {
        variable(name, value)
    }
}

fun StyleBuilder.property(propertyName: String, value: String)
        = property(propertyName, value.unsafeCast<StylePropertyValue>())
fun StyleBuilder.property(propertyName: String, value: Number)
        = property(propertyName, value.unsafeCast<StylePropertyValue>())
fun StyleBuilder.variable(variableName: String, value: String)
        = variable(variableName, value.unsafeCast<StylePropertyValue>())
fun StyleBuilder.variable(variableName: String, value: Number)
        = variable(variableName, value.unsafeCast<StylePropertyValue>())


external interface CSSVariableValueAs<out T: StylePropertyValue>: StylePropertyValue

inline fun <TValue> CSSVariableValue(value: StylePropertyValue) = value.unsafeCast<TValue>()
inline fun <TValue> CSSVariableValue(value: String) = value.unsafeCast<TValue>()

// after adding `variable` word `add` became ambiguous
@Deprecated(
    "use property instead, will remove it soon",
    ReplaceWith("property(propertyName, value)")
)
fun StyleBuilder.add(
    propertyName: String,
    value: StylePropertyValue
) = property(propertyName, value)

interface CSSVariables

class CSSVariable<out TValue: StylePropertyValue>(val name: String)

private inline fun <TValue : StylePropertyValue?> variableValue(variableName: String, fallback: TValue? = null) =
    "var(--$variableName${fallback?.let { ", $it" } ?: ""})"

fun <TValue: StylePropertyValue> CSSVariable<TValue>.value(fallback: TValue? = null) =
        variableValue(
            name,
            fallback
        )

fun <TValue: CSSVariableValueAs<TValue>> CSSVariable<TValue>.value(fallback: TValue? = null) =
    variableValue(
        name,
        fallback
    )

fun <TValue: StylePropertyValue> CSSVariables.variable() =
    ReadOnlyProperty<Any?, CSSVariable<TValue>> { _, property ->
        CSSVariable(property.name)
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
