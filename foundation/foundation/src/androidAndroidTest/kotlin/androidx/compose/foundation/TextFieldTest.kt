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

// TODO(b/160821157): Replace FocusState with FocusState2.isFocused
@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.TextInputServiceAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextEditOp
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextFieldValue.Companion.Saver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.length
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.assert
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertShape
import androidx.ui.test.assertTextEquals
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.hasImeAction
import androidx.ui.test.hasInputMethodsSupport
import androidx.ui.test.isFocused
import androidx.ui.test.isNotFocused
import androidx.ui.test.onNode
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import androidx.ui.test.performSemanticsAction
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
@OptIn(
    ExperimentalFocus::class,
    ExperimentalFoundationApi::class
)
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val DefaultTextFieldWidth = 280.dp

    @Test
    fun textField_focusInSemantics() {
        val inputService = mock<TextInputService>()

        var isFocused = false
        composeTestRule.setContent {
            val state = remember { mutableStateOf(TextFieldValue("")) }
            Providers(
                TextInputServiceAmbient provides inputService
            ) {
                BaseTextField(
                    value = state.value,
                    modifier = Modifier.fillMaxSize().focusObserver { isFocused = it.isFocused },
                    onValueChange = { state.value = it }
                )
            }
        }

        onNode(hasInputMethodsSupport()).performClick()

        runOnIdle {
            assertThat(isFocused).isTrue()
        }
    }

    @Composable
    private fun TextFieldApp() {
        val state = remember { mutableStateOf(TextFieldValue("")) }
        BaseTextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = {
                state.value = it
            }
        )
    }

    @Test
    fun textField_commitTexts() {
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                TextFieldApp()
            }
        }

        onNode(hasInputMethodsSupport()).performClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditOperation>) -> Unit>()
            verify(textInputService, times(1)).startInput(
                value = any(),
                keyboardType = any(),
                imeAction = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("a", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("b", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdle {
            val stateCaptor = argumentCaptor<TextFieldValue>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "1a2b3".
            assertThat(stateCaptor.lastValue.text).isEqualTo("1a2b3")
        }
    }

    @Composable
    private fun OnlyDigitsApp() {
        val state = remember { mutableStateOf(TextFieldValue("")) }
        BaseTextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = {
                if (it.text.all { it.isDigit() }) {
                    state.value = it
                }
            }
        )
    }

    @Test
    fun textField_commitTexts_state_may_not_set() {
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                OnlyDigitsApp()
            }
        }

        onNode(hasInputMethodsSupport()).performClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditOperation>) -> Unit>()
            verify(textInputService, times(1)).startInput(
                value = any(),
                keyboardType = any(),
                imeAction = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("a", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("b", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdle {
            val stateCaptor = argumentCaptor<TextFieldValue>()
            verify(textInputService, atLeastOnce())
                .onStateUpdated(eq(inputSessionToken), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123" since
            // the rejects if the incoming model contains alphabets.
            assertThat(stateCaptor.lastValue.text).isEqualTo("123")
        }
    }

    @Test
    fun textField_onTextLayoutCallback() {
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        val onTextLayout: (TextLayoutResult) -> Unit = mock()
        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                val state = remember { mutableStateOf(TextFieldValue("")) }
                BaseTextField(
                    value = state.value,
                    modifier = Modifier.fillMaxSize(),
                    onValueChange = {
                        state.value = it
                    },
                    onTextLayout = onTextLayout
                )
            }
        }

        onNode(hasInputMethodsSupport()).performClick()

        var onEditCommandCallback: ((List<EditOperation>) -> Unit)? = null
        runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditOperation>) -> Unit>()
            verify(textInputService, times(1)).startInput(
                value = any(),
                keyboardType = any(),
                imeAction = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "2", "3".
        arrayOf(
            listOf(CommitTextEditOp("1", 1)),
            listOf(CommitTextEditOp("2", 1)),
            listOf(CommitTextEditOp("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        runOnIdle {
            val layoutCaptor = argumentCaptor<TextLayoutResult>()
            verify(onTextLayout, atLeastOnce()).invoke(layoutCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123"
            assertThat(layoutCaptor.lastValue.layoutInput.text.text).isEqualTo("123")
        }
    }

    @Test
    fun textField_hasDefaultWidth() {
        var size: Int? = null
        composeTestRule.setContent {
            Box {
                BaseTextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier.onPositioned {
                        size = it.size.width
                    }
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(DefaultTextFieldWidth.toIntPx())
        }
    }

    @Test
    fun textField_respectsWidthSetByModifier() {
        val textFieldWidth = 100.dp
        var size: Int? = null
        composeTestRule.setContent {
            Box {
                BaseTextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier
                        .preferredWidth(textFieldWidth)
                        .onPositioned {
                            size = it.size.width
                        }
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(textFieldWidth.toIntPx())
        }
    }

    @Test
    fun textFieldInRow_fixedElementIsVisible() {
        val parentSize = 300.dp
        val boxSize = 50.dp
        var size: Int? = null
        composeTestRule.setContent {
            Box(Modifier.preferredSize(parentSize)) {
                Row {
                    BaseTextField(
                        value = TextFieldValue(),
                        onValueChange = {},
                        modifier = Modifier
                            .weight(1f)
                            .onPositioned {
                                size = it.size.width
                            }
                    )
                    Box(Modifier.preferredSize(boxSize))
                }
            }
        }

        with(composeTestRule.density) {
            assertThat(size).isEqualTo(parentSize.toIntPx() - boxSize.toIntPx())
        }
    }

    @Test
    fun textFieldValue_saverRestoresState() {
        var state: MutableState<TextFieldValue>? = null

        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            state = savedInstanceState(saver = Saver) {
                TextFieldValue()
            }
        }

        runOnIdle {
            state!!.value = TextFieldValue("test", TextRange(1, 2))

            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        runOnIdle {
            assertThat(state!!.value).isEqualTo(
                TextFieldValue("test", TextRange(1, 2))
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldNotFocused_cursorNotRendered() {
        composeTestRule.setContent {
            BaseTextField(
                value = TextFieldValue(),
                onValueChange = {},
                textColor = Color.White,
                modifier = Modifier.preferredSize(10.dp, 20.dp).background(color = Color.White),
                cursorColor = Color.Blue
            )
        }

        onNode(hasInputMethodsSupport())
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    fun defaultSemantics() {
        composeTestRule.setContent {
            BaseTextField(
                modifier = Modifier.testTag("textField"),
                value = TextFieldValue(),
                onValueChange = {}
            )
        }

        onNodeWithTag("textField")
            .assertTextEquals("")
            .assertHasClickAction()
            .assert(hasInputMethodsSupport())
            .assert(hasImeAction(ImeAction.Unspecified))
            .assert(isNotFocused())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange,
                TextRange.Zero))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        onNodeWithTag("textField")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_clickAction() {
        composeTestRule.setContent {
            var value by remember { mutableStateOf(TextFieldValue()) }
            BaseTextField(
                modifier = Modifier.testTag("textField"),
                value = value,
                onValueChange = { value = it }
            )
        }

        onNodeWithTag("textField")
            .assert(isNotFocused())
            .performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag("textField")
            .assert(isFocused())
    }

    @Test
    fun semantics_setTextSetSelectionActions() {
        composeTestRule.setContent {
            var value by remember { mutableStateOf(TextFieldValue()) }
            BaseTextField(
                modifier = Modifier.testTag("textField"),
                value = value,
                onValueChange = { value = it }
            )
        }

        val hello = AnnotatedString("Hello")
        onNodeWithTag("textField")
            .assertTextEquals("")
            .performSemanticsAction(SemanticsActions.SetText) { it(hello) }
        onNodeWithTag("textField")
            .assertTextEquals(hello.text)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange,
                TextRange(hello.length)))

        onNodeWithTag("textField")
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 3, true) }
        onNodeWithTag("textField")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange,
                TextRange(1, 3)))
    }

    @Test
    fun setImeAction_isReflectedInSemantics() {
        composeTestRule.setContent {
            BaseTextField(
                value = TextFieldValue(),
                imeAction = ImeAction.Search,
                onValueChange = {}
            )
        }

        onNode(hasInputMethodsSupport())
            .assert(hasImeAction(ImeAction.Search))
    }
}
