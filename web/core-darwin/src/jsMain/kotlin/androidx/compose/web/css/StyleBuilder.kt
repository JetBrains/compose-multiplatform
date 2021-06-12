package org.jetbrains.compose.web.css

import kotlin.properties.ReadOnlyProperty

interface StyleBuilder {
    fun property(propertyName: String, value: StylePropertyValue)
    fun variable(variableName: String, value: StylePropertyValue)

    operator fun <TValue> CSSStyleVariable<TValue>.invoke(value: TValue) {
        variable(this.name, (value as? CustomStyleValue)?.styleValue() ?: value(value.toString()))
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun StyleBuilder.value(value: String) = value.asStylePropertyValue()

@Suppress("NOTHING_TO_INLINE")
inline fun StyleBuilder.value(value: Number) = value.asStylePropertyValue()

@Suppress("NOTHING_TO_INLINE")
inline fun StyleBuilder.value(value: CSSStyleValue) = value.asStylePropertyValue()

fun variableValue(variableName: String, fallback: StylePropertyValue? = null) =
    "var(--$variableName${fallback?.let { ", $it" } ?: ""})".asStylePropertyValue()

interface CSSVariableValue<TValue> : StylePropertyValue {
    companion object {
        operator fun <TValue> invoke(value: String) =
            value.unsafeCast<CSSVariableValue<TValue>>()
        operator fun <TValue> invoke(value: Number) =
            value.unsafeCast<CSSVariableValue<TValue>>()
        operator fun <TValue : CSSStyleValue> invoke(value: TValue) =
            value.unsafeCast<CSSVariableValue<TValue>>()

        operator fun <TValue> invoke(value: StylePropertyValue) =
            value.unsafeCast<CSSVariableValue<TValue>>()
    }
}

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

interface CSSVariable {
    val name: String
}

interface CustomStyleValue {
    fun styleValue(): StylePropertyValue
}

data class CSSStyleVariable<TValue>(override val name: String) : CSSVariable

fun <TValue> CSSStyleVariable<TValue>.value(fallback: TValue? = null) =
    CSSVariableValue<TValue>(
        variableValue(
            name,
            fallback?.let {
                (fallback as? CustomStyleValue)?.styleValue()
                    ?: fallback.toString().asStylePropertyValue()
            }
        )
    )

fun <TValue> CSSVariables.variable() =
    ReadOnlyProperty<Any?, CSSStyleVariable<TValue>> { _, property ->
        CSSStyleVariable(property.name)
    }

interface StyleHolder {
    val properties: StylePropertyList
    val variables: StylePropertyList
}

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
}

data class StylePropertyDeclaration(
    val name: String,
    val value: StylePropertyValue
)
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
