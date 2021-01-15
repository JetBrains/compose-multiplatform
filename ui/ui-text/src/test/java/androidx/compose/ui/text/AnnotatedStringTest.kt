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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringTest {
    @Test
    fun normalizedParagraphStyles() {
        val text = "Hello World"
        val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
        val paragraphStyles = listOf(Range(paragraphStyle, 0, 5))
        val annotatedString = AnnotatedString(text = text, paragraphStyles = paragraphStyles)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(
            listOf(
                Range(defaultParagraphStyle.merge(paragraphStyle), 0, 5),
                Range(defaultParagraphStyle, 5, text.length)
            )
        )
    }

    @Test
    fun normalizedParagraphStyles_only_string() {
        val text = "Hello World"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_empty_string() {
        val text = ""
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_with_newLine() {
        val text = "Hello\nWorld"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, text.length)))
    }

    @Test
    fun normalizedParagraphStyles_with_only_lineFeed() {
        val text = "\n"
        val annotatedString = AnnotatedString(text = text)
        val defaultParagraphStyle = ParagraphStyle(lineHeight = 20.sp)

        val paragraphs = annotatedString.normalizedParagraphStyles(defaultParagraphStyle)

        assertThat(paragraphs).isEqualTo(listOf(Range(defaultParagraphStyle, 0, 1)))
    }

    @Test
    fun length_returns_text_length() {
        val text = "abc"
        val annotatedString = AnnotatedString(text)
        assertThat(annotatedString.length).isEqualTo(text.length)
    }

    @Test
    fun plus_operator_creates_a_new_annotated_string() {
        val text1 = "Hello"
        val spanStyles1 = listOf(
            Range(SpanStyle(color = Color.Red), 0, 3),
            Range(SpanStyle(color = Color.Blue), 2, 4)
        )
        val paragraphStyles1 = listOf(
            Range(ParagraphStyle(lineHeight = 20.sp), 0, 1),
            Range(ParagraphStyle(lineHeight = 30.sp), 1, 5)
        )
        val annotations1 = listOf(
            Range("annotation1", 0, 2, "scope1"),
            Range("annotation1", 3, 5, "scope1")
        )
        val annotatedString1 = AnnotatedString(
            text = text1,
            spanStyles = spanStyles1,
            paragraphStyles = paragraphStyles1,
            annotations = annotations1
        )

        val text2 = "World"
        val spanStyle = SpanStyle(color = Color.Cyan)
        val paragraphStyle = ParagraphStyle(lineHeight = 10.sp)
        val annotatedString2 = AnnotatedString(
            text = text2,
            spanStyles = listOf(Range(spanStyle, 0, text2.length)),
            paragraphStyles = listOf(Range(paragraphStyle, 0, text2.length)),
            annotations = listOf(Range("annotation2", 0, text2.length, "scope2"))
        )

        assertThat(annotatedString1 + annotatedString2).isEqualTo(
            AnnotatedString(
                "$text1$text2",
                spanStyles1 + listOf(
                    Range(spanStyle, text1.length, text1.length + text2.length)
                ),
                paragraphStyles1 + listOf(
                    Range(paragraphStyle, text1.length, text1.length + text2.length)
                ),
                annotations1 + listOf(
                    Range("annotation2", text1.length, text1.length + text2.length, "scope2")
                )
            )
        )
    }

    @Test
    fun subSequence_returns_the_correct_string() {
        val annotatedString = AnnotatedString.Builder("abcd").toAnnotatedString()

        assertThat(annotatedString.subSequence(1, 3).text).isEqualTo("bc")
    }

    @Test
    fun subSequence_returns_empty_text_for_start_equals_end() {
        val annotatedString = with(AnnotatedString.Builder()) {
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("a")
            }
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("b")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("c")
            }
            toAnnotatedString()
        }.subSequence(1, 1)

        assertThat(annotatedString).isEqualTo(
            AnnotatedString("", listOf(Range(SpanStyle(fontSize = 12.sp), 0, 0)))
        )
    }

    @Test
    fun subSequence_returns_original_text_for_text_range_is_full_range() {
        val annotatedString = with(AnnotatedString.Builder()) {
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("a")
            }
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("b")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("c")
            }
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence(0, 3)).isSameInstanceAs(annotatedString)
    }

    @Test
    fun subSequence_doesNot_include_styles_before_the_start() {
        val annotatedString = with(AnnotatedString.Builder()) {
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("a")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("b")
            }
            append("c")
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence("ab".length, annotatedString.length)).isEqualTo(
            AnnotatedString("c")
        )
    }

    @Test
    fun subSequence_doesNot_include_styles_after_the_end() {
        val annotatedString = with(AnnotatedString.Builder()) {
            append("a")
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("b")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("c")
            }
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence(0, "a".length)).isEqualTo(
            AnnotatedString("a")
        )
    }

    @Test
    fun subSequence_collapsed_item_with_itemStart_equalTo_rangeStart() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString = with(AnnotatedString.Builder()) {
            append("abc")
            // add collapsed item at the beginning of b
            addStyle(style, 1, 1)
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence(1, 2)).isEqualTo(
            AnnotatedString("b", listOf(Range(style, 0, 0)))
        )
    }

    @Test
    fun subSequence_collapses_included_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString = with(AnnotatedString.Builder()) {
            append("a")
            // will collapse this style in subsequence
            withStyle(style) {
                append("b")
            }
            append("c")
            toAnnotatedString()
        }

        // subsequence with 1,1 will remove text, but include the style
        assertThat(annotatedString.subSequence(1, 1)).isEqualTo(
            AnnotatedString("", listOf(Range(style, 0, 0)))
        )
    }

    @Test
    fun subSequence_collapses_covering_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString = with(AnnotatedString.Builder()) {
            withStyle(style) {
                append("abc")
            }
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence(1, 1)).isEqualTo(
            AnnotatedString("", listOf(Range(style, 0, 0)))
        )
    }

    @Test
    fun subSequence_with_collapsed_range_with_collapsed_item() {
        val style = SpanStyle(fontSize = 12.sp)
        val annotatedString = with(AnnotatedString.Builder()) {
            append("abc")
            // add collapsed item at the beginning of b
            addStyle(style, 1, 1)
            toAnnotatedString()
        }

        assertThat(annotatedString.subSequence(1, 1)).isEqualTo(
            AnnotatedString("", listOf(Range(style, 0, 0)))
        )
    }

    @Test
    fun subSequence_includes_partial_matches() {
        val annotatedString = with(AnnotatedString.Builder()) {
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("ab")
            }
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("c")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("de")
            }
            toAnnotatedString()
        }

        val expectedString = with(AnnotatedString.Builder()) {
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("b")
            }
            withStyle(SpanStyle(fontSize = 12.sp)) {
                append("c")
            }
            withStyle(ParagraphStyle(lineHeight = 14.sp)) {
                append("d")
            }
            toAnnotatedString()
        }

        val subSequence = annotatedString.subSequence("a".length, "abcd".length)

        assertThat(subSequence).isEqualTo(expectedString)
    }

    @Test
    fun subSequence_withAnnotations_noIntersection() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }

        assertThat(annotatedString.subSequence(0, 2)).isEqualTo(AnnotatedString("ab"))
        assertThat(annotatedString.subSequence(4, 6)).isEqualTo(AnnotatedString("ef"))
    }

    @Test
    fun subSequence_withAnnotations_collapsedRange() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }
        // Collapsed range equals to end, no annotation
        assertThat(annotatedString.subSequence(4, 4))
            .isEqualTo(AnnotatedString(""))

        // Collapsed range equals to start, has annotation
        assertThat(annotatedString.subSequence(2, 2))
            .isEqualTo(
                AnnotatedString(
                    "",
                    annotations = listOf(Range("annotation1", 0, 0, "scope1"))
                )
            )

        // Collapsed range covered by annotation, has annotation
        assertThat(annotatedString.subSequence(3, 3))
            .isEqualTo(
                AnnotatedString(
                    "",
                    annotations = listOf(Range("annotation1", 0, 0, "scope1"))
                )
            )
    }

    @Test
    fun subSequence_withAnnotations_hasIntersection() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }
        // Overlapping range, has annotation
        assertThat(annotatedString.subSequence(0, 3))
            .isEqualTo(
                AnnotatedString(
                    "abc",
                    annotations = listOf(Range("annotation1", 2, 3, "scope1"))
                )
            )

        // Overlapping, has annotation
        assertThat(annotatedString.subSequence(3, 5))
            .isEqualTo(
                AnnotatedString(
                    "de",
                    annotations = listOf(Range("annotation1", 0, 1, "scope1"))
                )
            )
    }

    @Test
    fun subSequence_withAnnotations_containsRange() {
        val annotatedString = buildAnnotatedString {
            append("ab")
            pushStringAnnotation("scope1", "annotation1")
            append("cd")
            pop()
            append("ef")
        }

        // Contains range, has annotation
        assertThat(annotatedString.subSequence(0, 5))
            .isEqualTo(
                AnnotatedString(
                    "abcde",
                    annotations = listOf(Range("annotation1", 2, 4, "scope1"))
                )
            )

        // Full range, has annotation
        assertThat(annotatedString.subSequence(2, 4))
            .isEqualTo(
                AnnotatedString(
                    "cd",
                    annotations = listOf(Range("annotation1", 0, 2, "scope1"))
                )
            )
    }

    @Test(expected = IllegalArgumentException::class)
    fun subSequence_throws_exception_for_start_greater_than_end() {
        AnnotatedString("ab").subSequence(1, 0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun subSequence_throws_exception_for_negative_values() {
        AnnotatedString("abc").subSequence(-1, 2)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun subSequence_throws_exception_when_start_is_out_of_bounds_values() {
        val text = "abc"
        AnnotatedString(text).subSequence(text.length, text.length + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun creating_item_with_start_greater_than_end_throws_exception() {
        Range(SpanStyle(color = Color.Red), 1, 0)
    }

    @Test
    fun creating_item_with_start_equal_to_end_does_not_throw_exception() {
        Range(SpanStyle(color = Color.Red), 1, 1)
    }

    @Test
    fun constructor_function_with_single_spanStyle() {
        val text = "a"
        val spanStyle = SpanStyle(color = Color.Red)
        assertThat(
            AnnotatedString(text, spanStyle)
        ).isEqualTo(
            AnnotatedString(text, listOf(Range(spanStyle, 0, text.length)))
        )
    }

    @Test
    fun constructor_function_with_single_paragraphStyle() {
        val text = "a"
        val paragraphStyle = ParagraphStyle(lineHeight = 12.sp)
        assertThat(
            AnnotatedString(text, paragraphStyle)
        ).isEqualTo(
            AnnotatedString(
                text,
                listOf(),
                listOf(Range(paragraphStyle, 0, text.length))
            )
        )
    }

    @Test
    fun constructor_function_with_single_spanStyle_and_paragraphStyle() {
        val text = "a"
        val spanStyle = SpanStyle(color = Color.Red)
        val paragraphStyle = ParagraphStyle(lineHeight = 12.sp)
        assertThat(
            AnnotatedString(text, spanStyle, paragraphStyle)
        ).isEqualTo(
            AnnotatedString(
                text,
                listOf(Range(spanStyle, 0, text.length)),
                listOf(Range(paragraphStyle, 0, text.length))
            )
        )
    }

    @Test
    fun toString_returns_the_plain_string() {
        val text = "abc"
        assertThat(AnnotatedString(text).toString()).isEqualTo(text)
    }
}