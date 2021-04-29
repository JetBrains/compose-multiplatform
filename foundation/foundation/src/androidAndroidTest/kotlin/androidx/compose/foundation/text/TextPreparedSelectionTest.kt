/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.BaseTextPreparedSelection
import androidx.compose.foundation.text.selection.TextPreparedSelection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextPreparedSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textSelection_leftRightMovements() {
        selectionTest("abc") {
            it.moveCursorRight()
            expectedSelection(cursorAt('b'))
            it.moveCursorRight()
            expectedSelection(cursorAt('c'))
            it.moveCursorRight()
            expectedSelection(cursorAfter('c'))
            it.moveCursorRight()
            expectedSelection(cursorAfter('c'))
            it.moveCursorLeft()
            expectedSelection(cursorAt('c'))
        }
    }

    @Test
    fun textSelection_leftRightMovements_rtl() {
        selectionTest("\u0671\u0679\u0683", rtl = true) {
            expectedSelection(cursorAt('\u0671'))
            it.moveCursorLeft()
            expectedSelection(cursorAt('\u0679'))
            it.moveCursorLeft()
            expectedSelection(cursorAt('\u0683'))
            it.moveCursorRight()
            expectedSelection(cursorAt('\u0679'))
        }
    }

    @Test
    fun textSelection_leftRightMovements_bidi() {
        selectionTest("ab \u0671\u0679\u0683 cd") {
            it.moveCursorRight()
            expectedSelection(TextRange(1))
            it.moveCursorRight()
            expectedSelection(TextRange(2))
            it.moveCursorRight()
            expectedSelection(TextRange(3))
            it.moveCursorRight()
            expectedSelection(TextRange(4))
        }
    }

    @Test
    fun textSelection_byWordMovements() {
        selectionTest("abc def\n\ngi") {
            it.moveCursorRightByWord()
            expectedSelection(cursorAfter('c'))
            it.moveCursorRightByWord()
            expectedSelection(cursorAfter('f'))
            it.moveCursorLeftByWord()
            expectedSelection(cursorAt('d'))
            it.moveCursorRightByWord()
            expectedSelection(cursorAfter('f'))
            it.moveCursorRightByWord()
            expectedSelection(cursorAfter('i'))
        }
    }

    @Test
    fun textSelection_byWordMovements_empty() {
        selectionTest("") {
            it.moveCursorRightByWord()
            expectedSelection(TextRange(0))
            it.moveCursorLeftByWord()
            expectedSelection(TextRange(0))
        }
    }

    @Test
    fun textSelection_byParagraphMovements_empty() {
        selectionTest("") {
            it.moveCursorNextByParagraph()
            expectedSelection(TextRange(0))
            it.moveCursorPrevByParagraph()
            expectedSelection(TextRange(0))
        }
    }

    @Test
    fun textSelection_lineMovements() {
        selectionTest("ab\ncde\n\ngi", initSelection = TextRange(1)) {
            it.moveCursorDownByLine()
            expectedSelection(cursorAt('d'))
            it.moveCursorDownByLine()
            // at empty line
            expectedSelection(TextRange(7))
            it.moveCursorDownByLine()
            // cursor should be at "cached" x-position
            expectedSelection(cursorAt('i'))
            it.moveCursorDownByLine()
            expectedSelection(cursorAfter('i'))
            it.moveCursorUpByLine()
            it.moveCursorUpByLine()
            // and again, it should be recovered at "cached" x-position
            expectedSelection(cursorAt('d'))
            it.moveCursorLeft()
            it.moveCursorUpByLine()
            // after horizontal move, "cached" x-position should be reset
            expectedSelection(cursorAt('a'))
        }
    }

    private inner class SelectionScope<T : BaseTextPreparedSelection<T>>(
        val prepared: BaseTextPreparedSelection<T>
    ) {
        fun expectedText(text: String) {
            rule.runOnIdle {
                Truth.assertThat(prepared.text).isEqualTo(text)
            }
        }

        fun expectedSelection(selection: TextRange) {
            rule.runOnIdle {
                Truth.assertThat(prepared.selection).isEqualTo(selection)
            }
        }

        fun cursorAt(char: Char) =
            TextRange(prepared.text.indexOf(char))

        fun cursorAfter(char: Char) =
            TextRange(prepared.text.indexOf(char) + 1)
    }

    private fun selectionTest(
        initText: String = "",
        initSelection: TextRange = TextRange(0),
        rtl: Boolean = false,
        test: SelectionScope<TextPreparedSelection>.(TextPreparedSelection) -> Unit
    ) {
        var textLayout: TextLayoutResult? = null
        val direction = if (rtl) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BasicText(
                    text = initText,
                    style = TextStyle(fontFamily = TEST_FONT_FAMILY),
                    onTextLayout = { textLayout = it }
                )
            }
        }

        val prepared = TextPreparedSelection(
            originalText = AnnotatedString(initText),
            originalSelection = initSelection,
            layoutResult = textLayout!!
        )

        test(SelectionScope(prepared), prepared)
    }
}
