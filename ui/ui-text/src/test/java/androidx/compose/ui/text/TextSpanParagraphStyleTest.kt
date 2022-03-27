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
import com.google.common.truth.Truth.assert_
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@RunWith(JUnit4::class)
class TextSpanParagraphStyleTest {

    @Test
    fun spanStyle_constructor_is_covered_by_TextStyle() {
        val spanStyleParameters = constructorParams(SpanStyle::class)

        for (constructor in TextStyle::class.constructors) {
            val textStyleParameters = constructorParams(constructor)
            // for every SpanStyle parameter, expecting that parameter to be in TextStyle
            // this guards that if a parameter is added to SpanStyle, it should be added
            // to TextStyle
            if (textStyleParameters.containsAll(spanStyleParameters)) return
        }

        assert_().fail()
    }

    @Test
    fun spanStyle_properties_is_covered_by_TextStyle() {
        val spanStyleProperties = memberProperties(SpanStyle::class)
        val textStyleProperties = memberProperties(TextStyle::class)
        assertThat(textStyleProperties).containsAtLeastElementsIn(spanStyleProperties)
    }

    @Test
    fun paragraphStyle_is_covered_by_TextStyle() {
        val paragraphStyleParameters = constructorParams(ParagraphStyle::class)

        for (constructor in TextStyle::class.constructors) {
            val textStyleParameters = constructorParams(constructor)
            // for every ParagraphStyle parameter, expecting that parameter to be in TextStyle
            // this guards that if a parameter is added to ParagraphStyle, it should be added
            // to TextStyle
            if (textStyleParameters.containsAll(paragraphStyleParameters)) return
        }

        assert_().fail()
    }

    @Test
    fun paragraphStyle_properties_is_covered_by_TextStyle() {
        val paragraphStyleProperties = memberProperties(ParagraphStyle::class)
        val textStyleProperties = memberProperties(TextStyle::class)
        assertThat(textStyleProperties).containsAtLeastElementsIn(paragraphStyleProperties)
    }

    @Test
    fun textStyle_covered_by_ParagraphStyle_and_SpanStyle() {
        val spanStyleParameters = constructorParams(SpanStyle::class)
        val paragraphStyleParameters = constructorParams(ParagraphStyle::class)
        val allParameters = spanStyleParameters + paragraphStyleParameters

        for (constructor in TextStyle::class.constructors) {
            val textStyleParameters = constructorParams(constructor)
            // for every TextStyle parameter, expecting that parameter to be in either ParagraphStyle
            // or SpanStyle
            // this guards that if a parameter is added to TextStyle, it should be added
            // to one of SpanStyle or ParagraphStyle
            if (allParameters.containsAll(textStyleParameters) &&
                textStyleParameters.containsAll(allParameters)
            ) return
        }

        assert_().fail()
    }

    @Test
    fun testStyle_properties_is_covered_by_ParagraphStyle_and_SpanStyle() {
        val spanStyleProperties = memberProperties(SpanStyle::class)
        val paragraphStyleProperties = memberProperties(ParagraphStyle::class)
        val allProperties = spanStyleProperties + paragraphStyleProperties
        val textStyleProperties = memberProperties(TextStyle::class).filter {
            it.name != "spanStyle" && it.name != "paragraphStyle"
        }
        assertThat(allProperties).containsAtLeastElementsIn(textStyleProperties)
    }

    private fun <T : Any> constructorParams(clazz: KClass<T>): List<Parameter> {
        return clazz.primaryConstructor?.let { constructorParams(it) } ?: listOf()
    }

    private fun <T : Any> constructorParams(constructor: KFunction<T>): List<Parameter> {
        return constructor.parameters.map { Parameter(it) }.filter {
            // types of platformStyle is different for each of TextStyle/ParagraphStyle/SpanStyle
            "platformStyle" != it.name
        }
    }

    private fun <T : Any> memberProperties(clazz: KClass<T>): Collection<Property> {
        return clazz.memberProperties.map { Property(it) }.filter {
            // types of platformStyle is different for each of TextStyle/ParagraphStyle/SpanStyle
            "platformStyle" != it.name
        }
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
