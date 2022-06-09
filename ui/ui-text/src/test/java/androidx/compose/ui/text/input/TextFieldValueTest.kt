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

package androidx.compose.ui.text.input

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextFieldValueTest {
    private val defaultSaverScope = SaverScope { true }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_selection() {
        TextFieldValue(text = "", selection = TextRange(-1))
    }

    @Test
    fun aligns_selection_to_the_text_length() {
        val text = "a"
        val textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length + 1))
        assertThat(textFieldValue.selection.collapsed).isTrue()
        assertThat(textFieldValue.selection.max).isEqualTo(textFieldValue.text.length)
    }

    @Test
    fun keep_selection_that_is_less_than_text_length() {
        val text = "a bc"
        val selection = TextRange(0, "a".length)

        val textFieldValue = TextFieldValue(text = text, selection = selection)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.selection).isEqualTo(selection)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_composition() {
        TextFieldValue(text = "", composition = TextRange(-1))
    }

    @Test
    fun aligns_composition_to_text_length() {
        val text = "a"
        val textFieldValue = TextFieldValue(text = text, composition = TextRange(text.length + 1))
        assertThat(textFieldValue.composition?.collapsed).isTrue()
        assertThat(textFieldValue.composition?.max).isEqualTo(textFieldValue.text.length)
    }

    @Test
    fun keep_composition_that_is_less_than_text_length() {
        val text = "a bc"
        val composition = TextRange(0, "a".length)

        val textFieldValue = TextFieldValue(text = text, composition = composition)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.composition).isEqualTo(composition)
    }

    @Test
    fun equals_returns_true_for_same_instance() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(textFieldValue).isEqualTo(textFieldValue)
    }

    @Test
    fun equals_returns_true_for_same_object() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(textFieldValue).isEqualTo(textFieldValue.copy())
    }

    @Test
    fun copy_sets_text_correctly() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        val expected = TextFieldValue(
            text = "b",
            selection = textFieldValue.selection,
            composition = textFieldValue.composition
        )

        assertThat(textFieldValue.copy(text = "b")).isEqualTo(expected)
    }

    @Test
    fun copy_sets_selection_correctly() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        val expected = TextFieldValue(
            text = textFieldValue.text,
            selection = TextRange.Zero,
            composition = textFieldValue.composition
        )

        assertThat(textFieldValue.copy(selection = TextRange.Zero)).isEqualTo(expected)
    }

    @Test
    fun text_and_selection_parameter_constructor_has_null_composition() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1)
        )

        assertThat(textFieldValue.composition).isNull()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_Saver() {
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) { append("1") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("2") }
            withAnnotation(tag = "Tag1", annotation = "Annotation1") { append("3") }
            withAnnotation(VerbatimTtsAnnotation("verbatim1")) { append("4") }
            withAnnotation(tag = "Tag2", annotation = "Annotation2") { append("5") }
            withAnnotation(VerbatimTtsAnnotation("verbatim2")) { append("6") }
            withAnnotation(UrlAnnotation("url1")) { append("7") }
            withAnnotation(UrlAnnotation("url2")) { append("8") }
            withStyle(
                SpanStyle(
                    color = Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.All,
                    fontFeatureSettings = "feature settings",
                    letterSpacing = 2.em,
                    baselineShift = BaselineShift.Superscript,
                    textGeometricTransform = TextGeometricTransform(2f, 3f),
                    localeList = LocaleList(
                        Locale("sr-Latn-SR"),
                        Locale("sr-Cyrl-SR"),
                        Locale.current
                    ),
                    background = Color.Blue,
                    textDecoration = TextDecoration.LineThrough,
                    shadow = Shadow(color = Color.Red, offset = Offset(2f, 2f), blurRadius = 4f)

                )
            ) {
                append("7")
            }
            withStyle(
                ParagraphStyle(
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 10.sp,
                    textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp)
                )
            ) {
                append("8")
            }
        }

        val original = TextFieldValue(
            annotatedString = annotatedString,
            selection = TextRange(1, 2),
            composition = TextRange(3, 4)
        )

        val saved = with(TextFieldValue.Saver) { defaultSaverScope.save(original) }
        val restored = TextFieldValue.Saver.restore(saved!!)

        assertThat(restored).isEqualTo(
            TextFieldValue(original.annotatedString, original.selection)
        )
    }

    @Test
    fun test_Saver_defaultInstance() {
        val original = TextFieldValue()
        val saved = with(TextFieldValue.Saver) { defaultSaverScope.save(original) }
        val restored = TextFieldValue.Saver.restore(saved!!)

        assertThat(restored).isEqualTo(original)
    }
}