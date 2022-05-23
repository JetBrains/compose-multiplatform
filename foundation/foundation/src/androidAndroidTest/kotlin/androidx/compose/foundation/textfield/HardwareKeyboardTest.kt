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

package androidx.compose.foundation.textfield

import android.view.KeyEvent
import android.view.KeyEvent.META_CTRL_ON
import android.view.KeyEvent.META_SHIFT_ON
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class HardwareKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textField_typedEvents() {
        keysSequenceTest {
            Key.H.downAndUp()
            Key.I.downAndUp(META_SHIFT_ON)
            expectedText("hI")
        }
    }

    @Test
    fun textField_copyPaste() {
        keysSequenceTest(initText = "hello") {
            Key.A.downAndUp(META_CTRL_ON)
            Key.C.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp()
            Key.Spacebar.downAndUp()
            Key.V.downAndUp(META_CTRL_ON)
            expectedText("hello hello")
        }
    }

    @Test
    fun textField_linesNavigation() {
        keysSequenceTest(initText = "hello\nworld") {
            Key.DirectionDown.downAndUp()
            Key.Zero.downAndUp()
            Key.DirectionUp.downAndUp()
            Key.Zero.downAndUp()
            expectedText("h0ello\n0world")
            Key.DirectionUp.downAndUp()
            Key.Zero.downAndUp()
            expectedText("0h0ello\n0world")
        }
    }

    @Test
    fun textField_linesNavigation_cache() {
        keysSequenceTest(initText = "hello\n\nworld") {
            Key.DirectionRight.downAndUp()
            Key.DirectionDown.downAndUp()
            Key.DirectionDown.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hello\n\nw0orld")
        }
    }

    @Test
    fun textField_newLine() {
        keysSequenceTest(initText = "hello") {
            Key.Enter.downAndUp()
            expectedText("\nhello")
        }
    }

    @Test
    fun textField_backspace() {
        keysSequenceTest(initText = "hello") {
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp()
            Key.Backspace.downAndUp()
            expectedText("hllo")
        }
    }

    @Test
    fun textField_delete() {
        keysSequenceTest(initText = "hello") {
            Key.Delete.downAndUp()
            expectedText("ello")
        }
    }

    @Test
    fun textField_delete_atEnd() {
        val text = "hello"
        val value = mutableStateOf(
            TextFieldValue(
                text,
                // Place cursor at end.
                selection = TextRange(text.length)
            )
        )
        keysSequenceTest(value = value) {
            Key.Delete.downAndUp()
            expectedText("hello")
        }
    }

    @Test
    fun textField_delete_whenEmpty() {
        keysSequenceTest(initText = "") {
            Key.Delete.downAndUp()
            expectedText("")
        }
    }

    @Test
    fun textField_nextWord() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello0 world")
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello0 world0")
        }
    }

    @Test
    fun textField_nextWord_doubleSpace() {
        keysSequenceTest(initText = "hello  world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp()
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello  world0")
        }
    }

    @Test
    fun textField_prevWord() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionRight.downAndUp(META_CTRL_ON)
            Key.DirectionLeft.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello 0world")
        }
    }

    @Test
    fun textField_HomeAndEnd() {
        keysSequenceTest(initText = "hello world") {
            Key.MoveEnd.downAndUp()
            Key.Zero.downAndUp()
            Key.MoveHome.downAndUp()
            Key.Zero.downAndUp()
            expectedText("0hello world0")
        }
    }

    @Test
    fun textField_byWordSelection() {
        keysSequenceTest(initText = "hello  world\nhi") {
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 5))
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 12))
            Key.DirectionRight.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 15))
            Key.DirectionLeft.downAndUp(META_SHIFT_ON or META_CTRL_ON)
            expectedSelection(TextRange(0, 13))
        }
    }

    @Test
    fun textField_lineEndStart() {
        keysSequenceTest(initText = "hello world\nhi") {
            Key.MoveEnd.downAndUp()
            Key.Zero.downAndUp()
            expectedText("hello world0\nhi")
            Key.MoveEnd.downAndUp()
            Key.MoveHome.downAndUp()
            Key.Zero.downAndUp()
            expectedText("0hello world0\nhi")
            Key.MoveEnd.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(1, 16))
        }
    }

    @Test
    fun textField_deleteWords() {
        keysSequenceTest(initText = "hello world\nhi world") {
            Key.MoveEnd.downAndUp()
            Key.Backspace.downAndUp(META_CTRL_ON)
            expectedText("hello \nhi world")
            Key.Delete.downAndUp(META_CTRL_ON)
            expectedText("hello  world")
        }
    }

    @Test
    fun textField_paragraphNavigation() {
        keysSequenceTest(initText = "hello world\nhi") {
            Key.DirectionDown.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello world0\nhi")
            Key.DirectionDown.downAndUp(META_CTRL_ON)
            Key.DirectionUp.downAndUp(META_CTRL_ON)
            Key.Zero.downAndUp()
            expectedText("hello world0\n0hi")
        }
    }

    @Test
    fun textField_selectionCaret() {
        keysSequenceTest(initText = "hello world") {
            Key.DirectionRight.downAndUp(META_CTRL_ON or META_SHIFT_ON)
            expectedSelection(TextRange(0, 5))
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(0, 6))
            Key.Backslash.downAndUp(META_CTRL_ON)
            expectedSelection(TextRange(6, 6))
            Key.DirectionLeft.downAndUp(META_CTRL_ON or META_SHIFT_ON)
            expectedSelection(TextRange(6, 0))
            Key.DirectionRight.downAndUp(META_SHIFT_ON)
            expectedSelection(TextRange(6, 1))
        }
    }

    @Test
    fun textField_onValueChangeRecomposeTest() {
        // sample code in b/200577798
        val value = mutableStateOf(TextFieldValue(""))
        var lastNewValue: TextFieldValue? = null
        val onValueChange: (TextFieldValue) -> Unit = { newValue ->
            lastNewValue = newValue
            if (newValue.text.isBlank() || newValue.text.startsWith("z")) {
                value.value = newValue
            }
        }

        keysSequenceTest(value = value, onValueChange = onValueChange) {
            // based on repro steps in the ticket, one of the values would become "aa"
            // check 10 times to make sure it is not "aa"
            repeat(10) {
                Key.A.downAndUp()
                // should always be "a" and buffer should not accumulate
                assertThat(lastNewValue?.text).isEqualTo("a")
            }
        }
    }

    @Test
    fun textField_pageNavigation() {
        keysSequenceTest(
            initText = "1\n2\n3\n4\n5",
            modifier = Modifier.requiredSize(27.dp)
        ) {
            // By page down, the cursor should be at the visible top line. In this case the height
            // constraint is 27dp which covers from 1, 2 and middle of 3. Thus, by page down, the
            // first line should be 3, and cursor should be the before letter 3, i.e. index = 4.
            Key.PageDown.downAndUp()
            expectedSelection(TextRange(4))
        }
    }

    private inner class SequenceScope(
        val state: MutableState<TextFieldValue>,
        val nodeGetter: () -> SemanticsNodeInteraction
    ) {
        fun Key.downAndUp(metaState: Int = 0) {
            this.down(metaState)
            this.up(metaState)
        }

        fun Key.down(metaState: Int = 0) {
            nodeGetter().performKeyPress(downEvent(this, metaState))
        }

        fun Key.up(metaState: Int = 0) {
            nodeGetter().performKeyPress(upEvent(this, metaState))
        }

        fun expectedText(text: String) {
            rule.runOnIdle {
                assertThat(state.value.text).isEqualTo(text)
            }
        }

        fun expectedSelection(selection: TextRange) {
            rule.runOnIdle {
                assertThat(state.value.selection).isEqualTo(selection)
            }
        }
    }

    private fun keysSequenceTest(
        initText: String = "",
        modifier: Modifier = Modifier.fillMaxSize(),
        sequence: SequenceScope.() -> Unit,
    ) {
        val value = mutableStateOf(TextFieldValue(initText))
        keysSequenceTest(value = value, modifier = modifier, sequence = sequence)
    }

    private fun keysSequenceTest(
        value: MutableState<TextFieldValue>,
        modifier: Modifier = Modifier.fillMaxSize(),
        onValueChange: (TextFieldValue) -> Unit = { value.value = it },
        sequence: SequenceScope.() -> Unit,
    ) {
        val inputService = TextInputService(mock())
        val focusFequester = FocusRequester()
        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides inputService
            ) {
                BasicTextField(
                    value = value.value,
                    textStyle = TextStyle(
                        fontFamily = TEST_FONT_FAMILY,
                        fontSize = 10.sp
                    ),
                    modifier = modifier.focusRequester(focusFequester),
                    onValueChange = onValueChange
                )
            }
        }

        rule.runOnIdle { focusFequester.requestFocus() }

        sequence(SequenceScope(value) { rule.onNode(hasSetTextAction()) })
    }
}

private fun downEvent(key: Key, metaState: Int = 0): androidx.compose.ui.input.key.KeyEvent {
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, key.nativeKeyCode, 0, metaState)
    )
}

private fun upEvent(key: Key, metaState: Int = 0): androidx.compose.ui.input.key.KeyEvent {
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(0L, 0L, KeyEvent.ACTION_UP, key.nativeKeyCode, 0, metaState)
    )
}