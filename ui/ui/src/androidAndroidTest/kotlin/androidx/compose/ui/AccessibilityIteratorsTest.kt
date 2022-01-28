/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AccessibilityIterators
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.abs

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccessibilityIteratorsTest {
    @get:Rule
    val rule = createComposeRule()

    private val InputText = List(500) { "Line: $it" }.joinToString("\n")
    private val TextFieldTag = "textFieldTag"

    @Test
    fun characterIterator_following() {
        val text = "abc"
        val characterIterator = AccessibilityIterators.CharacterTextSegmentIterator
            .getInstance(Locale.ENGLISH)
        characterIterator.initialize(text)
        // Start from the beginning.
        var currentOffset = 0
        // The character is 'a'.
        var range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('a'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('a') + 1)
        currentOffset++
        // The character is 'b'.
        range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('b'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('b') + 1)
        currentOffset++
        // The character is 'c'.
        range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('c'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        currentOffset++
        range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun characterIterator_preceding() {
        val text = "abc"
        val characterIterator = AccessibilityIterators.CharacterTextSegmentIterator
            .getInstance(Locale.ENGLISH)
        characterIterator.initialize(text)
        // Start from the end.
        var currentOffset = text.length
        // The character is 'c'.
        var range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('c'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        currentOffset--
        // The character is 'b'.
        range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('b'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('b') + 1)
        currentOffset--
        // The character is 'a'.
        range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('a'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('a') + 1)
        currentOffset--
        range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun characterIterator_following_rtl() { // Hebrew -- אבג"
        val text = "\u05d0\u05d1\u05d2"
        val characterIterator = AccessibilityIterators.CharacterTextSegmentIterator
            .getInstance(Locale("he", "IL"))
        characterIterator.initialize(text)
        // Start from the beginning.
        var currentOffset = 0
        // The character is '\u05d0'.
        var range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d0'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('\u05d0') + 1)
        currentOffset = text.length
        range = characterIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun characterIterator_preceding_rtl() { // Hebrew -- אבג"
        val text = "\u05d0\u05d1\u05d2"
        val characterIterator = AccessibilityIterators.CharacterTextSegmentIterator
            .getInstance(Locale("he", "IL"))
        characterIterator.initialize(text)
        // Start from the end.
        var currentOffset = text.length
        // The character is '\u05d2'.
        var range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d2'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('\u05d2') + 1)
        currentOffset = 0
        range = characterIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun wordIterator_following() {
        val text = "abc def-ghi. jkl"
        val wordIterator = AccessibilityIterators.WordTextSegmentIterator
            .getInstance(Locale.ENGLISH)
        wordIterator.initialize(text)
        // Start from the beginning.
        var currentOffset = 0
        // The word is "abc".
        var range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('a'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        // Current position is in the middle of a word.
        currentOffset = text.indexOf('b')
        // The word is "bc".
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('b'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        // Test "-" is a word break.
        currentOffset = text.indexOf('c') + 1
        // The word is "def".
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('d'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('f') + 1)
        currentOffset = text.indexOf('j')
        // The word is "jkl".
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('j'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('l') + 1)
        currentOffset = text.length
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun wordIterator_preceding() {
        val text = "abc def-ghi. jkl"
        val wordIterator = AccessibilityIterators.WordTextSegmentIterator
            .getInstance(Locale.ENGLISH)
        wordIterator.initialize(text)
        // Start from the end.
        var currentOffset = text.length
        // The word is "jkl".
        var range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('j'))
        Truth.assertThat(range[1]).isEqualTo(text.length)
        // Current position is in the middle of a word.
        currentOffset = text.indexOf('h')
        // The word is "g".
        range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('g'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('g') + 1)
        currentOffset = text.indexOf('d')
        // The word is "abc".
        range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('a'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        currentOffset = 0
        range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun wordIterator_following_rtl() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordIterator = AccessibilityIterators.WordTextSegmentIterator
            .getInstance(Locale("he", "IL"))
        wordIterator.initialize(text)
        // Start from the beginning.
        var currentOffset = 0
        // The word is "\u05d0\u05d1\u05d2".
        var range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d0'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('\u05d2') + 1)
        currentOffset = text.indexOf('\u05d3')
        // The word is "\u05d3\u05d4"
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d3'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('-'))
        currentOffset = text.length
        range = wordIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun wordIterator_preceding_rtl() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val wordIterator = AccessibilityIterators.WordTextSegmentIterator
            .getInstance(Locale("he", "IL"))
        wordIterator.initialize(text)
        // Start from the end.
        var currentOffset = text.length
        // The word is "\u05d7\u05d8".
        var range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d7'))
        Truth.assertThat(range[1]).isEqualTo(text.length)
        currentOffset = text.indexOf('.') + 1
        // The word is "\u05d5\u05d6".
        range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('\u05d5'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('\u05d6') + 1)
        currentOffset = 0
        range = wordIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun paragraphIterator_following() {
        val text = "abc\ndefg\nhijk."
        val paragraphIterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance()
        paragraphIterator.initialize(text)
        var currentOffset = 0
        // The paragraph is "abc".
        var range = paragraphIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('a'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('c') + 1)
        currentOffset = text.indexOf('c' + 1)
        // The paragraph is "defg".
        range = paragraphIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('d'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('g') + 1)
        currentOffset = text.indexOf('h')
        // The paragraph is "hijk".
        range = paragraphIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('h'))
        Truth.assertThat(range[1]).isEqualTo(text.length)
        currentOffset = text.length
        range = paragraphIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun paragraphIterator_preceding() {
        val text = "abc\ndefg\nhijk."
        val paragraphIterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance()
        paragraphIterator.initialize(text)
        var currentOffset = text.length
        // The paragraph is "hijk".
        var range = paragraphIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('h'))
        Truth.assertThat(range[1]).isEqualTo(text.length)
        currentOffset = text.indexOf('h')
        // The paragraph is "defg".
        range = paragraphIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(text.indexOf('d'))
        Truth.assertThat(range[1]).isEqualTo(text.indexOf('g') + 1)
        currentOffset = 0
        range = paragraphIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun lineIterator_following() {
        val text = "abcdefgh"
        val textLayoutResult = multiLineText(text, 20.sp, 40.sp)
        val lineIterator = AccessibilityIterators.LineTextSegmentIterator.getInstance()
        lineIterator.initialize(text, textLayoutResult)
        var currentOffset = 0
        // The line is line 0.
        var range = lineIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(range!![0]).isEqualTo(0)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1] - 1)).isEqualTo(0)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1])).isEqualTo(1)
        currentOffset = range[1] + 1
        // The line is line 2 (currentOffset is in the middle of line 1).
        range = lineIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(textLayoutResult.getLineForOffset(range!![0])).isEqualTo(2)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1] - 1)).isEqualTo(2)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1])).isEqualTo(3)
        currentOffset = text.length
        range = lineIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun lineIterator_preceding() {
        val text = "abcdefgh"
        val textLayoutResult = multiLineText(text, 20.sp, 40.sp)
        val lineIterator = AccessibilityIterators.LineTextSegmentIterator.getInstance()
        lineIterator.initialize(text, textLayoutResult)
        var currentOffset = text.length
        // The line is the last line.
        var range = lineIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        val lastLine = textLayoutResult.getLineForOffset(range!![0])
        val previousLine = textLayoutResult.getLineForOffset(range[0] - 1)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1])).isEqualTo(lastLine)
        Truth.assertThat(previousLine).isEqualTo(lastLine - 1)
        currentOffset = range[0] + 1
        // The line is the second last line.
        range = lineIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        Truth.assertThat(textLayoutResult.getLineForOffset(range!![0])).isEqualTo(previousLine)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[0] - 1))
            .isEqualTo(previousLine - 1)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1] - 1)).isEqualTo(previousLine)
        Truth.assertThat(textLayoutResult.getLineForOffset(range[1])).isEqualTo(lastLine)
        currentOffset = 0
        range = lineIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun pageIterator_following() {
        val textLayoutResult = textFieldInScroller()
        val textFieldNode = rule.onNodeWithTag(TextFieldTag).fetchSemanticsNode()
        val pageIterator = AccessibilityIterators.PageTextSegmentIterator.getInstance()
        pageIterator.initialize(InputText, textLayoutResult, textFieldNode)
        var currentOffset = 0
        var range = pageIterator.following(currentOffset)
        Truth.assertThat(range).isNotNull()
        val startLine = textLayoutResult.getLineForOffset(range!![0])
        val endLine = textLayoutResult.getLineForOffset(range[1])
        val startLineTop = textLayoutResult.getLineTop(startLine)
        val endLineTop = textLayoutResult.getLineTop(endLine)
        val lineHeight = textLayoutResult.getLineBottom(endLine) -
            textLayoutResult.getLineTop(endLine)
        val iteratorStep = endLineTop - startLineTop
        val nodeHeight = textFieldNode.boundsInWindow.bottom - textFieldNode.boundsInWindow.top
        Truth.assertThat(abs(iteratorStep - nodeHeight) < lineHeight)
        currentOffset = InputText.length
        range = pageIterator.following(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @Test
    fun pageIterator_preceding() {
        val textLayoutResult = textFieldInScroller()
        val textFieldNode = rule.onNodeWithTag(TextFieldTag).fetchSemanticsNode()
        val pageIterator = AccessibilityIterators.PageTextSegmentIterator.getInstance()
        pageIterator.initialize(InputText, textLayoutResult, textFieldNode)
        var currentOffset = InputText.length
        var range = pageIterator.preceding(currentOffset)
        Truth.assertThat(range).isNotNull()
        val startLine = textLayoutResult.getLineForOffset(range!![0])
        val endLine = textLayoutResult.getLineForOffset(range[1])
        val startLineTop = textLayoutResult.getLineTop(startLine)
        val endLineTop = textLayoutResult.getLineTop(endLine)
        val lineHeight = textLayoutResult.getLineBottom(endLine) -
            textLayoutResult.getLineTop(endLine)
        val iteratorStep = endLineTop - startLineTop
        val nodeHeight = textFieldNode.boundsInWindow.bottom - textFieldNode.boundsInWindow.top
        Truth.assertThat(abs(iteratorStep - nodeHeight) < lineHeight)
        currentOffset = 0
        range = pageIterator.preceding(currentOffset)
        Truth.assertThat(range).isNull()
    }

    @OptIn(ExperimentalTextApi::class)
    private fun multiLineText(
        text: String,
        fontSize: TextUnit = 20.sp,
        width: TextUnit = 40.sp
    ): TextLayoutResult {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            // TODO(yingleiw): use predefined LocalDensity.current when b/163142237 is fixed.
            with(LocalDensity.current) {
                BasicText(
                    style = TextStyle(
                        fontSize = fontSize,
                        fontFamily = Font(
                            resId = androidx.compose.ui.text.font.test.R.font.sample_font,
                            weight = FontWeight.Normal,
                            style = FontStyle.Normal
                        ).toFontFamily()
                    ),
                    text = AnnotatedString(text),
                    modifier = Modifier.requiredWidth(width.toDp()),
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }
        return textLayoutResult!!
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun textFieldInScroller(): TextLayoutResult {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val state = remember { mutableStateOf(InputText) }
                BasicTextField(
                    value = state.value,
                    onValueChange = { state.value = it },
                    modifier = Modifier.testTag(TextFieldTag),
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }
        return textLayoutResult!!
    }
}
