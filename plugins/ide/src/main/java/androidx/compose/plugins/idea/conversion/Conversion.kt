/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.idea.conversion

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

    fun anyOf(vararg attributes: String, builder: ExactXmlAttributeConversion.() -> Unit) {
        attributes.forEach { attribute ->
            attributeConversions.add(
                ExactXmlAttributeConversion(
                    attribute
                ).apply(builder))
        }
    }

    operator fun String.invoke(builder: ExactXmlAttributeConversion.() -> Unit) {
        attributeConversions.add(
            ExactXmlAttributeConversion(
                this
            ).apply(builder))
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
    val xmlName: String
    val xmlValue: String
}

sealed class ConversionResult {
    class Success(
        val expression: Expression,
        val kotlinType: FqName
    ) : ConversionResult()

    object Failure : ConversionResult()
}

inline fun <reified T> success(expression: Expression) =
    ConversionResult.Success(
        expression,
        FqName(T::class.qualifiedName!!)
    )
fun success(expression: Expression, kotlinType: FqName) =
    ConversionResult.Success(expression, kotlinType)
fun failure() = ConversionResult.Failure

typealias Conversion = ConversionContext.() -> ConversionResult
typealias ConversionWithMatcher = ConversionContext.(Matcher) -> ConversionResult

@ConversionDsl
sealed class AttributeConversion {
    val valueConversions = arrayListOf<ValueConversion>()

    abstract fun matches(xmlName: String): Boolean

    fun anyValue(conversion: Conversion) {
        valueConversions.add(AnyValueConversion(conversion))
    }

    fun anyOf(vararg values: String, conversion: Conversion) {
        values.forEach { value ->
            valueConversions.add(
                ExactValueConversion(
                    value,
                    conversion
                )
            )
        }
    }

    operator fun String.invoke(conversion: Conversion) {
        valueConversions.add(
            ExactValueConversion(
                this,
                conversion
            )
        )
    }

    fun pattern(pattern: String, conversion: ConversionWithMatcher) {
        valueConversions.add(
            PatternValueConversion(
                pattern,
                conversion
            )
        )
    }
}
class AnyAttributeConversion : AttributeConversion() {
    override fun matches(xmlName: String) = true
}
class ExactXmlAttributeConversion(private val attributeName: String) : AttributeConversion() {
    override fun matches(xmlName: String): Boolean = this.attributeName == xmlName
}

abstract class ValueConversion {
    abstract fun matches(attributeValue: String): Boolean

    abstract fun convert(context: ConversionContext): ConversionResult
}

abstract class BaseValueConversion(private val conversion: Conversion) : ValueConversion() {
    override fun convert(context: ConversionContext): ConversionResult = conversion(context)
}

class AnyValueConversion(conversion: Conversion) : BaseValueConversion(conversion) {
    override fun matches(attributeValue: String): Boolean = true
}
class ExactValueConversion(
    private val value: String,
    conversion: Conversion
) : BaseValueConversion(conversion) {
    override fun matches(attributeValue: String): Boolean = this.value == attributeValue
}

class PatternValueConversion(
    pattern: String,
    private val conversion: ConversionWithMatcher
) : ValueConversion() {
    private val pattern = Pattern.compile(pattern)
    override fun matches(attributeValue: String): Boolean =
        pattern.matcher(attributeValue).matches()

    override fun convert(context: ConversionContext): ConversionResult {
        return context.conversion(pattern.matcher(context.xmlValue).also { assert(it.matches()) })
    }
}

internal fun String.asIdentifier(import: FqName? = null) =
    Identifier(this, isNullable = false, imports = import?.let(::listOf) ?: emptyList())
internal fun String.asIdentifier(import: String): Identifier =
    Identifier(this, isNullable = false, imports = listOf(FqName(import)))
