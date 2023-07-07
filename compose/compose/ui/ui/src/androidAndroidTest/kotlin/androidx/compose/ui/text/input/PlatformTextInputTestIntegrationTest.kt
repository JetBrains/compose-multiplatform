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

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test only tests that the [SemanticsNodeInteraction] extension functions related to text
 * input get sent to the [PlatformTextInputAdapter]'s [TextInputForTests].
 *
 * It does *not* test integration with Android's text input system or platform-specific code.
 */
@OptIn(ExperimentalTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputTestIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenNoFieldFocused() {
        val testCommands = mutableListOf<String>()
        rule.setContent {
            TestTextField(testCommands, Modifier.testTag("field"))
        }

        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag("field").performTextInput("hello")
        }
        assertThat(error).hasMessageThat().isEqualTo("No input session started. Missing a focus?")
    }

    @Test
    fun sendsAllTestCommandsToFocusedAdapter() {
        val testCommands = mutableListOf<String>()
        rule.setContent {
            TestTextField(testCommands, Modifier.testTag("field"))
        }

        with(rule.onNodeWithTag("field")) {
            performSemanticsAction(SemanticsActions.RequestFocus)

            performTextInput("hello")
            performImeAction()
        }

        rule.runOnIdle {
            assertThat(testCommands).containsExactly(
                "input(hello)",
                "submit",
            ).inOrder()
        }
    }

    @Test
    fun handlesFocusChange() {
        val testCommands1 = mutableListOf<String>()
        val testCommands2 = mutableListOf<String>()
        rule.setContent {
            TestTextField(testCommands1, Modifier.testTag("field1"))
            TestTextField(testCommands2, Modifier.testTag("field2"))
        }

        with(rule.onNodeWithTag("field1")) {
            performSemanticsAction(SemanticsActions.RequestFocus)
            performTextInput("hello")
        }
        with(rule.onNodeWithTag("field2")) {
            performSemanticsAction(SemanticsActions.RequestFocus)
            performTextInput("world")
        }

        rule.runOnIdle {
            assertThat(testCommands1).containsExactly("input(hello)")
            assertThat(testCommands2).containsExactly("input(world)")
        }
    }

    @Composable
    private fun TestTextField(
        testCommands: MutableList<String>,
        modifier: Modifier = Modifier
    ) {
        val adapter = LocalPlatformTextInputPluginRegistry.current
            .rememberAdapter(TestPlugin)

        Box(
            modifier
                .size(1.dp)
                .onFocusChanged {
                    if (it.isFocused) {
                        adapter.startInput(testCommands)
                    } else {
                        adapter.endInput()
                    }
                }
                .focusable()
                .semantics {
                    setText { true }
                }
        )
    }

    private object TestPlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput)
    }

    private class TestAdapter(
        private val context: PlatformTextInput,
    ) : PlatformTextInputAdapter, TextInputForTests {
        private var testCommands: MutableList<String>? = null

        fun startInput(testCommands: MutableList<String>) {
            this.testCommands = testCommands
            context.requestInputFocus()
        }

        fun endInput() {
            context.releaseInputFocus()
            this.testCommands = null
        }

        override val inputForTests get() = this

        override fun createInputConnection(outAttrs: EditorInfo): InputConnection? = null

        override fun inputTextForTest(text: String) {
            testCommands!! += "input($text)"
        }

        override fun submitTextForTest() {
            testCommands!! += "submit"
        }
    }
}