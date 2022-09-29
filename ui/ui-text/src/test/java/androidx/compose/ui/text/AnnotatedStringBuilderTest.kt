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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotatedStringBuilderTest {

    @Test
    fun defaultConstructor() {
        val annotatedString = AnnotatedString.Builder().toAnnotatedString()

        assertThat(annotatedString.text).isEmpty()
        assertThat(annotatedString.spanStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun constructorWithString() {
        val text = "a"
        val annotatedString = AnnotatedString.Builder(text).toAnnotatedString()

        assertThat(annotatedString.text).isEqualTo(text)
        assertThat(annotatedString.spanStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun constructorWithAnnotatedString_hasSameAnnotatedStringAttributes() {
        val text = createAnnotatedString(text = "a")
        val annotatedString = AnnotatedString.Builder(text).toAnnotatedString()

        assertThat(annotatedString.text).isEqualTo(text.text)
        assertThat(annotatedString.spanStyles).isEqualTo(text.spanStyles)
        assertThat(annotatedString.paragraphStyles).isEqualTo(text.paragraphStyles)
    }

    @Test
    fun addStyle_withSpanStyle_addsStyle() {
        val style = SpanStyle(color = Color.Red)
        val range = TextRange(0, 1)
        val annotatedString = with(AnnotatedString.Builder("ab")) {
            addStyle(style, range.start, range.end)
            toAnnotatedString()
        }

        val expectedSpanStyles = listOf(
            Range(style, range.start, range.end)
        )

        assertThat(annotatedString.paragraphStyles).isEmpty()
        assertThat(annotatedString.spanStyles).isEqualTo(expectedSpanStyles)
    }

    @Test
    fun addStyle_withParagraphStyle_addsStyle() {
        val style = ParagraphStyle(lineHeight = 30.sp)
        val range = TextRange(0, 1)
        val annotatedString = with(AnnotatedString.Builder("ab")) {
            addStyle(style, range.start, range.end)
            toAnnotatedString()
        }

        val expectedParagraphStyles = listOf(
            Range(style, range.start, range.end)
        )

        assertThat(annotatedString.spanStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    @Test
    fun append_withString_appendsTheText() {
        val text = "a"
        val appendedText = "b"
        val annotatedString = with(AnnotatedString.Builder(text)) {
            append(appendedText)
            toAnnotatedString()
        }

        val expectedString = "$text$appendedText"

        assertThat(annotatedString.text).isEqualTo(expectedString)
        assertThat(annotatedString.spanStyles).isEmpty()
        assertThat(annotatedString.paragraphStyles).isEmpty()
    }

    @Test
    fun append_withString_andMultipleCalls_appendsAllOfTheText() {
        val annotatedString = with(AnnotatedString.Builder("a")) {
            append("b")
            append("c")
            toAnnotatedString()
        }

        assertThat(annotatedString.text).isEqualTo("abc")
    }

    @Test
    fun append_withAnnotatedString_appendsTheText() {
        val color = Color.Red
        val text = "a"
        val lineHeight = 20.sp
        val annotatedString = createAnnotatedString(
            text = text,
            color = color,
            lineHeight = lineHeight
        )

        val appendedColor = Color.Blue
        val appendedText = "b"
        val appendedLineHeight = 30.sp
        val appendedAnnotatedString = createAnnotatedString(
            text = appendedText,
            color = appendedColor,
            lineHeight = appendedLineHeight
        )

        val buildResult = with(AnnotatedString.Builder(annotatedString)) {
            append(appendedAnnotatedString)
            toAnnotatedString()
        }

        val expectedString = "$text$appendedText"
        val expectedSpanStyles = listOf(
            Range(
                item = SpanStyle(color),
                start = 0,
                end = text.length
            ),
            Range(
                item = SpanStyle(appendedColor),
                start = text.length,
                end = expectedString.length
            )
        )

        val expectedParagraphStyles = listOf(
            Range(
                item = ParagraphStyle(lineHeight = lineHeight),
                start = 0,
                end = text.length
            ),
            Range(
                item = ParagraphStyle(lineHeight = appendedLineHeight),
                start = text.length,
                end = expectedString.length
            )
        )

        assertThat(buildResult.text).isEqualTo(expectedString)
        assertThat(buildResult.spanStyles).isEqualTo(expectedSpanStyles)
        assertThat(buildResult.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    @Test
    fun pushStyle() {
        val text = "Test"
        val style = SpanStyle(color = Color.Red)
        val buildResult = AnnotatedString.Builder().apply {
            pushStyle(style)
            append(text)
            pop()
        }.toAnnotatedString()

        assertThat(buildResult.text).isEqualTo(text)
        assertThat(buildResult.spanStyles).hasSize(1)
        assertThat(buildResult.spanStyles[0].item).isEqualTo(style)
        assertThat(buildResult.spanStyles[0].start).isEqualTo(0)
        assertThat(buildResult.spanStyles[0].end).isEqualTo(buildResult.length)
    }

    @Test
    fun pushStyle_without_pop() {
        val styles = arrayOf(
            SpanStyle(color = Color.Red),
            SpanStyle(fontStyle = FontStyle.Italic),
            SpanStyle(fontWeight = FontWeight.Bold)
        )

        val buildResult = with(AnnotatedString.Builder()) {
            styles.forEachIndexed { index, spanStyle ->
                // pop is intentionally not called here
                pushStyle(spanStyle)
                append("Style$index")
            }
            toAnnotatedString()
        }

        assertThat(buildResult.text).isEqualTo("Style0Style1Style2")
        assertThat(buildResult.spanStyles).hasSize(3)

        styles.forEachIndexed { index, spanStyle ->
            assertThat(buildResult.spanStyles[index].item).isEqualTo(spanStyle)
            assertThat(buildResult.spanStyles[index].end).isEqualTo(buildResult.length)
        }

        assertThat(buildResult.spanStyles[0].start).isEqualTo(0)
        assertThat(buildResult.spanStyles[1].start).isEqualTo("Style0".length)
        assertThat(buildResult.spanStyles[2].start).isEqualTo("Style0Style1".length)
    }

    @Test
    fun pushStyle_with_multiple_styles() {
        val spanStyle1 = SpanStyle(color = Color.Red)
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val buildResult = with(AnnotatedString.Builder()) {
            pushStyle(spanStyle1)
            append("Test")
            pushStyle(spanStyle2)
            append(" me")
            pop()
            pop()
            toAnnotatedString()
        }

        assertThat(buildResult.text).isEqualTo("Test me")
        assertThat(buildResult.spanStyles).hasSize(2)

        assertThat(buildResult.spanStyles[0].item).isEqualTo(spanStyle1)
        assertThat(buildResult.spanStyles[0].start).isEqualTo(0)
        assertThat(buildResult.spanStyles[0].end).isEqualTo(buildResult.length)

        assertThat(buildResult.spanStyles[1].item).isEqualTo(spanStyle2)
        assertThat(buildResult.spanStyles[1].start).isEqualTo("Test".length)
        assertThat(buildResult.spanStyles[1].end).isEqualTo(buildResult.length)
    }

    @Test
    fun pushStyle_with_multiple_styles_on_top_of_each_other() {
        val styles = arrayOf(
            SpanStyle(color = Color.Red),
            SpanStyle(fontStyle = FontStyle.Italic),
            SpanStyle(fontWeight = FontWeight.Bold)
        )

        val buildResult = with(AnnotatedString.Builder()) {
            styles.forEach { spanStyle ->
                // pop is intentionally not called here
                pushStyle(spanStyle)
            }
            toAnnotatedString()
        }

        assertThat(buildResult.text).isEmpty()
        assertThat(buildResult.spanStyles).hasSize(3)
        styles.forEachIndexed { index, spanStyle ->
            assertThat(buildResult.spanStyles[index].item).isEqualTo(spanStyle)
            assertThat(buildResult.spanStyles[index].start).isEqualTo(buildResult.length)
            assertThat(buildResult.spanStyles[index].end).isEqualTo(buildResult.length)
        }
    }

    @Test
    fun pushStyle_with_multiple_stacks_should_construct_styles_in_the_same_order() {
        val styles = arrayOf(
            SpanStyle(color = Color.Red),
            SpanStyle(fontStyle = FontStyle.Italic),
            SpanStyle(fontWeight = FontWeight.Bold),
            SpanStyle(letterSpacing = 1.2.em)
        )

        val buildResult = with(AnnotatedString.Builder()) {
            pushStyle(styles[0])
            append("layer1-1")
            pushStyle(styles[1])
            append("layer2-1")
            pushStyle(styles[2])
            append("layer3-1")
            pop()
            pushStyle(styles[3])
            append("layer3-2")
            pop()
            append("layer2-2")
            pop()
            append("layer1-2")
            toAnnotatedString()
        }

        assertThat(buildResult.spanStyles).hasSize(4)
        styles.forEachIndexed { index, spanStyle ->
            assertThat(buildResult.spanStyles[index].item).isEqualTo(spanStyle)
        }
    }

    @Test
    fun pushStyle_with_multiple_nested_styles_should_return_styles_in_same_order() {
        val styles = arrayOf(
            SpanStyle(color = Color.Red),
            SpanStyle(fontStyle = FontStyle.Italic),
            SpanStyle(fontWeight = FontWeight.Bold),
            SpanStyle(letterSpacing = 1.2.em)
        )

        val buildResult = with(AnnotatedString.Builder()) {
            pushStyle(styles[0])
            append("layer1-1")
            pushStyle(styles[1])
            append("layer2-1")
            pop()
            pushStyle(styles[2])
            append("layer2-2")
            pushStyle(styles[3])
            append("layer3-1")
            pop()
            append("layer2-3")
            pop()
            append("layer1-2")
            pop()
            toAnnotatedString()
        }

        assertThat(buildResult.spanStyles).hasSize(4)
        styles.forEachIndexed { index, spanStyle ->
            assertThat(buildResult.spanStyles[index].item).isEqualTo(spanStyle)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun pop_when_empty_does_not_throw_exception() {
        AnnotatedString.Builder().pop()
    }

    @Test
    fun pop_in_the_middle() {
        val spanStyle1 = SpanStyle(color = Color.Red)
        val spanStyle2 = SpanStyle(fontStyle = FontStyle.Italic)

        val buildResult = with(AnnotatedString.Builder()) {
            append("Style0")
            pushStyle(spanStyle1)
            append("Style1")
            pop()
            pushStyle(spanStyle2)
            append("Style2")
            pop()
            append("Style3")
            toAnnotatedString()
        }

        assertThat(buildResult.text).isEqualTo("Style0Style1Style2Style3")
        assertThat(buildResult.spanStyles).hasSize(2)

        // the order is first applied is in the second
        assertThat(buildResult.spanStyles[0].item).isEqualTo((spanStyle1))
        assertThat(buildResult.spanStyles[0].start).isEqualTo(("Style0".length))
        assertThat(buildResult.spanStyles[0].end).isEqualTo(("Style0Style1".length))

        assertThat(buildResult.spanStyles[1].item).isEqualTo((spanStyle2))
        assertThat(buildResult.spanStyles[1].start).isEqualTo(("Style0Style1".length))
        assertThat(buildResult.spanStyles[1].end).isEqualTo(("Style0Style1Style2".length))
    }

    @Test
    fun push_increments_the_style_index() {
        val style = SpanStyle(color = Color.Red)
        with(AnnotatedString.Builder()) {
            val styleIndex0 = pushStyle(style)
            val styleIndex1 = pushStyle(style)
            val styleIndex2 = pushStyle(style)

            assertThat(styleIndex0).isEqualTo(0)
            assertThat(styleIndex1).isEqualTo(1)
            assertThat(styleIndex2).isEqualTo(2)
        }
    }

    @Test
    fun push_reduces_the_style_index_after_pop() {
        val spanStyle = SpanStyle(color = Color.Red)
        val paragraphStyle = ParagraphStyle(lineHeight = 18.sp)

        with(AnnotatedString.Builder()) {
            val styleIndex0 = pushStyle(spanStyle)
            val styleIndex1 = pushStyle(spanStyle)

            assertThat(styleIndex0).isEqualTo(0)
            assertThat(styleIndex1).isEqualTo(1)

            // a pop should reduce the next index to one
            pop()

            val paragraphStyleIndex = pushStyle(paragraphStyle)
            assertThat(paragraphStyleIndex).isEqualTo(1)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun pop_until_throws_exception_for_invalid_index() {
        val style = SpanStyle(color = Color.Red)
        with(AnnotatedString.Builder()) {
            val styleIndex = pushStyle(style)

            // should throw exception
            pop(styleIndex + 1)
        }
    }

    @Test
    fun pop_until_index_pops_correctly() {
        val style = SpanStyle(color = Color.Red)
        with(AnnotatedString.Builder()) {
            pushStyle(style)
            // store the index of second push
            val styleIndex = pushStyle(style)
            pushStyle(style)
            // pop up to and including styleIndex
            pop(styleIndex)
            // push again to get a new index to compare
            val newStyleIndex = pushStyle(style)

            assertThat(newStyleIndex).isEqualTo(styleIndex)
        }
    }

    @Test
    fun withStyle_applies_style_to_block() {
        val style = SpanStyle(color = Color.Red)
        val buildResult = with(AnnotatedString.Builder()) {
            withStyle(style) {
                append("Style")
            }
            toAnnotatedString()
        }

        assertThat(buildResult.paragraphStyles).isEmpty()
        assertThat(buildResult.spanStyles).isEqualTo(
            listOf(Range(style, 0, buildResult.length))
        )
    }

    @Test
    fun withStyle_with_paragraphStyle_applies_style_to_block() {
        val style = ParagraphStyle(lineHeight = 18.sp)
        val buildResult = with(AnnotatedString.Builder()) {
            withStyle(style) {
                append("Style")
            }
            toAnnotatedString()
        }

        assertThat(buildResult.spanStyles).isEmpty()
        assertThat(buildResult.paragraphStyles).isEqualTo(
            listOf(Range(style, 0, buildResult.length))
        )
    }

    @Test
    fun append_char_appends() {
        val buildResult = with(AnnotatedString.Builder("a")) {
            append('b')
            append('c')
            toAnnotatedString()
        }

        assertThat(buildResult).isEqualTo(AnnotatedString("abc"))
    }

    @Test
    fun builderLambda() {
        val text1 = "Hello"
        val text2 = "World"
        val spanStyle1 = SpanStyle(color = Color.Red)
        val spanStyle2 = SpanStyle(color = Color.Blue)
        val paragraphStyle1 = ParagraphStyle(textAlign = TextAlign.Right)
        val paragraphStyle2 = ParagraphStyle(textAlign = TextAlign.Right)

        val buildResult = buildAnnotatedString {
            withStyle(paragraphStyle1) {
                withStyle(spanStyle1) {
                    append(text1)
                }
            }
            append(" ")
            pushStyle(paragraphStyle2)
            pushStyle(spanStyle2)
            append(text2)
            pop()
        }

        val expectedString = "$text1 $text2"
        val expectedSpanStyles = listOf(
            Range(spanStyle1, 0, text1.length),
            Range(spanStyle2, text1.length + 1, expectedString.length)
        )
        val expectedParagraphStyles = listOf(
            Range(paragraphStyle1, 0, text1.length),
            Range(paragraphStyle2, text1.length + 1, expectedString.length)
        )

        assertThat(buildResult.text).isEqualTo(expectedString)
        assertThat(buildResult.spanStyles).isEqualTo(expectedSpanStyles)
        assertThat(buildResult.paragraphStyles).isEqualTo(expectedParagraphStyles)
    }

    @Test
    fun toAnnotatedString_calling_twice_creates_equal_annotated_strings() {
        val builder = AnnotatedString.Builder().apply {
            // pushed styles not popped on purpose
            pushStyle(SpanStyle(color = Color.Red))
            append("Hello")
            pushStyle(SpanStyle(color = Color.Blue))
            append("World")
        }

        assertThat(builder.toAnnotatedString()).isEqualTo(builder.toAnnotatedString())
    }

    @Test
    fun can_call_other_functions_after_toAnnotatedString() {
        val builder = AnnotatedString.Builder().apply {
            // pushed styles not popped on purpose
            pushStyle(SpanStyle(fontSize = 12.sp))
            append("Hello")
            pushStyle(SpanStyle(fontSize = 16.sp))
            append("World")
        }

        val buildResult1 = builder.toAnnotatedString()
        val buildResult2 = with(builder) {
            pop()
            pop()
            pushStyle(SpanStyle(fontSize = 18.sp))
            append("!")
            toAnnotatedString()
        }

        // buildResult2 should be the same as creating a new AnnotatedString based on the first
        // result and appending the same values
        val expectedResult = with(AnnotatedString.Builder(buildResult1)) {
            withStyle(SpanStyle(fontSize = 18.sp)) {
                append("!")
            }
            toAnnotatedString()
        }

        assertThat(buildResult2).isEqualTo(expectedResult)
    }

    @Test
    fun pushAnnotation() {
        val text = "Test"
        val annotation = "Annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushStringAnnotation(tag, annotation)
            append(text)
            pop()
        }.toAnnotatedString()

        assertThat(buildResult.text).isEqualTo(text)
        val stringAnnotations = buildResult.getStringAnnotations(tag, 0, text.length)
        assertThat(stringAnnotations).hasSize(1)
        assertThat(stringAnnotations.first()).isEqualTo(
            Range(annotation, 0, text.length, tag)
        )
    }

    @Test
    fun pushAnnotation_multiple_nested() {
        val annotation1 = "Annotation1"
        val annotation2 = "Annotation2"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushStringAnnotation(tag, annotation1)
            append("Hello")
            pushStringAnnotation(tag, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ]
        //                          [   ]
        assertThat(buildResult.text).isEqualTo("Helloworld!")
        assertThat(buildResult.getStringAnnotations(tag, 0, 11)).hasSize(2)
        assertThat(buildResult.getStringAnnotations(tag, 0, 5)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag, 5, 10)).hasSize(2)
        assertThat(buildResult.getStringAnnotations(tag, 10, 11)).hasSize(1)
        val annotations = buildResult.getStringAnnotations(tag, 0, 11)
        assertThat(annotations[0]).isEqualTo(
            Range(annotation1, 0, 11, tag)
        )
        assertThat(annotations[1]).isEqualTo(
            Range(annotation2, 5, 10, tag)
        )
    }

    @Test
    fun pushAnnotation_multiple_differentTag() {
        val annotation1 = "Annotation1"
        val annotation2 = "Annotation2"
        val tag1 = "tag1"
        val tag2 = "tag2"
        val buildResult = AnnotatedString.Builder().apply {
            pushStringAnnotation(tag1, annotation1)
            append("Hello")
            pushStringAnnotation(tag2, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ]
        //                          [   ]
        assertThat(buildResult.text).isEqualTo("Helloworld!")
        assertThat(buildResult.getStringAnnotations(tag1, 0, 11)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag1, 0, 5)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag1, 5, 10)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag1, 5, 10).first())
            .isEqualTo(Range(annotation1, 0, 11, tag1))

        assertThat(buildResult.getStringAnnotations(tag2, 5, 10)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag2, 5, 10).first())
            .isEqualTo(Range(annotation2, 5, 10, tag2))
        assertThat(buildResult.getStringAnnotations(tag2, 10, 11)).hasSize(0)
    }

    @Test
    fun getAnnotation() {
        val annotation = "Annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            append("Hello")
            pushStringAnnotation(tag, annotation)
            append("World")
            pop()
            append("Hello")
            pushStringAnnotation(tag, annotation)
            pop()
            append("World")
        }.toAnnotatedString()
        // The final result is: HelloWorldHelloWorld
        //                           [   ]
        //                                    []
        assertThat(buildResult.getStringAnnotations(tag, 0, 5)).hasSize(0)
        assertThat(buildResult.getStringAnnotations(tag, 0, 6)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag, 6, 6)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag, 10, 10)).hasSize(0)
        assertThat(buildResult.getStringAnnotations(tag, 8, 13)).hasSize(1)

        assertThat(buildResult.getStringAnnotations(tag, 15, 15)).hasSize(1)
        assertThat(buildResult.getStringAnnotations(tag, 10, 15)).hasSize(0)
        assertThat(buildResult.getStringAnnotations(tag, 15, 20)).hasSize(1)
    }

    @Test
    fun getAnnotation_withoutTag_multipleAnnotations() {
        val annotation1 = "Annotation1"
        val annotation2 = "Annotation2"
        val tag1 = "tag1"
        val tag2 = "tag2"
        val buildResult = AnnotatedString.Builder().apply {
            pushStringAnnotation(tag1, annotation1)
            append("Hello")
            pushStringAnnotation(tag2, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ]
        //                          [   ]
        assertThat(buildResult.getStringAnnotations(0, 5)).isEqualTo(
            listOf(Range(annotation1, 0, 11, tag1))
        )

        assertThat(buildResult.getStringAnnotations(5, 6)).isEqualTo(
            listOf(
                Range(annotation1, 0, 11, tag1),
                Range(annotation2, 5, 10, tag2)
            )
        )

        assertThat(buildResult.getStringAnnotations(10, 11)).isEqualTo(
            listOf(Range(annotation1, 0, 11, tag1))
        )
    }

    @Test
    fun getAnnotation_separates_ttsAnnotation_and_stringAnnotation() {
        val annotation1 = VerbatimTtsAnnotation("abc")
        val annotation2 = "annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushTtsAnnotation(annotation1)
            append("Hello")
            pushStringAnnotation(tag, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ] TtsAnnotation
        //                          [   ]  Range<String>
        assertThat(buildResult.getTtsAnnotations(0, 5)).isEqualTo(
            listOf(Range(annotation1, 0, 11, ""))
        )
        assertThat(buildResult.getTtsAnnotations(5, 6)).isEqualTo(
            listOf(Range(annotation1, 0, 11, ""))
        )

        assertThat(buildResult.getStringAnnotations(0, 5)).isEmpty()
        assertThat(buildResult.getStringAnnotations(5, 6)).isEqualTo(
            listOf(Range(annotation2, 5, 10, tag))
        )
        assertThat(buildResult.getStringAnnotations(10, 11)).isEmpty()
    }

    @Test
    fun getAnnotation_withTag_withTtsAnnotation_withStringAnnotation() {
        val annotation1 = VerbatimTtsAnnotation("abc")
        val annotation2 = "annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushTtsAnnotation(annotation1)
            append("Hello")
            pushStringAnnotation(tag, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ] TtsAnnotation
        //                          [   ]  Range<String>
        assertThat(buildResult.getStringAnnotations(tag, 0, 5)).isEmpty()
        assertThat(buildResult.getStringAnnotations(tag, 5, 6)).isEqualTo(
            listOf(Range(annotation2, 5, 10, tag))
        )
        // The tag doesn't match, return empty list.
        assertThat(buildResult.getStringAnnotations("tag1", 5, 6)).isEmpty()
        assertThat(buildResult.getStringAnnotations(tag, 10, 11)).isEmpty()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun getAnnotation_separates_urlAnnotation_and_stringAnnotation() {
        val annotation1 = UrlAnnotation("abc")
        val annotation2 = "annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushUrlAnnotation(annotation1)
            append("Hello")
            pushStringAnnotation(tag, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ] UrlAnnotation
        //                          [   ]  Range<String>
        assertThat(buildResult.getUrlAnnotations(0, 5)).isEqualTo(
            listOf(Range(annotation1, 0, 11, ""))
        )
        assertThat(buildResult.getUrlAnnotations(5, 6)).isEqualTo(
            listOf(Range(annotation1, 0, 11, ""))
        )

        assertThat(buildResult.getStringAnnotations(0, 5)).isEmpty()
        assertThat(buildResult.getStringAnnotations(5, 6)).isEqualTo(
            listOf(Range(annotation2, 5, 10, tag))
        )
        assertThat(buildResult.getStringAnnotations(10, 11)).isEmpty()
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun getAnnotation_withTag_withUrlAnnotation_withStringAnnotation() {
        val annotation1 = UrlAnnotation("abc")
        val annotation2 = "annotation"
        val tag = "tag"
        val buildResult = AnnotatedString.Builder().apply {
            pushUrlAnnotation(annotation1)
            append("Hello")
            pushStringAnnotation(tag, annotation2)
            append("world")
            pop()
            append("!")
            pop()
        }.toAnnotatedString()

        // The final result is Helloworld!
        //                     [         ] UrlAnnotation
        //                          [   ]  Range<String>
        assertThat(buildResult.getStringAnnotations(tag, 0, 5)).isEmpty()
        assertThat(buildResult.getStringAnnotations(tag, 5, 6)).isEqualTo(
            listOf(Range(annotation2, 5, 10, tag))
        )
        // The tag doesn't match, return empty list.
        assertThat(buildResult.getStringAnnotations("tag1", 5, 6)).isEmpty()
        assertThat(buildResult.getStringAnnotations(tag, 10, 11)).isEmpty()
    }

    private fun createAnnotatedString(
        text: String,
        color: Color = Color.Red,
        lineHeight: TextUnit = 20.sp
    ): AnnotatedString {
        return AnnotatedString(
            text = text,
            spanStyles = listOf(
                Range(
                    item = SpanStyle(color),
                    start = 0,
                    end = text.length
                )
            ),
            paragraphStyles = listOf(
                Range(
                    item = ParagraphStyle(lineHeight = lineHeight),
                    start = 0,
                    end = text.length
                )
            )
        )
    }
}