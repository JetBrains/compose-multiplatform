/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.Identifier
import org.jetbrains.kotlin.name.FqName
import java.util.regex.Matcher
import java.util.regex.Pattern

internal fun conversion(builder: ConversionConfiguration.() -> Unit): ConversionConfiguration {
    return ConversionConfiguration().apply(builder)
}

@DslMarker
annotation class ConversionDsl

@ConversionDsl
class ConversionConfiguration {
    val classConversions = arrayListOf<ClassConversion>()

    fun anyClass(builder: AnyClassConversion.() -> Unit) {
        classConversions.add(AnyClassConversion().apply(builder))
    }

    operator fun String.invoke(builder: ExactClassConversion.() -> Unit) {
        classConversions.add(ExactClassConversion(this).apply(builder))
    }
}

@ConversionDsl
sealed class ClassConversion {
    val attributeConversions = arrayListOf<AttributeConversion>()

    abstract fun matchesAny(classNames: Set<String>): Boolean

    fun anyAttribute(builder: AnyAttributeConversion.() -> Unit) {
        attributeConversions.add(AnyAttributeConversion().apply(builder))
    }

    fun anyOf(vararg attributes: String, builder: ExactAttributeConversion.() -> Unit) {
        attributes.forEach { attribute ->
            attributeConversions.add(ExactAttributeConversion(attribute).apply(builder))
        }
    }

    operator fun String.invoke(builder: ExactAttributeConversion.() -> Unit) {
        attributeConversions.add(ExactAttributeConversion(this).apply(builder))
    }
}
class AnyClassConversion : ClassConversion() {
    override fun matchesAny(classNames: Set<String>): Boolean = true
}
class ExactClassConversion(val className: String) : ClassConversion() {
    override fun matchesAny(classNames: Set<String>): Boolean = classNames.contains(className)
}

interface ConversionContext {
    // TODO(jdemeulenaere): Add tagClass here if a converter needs it.
    val attributeName: String
    val attributeValue: String
}

typealias Conversion = ConversionContext.() -> Expression?
typealias ConversionWithMatcher = ConversionContext.(Matcher) -> Expression?

@ConversionDsl
sealed class AttributeConversion {
    val valueConversions = arrayListOf<ValueConversion>()

    abstract fun matches(attributeName: String): Boolean

    fun anyValue(conversion: Conversion) {
        valueConversions.add(AnyValueConversion(conversion))
    }

    fun anyOf(vararg values: String, conversion: Conversion) {
        values.forEach { value ->
            valueConversions.add(ExactValueConversion(value, conversion))
        }
    }

    operator fun String.invoke(conversion: Conversion) {
        valueConversions.add(ExactValueConversion(this, conversion))
    }

    fun pattern(pattern: String, conversion: ConversionWithMatcher) {
        valueConversions.add(PatternValueConversion(pattern, conversion))
    }
}
class AnyAttributeConversion : AttributeConversion() {
    override fun matches(attributeName: String): Boolean = true
}
class ExactAttributeConversion(private val attributeName: String) : AttributeConversion() {
    override fun matches(attributeName: String): Boolean = this.attributeName == attributeName
}

sealed class ValueConversion {
    abstract fun matches(attributeValue: String): Boolean

    abstract fun convert(attributeName: String, attributeValue: String): Expression?
}

abstract class BaseValueConversion(private val conversion: Conversion) : ValueConversion() {
    override fun convert(attributeName: String, attributeValue: String): Expression? {
        return conversion.invoke(object : ConversionContext {
            override val attributeName: String = attributeName
            override val attributeValue: String = attributeValue
        })
    }
}

class AnyValueConversion(conversion: Conversion) : BaseValueConversion(conversion) {
    override fun matches(attributeValue: String): Boolean = true
}
class ExactValueConversion(private val value: String,
                           conversion: Conversion) : BaseValueConversion(conversion) {
    override fun matches(attributeValue: String): Boolean = this.value == attributeValue
}

class PatternValueConversion(pattern: String,
                             private val conversion: ConversionWithMatcher) : ValueConversion() {
    private val pattern = Pattern.compile(pattern)
    override fun matches(attributeValue: String): Boolean = pattern.matcher(attributeValue).matches()

    override fun convert(attributeName: String, attributeValue: String): Expression? {
        return conversion.invoke(object : ConversionContext {
            override val attributeName: String = attributeName
            override val attributeValue: String = attributeValue
        }, pattern.matcher(attributeValue).also { assert(it.matches()) })
    }
}

internal fun String.asIdentifier(import: FqName? = null) = Identifier(this, isNullable = false, imports = import?.let(::listOf) ?: emptyList())
