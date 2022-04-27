/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text

import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextSpanParagraphStyleTest {

    @Test
    fun spanStyle_constructor_is_covered_by_TextStyle() {
        val spanStyleParameters = constructorParams(SpanStyle::class).toMutableSet().filter {
            it.name != "platformStyle" && it.name != "textDrawStyle"
        }
        val textStyleParameters = mutableSetOf<Parameter>()

        // In case of multiple constructors, all constructors together should cover
        // all SpanStyle parameters.
        for (constructor in TextStyle::class.constructors) {
            // for every SpanStyle parameter, expecting that parameter to be in TextStyle
            // this guards that if a parameter is added to SpanStyle, it should be added
            // to TextStyle
            textStyleParameters += constructorParams(constructor).toSet().filter {
                it.name != "platformStyle"
            }
        }

        assertThat(textStyleParameters).containsAtLeastElementsIn(spanStyleParameters)
    }

    @Test
    fun spanStyle_properties_is_covered_by_TextStyle() {
        val spanStyleProperties = memberProperties(SpanStyle::class).filter {
            it.name != "platformStyle" && it.name != "textDrawStyle"
        }
        val textStyleProperties = memberProperties(TextStyle::class).filter {
            it.name != "platformStyle"
        }
        assertThat(textStyleProperties).containsAtLeastElementsIn(spanStyleProperties)
    }

    @Test
    fun paragraphStyle_is_covered_by_TextStyle() {
        val paragraphStyleProperties = memberProperties(ParagraphStyle::class).filter {
            it.name != "platformStyle"
        }
        val textStyleProperties = memberProperties(TextStyle::class).filter {
            it.name != "platformStyle"
        }
        assertThat(textStyleProperties).containsAtLeastElementsIn(paragraphStyleProperties)
    }

    @Test
    fun paragraphStyle_properties_is_covered_by_TextStyle() {
        val paragraphStyleProperties = memberProperties(ParagraphStyle::class).filter {
            it.name != "platformStyle"
        }
        val textStyleProperties = memberProperties(TextStyle::class).filter {
            it.name != "platformStyle"
        }
        assertThat(textStyleProperties).containsAtLeastElementsIn(paragraphStyleProperties)
    }

    @Test
    fun textStyle_covered_by_ParagraphStyle_and_SpanStyle() {
        val spanStyleParameters = allConstructorParams(SpanStyle::class).filter {
            it.name != "platformStyle" && it.name != "textDrawStyle"
        }
        val paragraphStyleParameters = allConstructorParams(ParagraphStyle::class).filter {
            it.name != "platformStyle"
        }
        val allParameters = (spanStyleParameters + paragraphStyleParameters).toMutableSet()

        val textStyleParameters = allConstructorParams(TextStyle::class).filter {
            it.name != "platformStyle" && it.name != "spanStyle" && it.name != "paragraphStyle"
        }

        // for every TextStyle parameter, expecting that parameter to be in either ParagraphStyle
        // or SpanStyle
        // this guards that if a parameter is added to TextStyle, it should be added
        // to one of SpanStyle or ParagraphStyle
        assertThat(allParameters).containsAtLeastElementsIn(textStyleParameters)
    }

    @Test
    fun testStyle_properties_is_covered_by_ParagraphStyle_and_SpanStyle() {
        val spanStyleProperties = memberProperties(SpanStyle::class).filter {
            it.name != "platformStyle" && it.name != "textDrawStyle"
        }
        val paragraphStyleProperties = memberProperties(ParagraphStyle::class).filter {
            it.name != "platformStyle"
        }
        val allProperties = spanStyleProperties + paragraphStyleProperties
        val textStyleProperties = memberProperties(TextStyle::class).filter {
            it.name != "spanStyle" && it.name != "paragraphStyle" && it.name != "platformStyle"
        }
        assertThat(allProperties).containsAtLeastElementsIn(textStyleProperties)
    }

    private fun <T : Any> constructorParams(clazz: KClass<T>): List<Parameter> {
        return clazz.primaryConstructor?.let { constructorParams(it) } ?: listOf()
    }

    private fun <T : Any> allConstructorParams(clazz: KClass<T>): Set<Parameter> {
        return clazz.constructors.flatMap { ctor -> constructorParams(ctor) }.toSet()
    }

    private fun <T : Any> constructorParams(constructor: KFunction<T>): List<Parameter> {
        return constructor.parameters.map { Parameter(it) }
    }

    private fun <T : Any> memberProperties(clazz: KClass<T>): Collection<Property> {
        return clazz.memberProperties.map { Property(it) }
    }

    private data class Parameter(
        val name: String?,
        val type: KType,
        val optional: Boolean,
        val isVarArg: Boolean,
        val kind: KParameter.Kind
    ) {
        constructor(parameter: KParameter) : this(
            parameter.name,
            parameter.type,
            parameter.isOptional,
            parameter.isVararg,
            parameter.kind
        )
    }

    private data class Property(
        val name: String?,
        val type: KType
    ) {
        constructor(parameter: KProperty1<*, *>) : this(parameter.name, parameter.returnType)
    }
}
