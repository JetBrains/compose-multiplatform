/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val Tag = "field"
private const val ExpectedActionCode = 42

/**
 * This test exercises the use case of an [EditText] embedded in a composition using the text input
 * plugin system to wire into Compose's testing framework.
 */
@OptIn(ExperimentalTextApi::class, ExperimentalTestApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputEditTextIntegrationTest {

    @get:Rule
    val rule = createComposeRule()
    private lateinit var editText: EditText

    @Test
    fun inputText() {
        setContentAndFocusField()

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).performTextInput(" world")

        rule.runOnIdle {
            assertThat(editText.text.toString()).isEqualTo("hello world")
        }
    }

    @Test
    fun clearText() {
        setContentAndFocusField()

        rule.runOnIdle {
            editText.setText("hello")
        }

        rule.onNodeWithTag(Tag).performTextClearance()

        rule.runOnIdle {
            assertThat(editText.text.toString()).isEmpty()
        }
    }

    @Test
    fun replaceText() {
        setContentAndFocusField()

        rule.runOnIdle {
            editText.setText("hello")
        }

        rule.onNodeWithTag(Tag).performTextReplacement("world")

        rule.runOnIdle {
            assertThat(editText.text.toString()).isEqualTo("world")
        }
    }

    @Test
    fun textSelection() {
        setContentAndFocusField()

        rule.runOnIdle {
            editText.setText("hello")
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(1, 3))

        rule.runOnIdle {
            assertThat(editText.text.toString()).isEqualTo("hello")
            assertThat(editText.selectionStart).isEqualTo(1)
            assertThat(editText.selectionEnd).isEqualTo(3)
        }
    }

    @Test
    fun textSubmit() {
        var recordedActionCode: Int = -1
        var recordedKeyEvent: KeyEvent? = null
        setContentAndFocusField()

        rule.runOnIdle {
            editText.setOnEditorActionListener { _, actionCode, keyEvent ->
                recordedActionCode = actionCode
                recordedKeyEvent = keyEvent
                true
            }
        }

        rule.onNodeWithTag(Tag).performImeAction()

        rule.runOnIdle {
            assertThat(recordedActionCode).isEqualTo(ExpectedActionCode)
            assertThat(recordedKeyEvent).isNull()
        }
    }

    private fun setContentAndFocusField() {
        rule.setContent {
            TestTextField(Modifier.testTag(Tag))
        }

        // Focus the field.
        rule.onNodeWithTag(Tag).performClick()
        rule.runOnIdle { assertThat(editText.isFocused).isTrue() }
    }

    @Composable
    private fun TestTextField(modifier: Modifier = Modifier) {
        val adapter = LocalPlatformTextInputPluginRegistry.current
            .rememberAdapter(TestPlugin)

        AndroidView(
            modifier = modifier.semantics {
                // Required for the semantics actions to recognize this node as a text editor.
                setText { text ->
                    adapter.editText?.also {
                        it.setText(text.text)
                        return@setText true
                    }
                    return@setText false
                }
                setSelection { start, end, _ ->
                    adapter.editText?.also {
                        it.setSelection(start, end)
                        return@setSelection true
                    }
                    return@setSelection false
                }
            },
            factory = { context ->
                EditTextWrapper(context, adapter)
                    .also { editText = it }
            }
        )
    }

    private class EditTextWrapper(
        context: Context,
        private val adapter: TestAdapter
    ) : EditText(context), TextInputForTests {

        override fun onFocusChanged(
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)

            // Doesn't interact with the actual compose focus system, it only tells the input
            // plugin registry to delegate test input commands to this adapter.
            if (focused) {
                adapter.editText = this
                adapter.context.requestInputFocus()
            } else {
                adapter.context.releaseInputFocus()
                adapter.editText = null
            }
        }

        override fun inputTextForTest(text: String) {
            this.text.append(text)
        }

        override fun submitTextForTest() {
            onEditorAction(ExpectedActionCode)
        }
    }

    private object TestPlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput)
    }

    private class TestAdapter(
        val context: PlatformTextInput,
    ) : PlatformTextInputAdapter {
        var editText: EditTextWrapper? = null
        override val inputForTests: TextInputForTests? get() = editText
        override fun createInputConnection(outAttrs: EditorInfo): InputConnection? = null
    }
}