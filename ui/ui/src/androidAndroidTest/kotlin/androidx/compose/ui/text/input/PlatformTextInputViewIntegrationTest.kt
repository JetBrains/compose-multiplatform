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
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputViewIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var hostView: AndroidComposeView
    private lateinit var adapter1: TestAdapter
    private lateinit var adapter2: TestAdapter

    @Test
    fun hostViewIsPassedToFactory() {
        setupContent()
        rule.runOnIdle {
            assertThat(adapter1.view).isSameInstanceAs(hostView)
            assertThat(adapter2.view).isSameInstanceAs(hostView)
        }
    }

    @Test
    fun checkIsTextEditor_whenNoAdapterFocused() {
        setupContent()
        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isFalse()
        }
    }

    @Test
    fun checkIsTextEditor_whenAdapterFocused() {
        setupContent()
        rule.runOnIdle {
            adapter2.context.requestInputFocus()
            assertThat(hostView.onCheckIsTextEditor()).isTrue()

            adapter2.context.releaseInputFocus()
            assertThat(hostView.onCheckIsTextEditor()).isFalse()
        }
    }

    @Test
    fun createInputConnection_whenNoAdapterFocused() {
        setupContent()
        rule.runOnIdle {
            val editorInfo = EditorInfo()
            assertThat(hostView.onCreateInputConnection(editorInfo)).isNull()
        }
    }

    @Test
    fun createInputConnection_goesToFocusedAdapter() {
        setupContent()
        rule.runOnIdle {
            adapter2.context.requestInputFocus()

            val editorInfo = EditorInfo()
            val connection1 = hostView.onCreateInputConnection(editorInfo)
            assertThat(connection1).isSameInstanceAs(adapter2.inputConnection)
            assertThat(editorInfo.actionLabel).isEqualTo(adapter2.actionLabel)

            adapter1.context.requestInputFocus()

            val connection2 = hostView.onCreateInputConnection(editorInfo)
            assertThat(connection2).isNotSameInstanceAs(connection1)
            assertThat(connection2).isSameInstanceAs(adapter1.inputConnection)
        }
    }

    private fun setupContent() {
        rule.setContent {
            hostView = LocalView.current as AndroidComposeView
            val adapterProvider = LocalPlatformTextInputPluginRegistry.current
            adapter1 = adapterProvider.rememberAdapter(TestPlugin)
            adapter2 = adapterProvider.rememberAdapter(AlternatePlugin)
        }
    }

    private object TestPlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput, view)
    }

    private object AlternatePlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput, view)
    }

    private class TestAdapter(
        val context: PlatformTextInput,
        val view: View
    ) : PlatformTextInputAdapter {
        var isDisposed: Boolean = false
            private set

        val actionLabel = "test connection!"
        val inputConnection = TestInputConnection(view)

        override val inputForTests: TextInputForTests = NoopInputForTests

        override fun createInputConnection(outAttrs: EditorInfo): InputConnection {
            outAttrs.actionLabel = actionLabel
            return inputConnection
        }

        override fun onDisposed() {
            check(!isDisposed) { "TestAdapter already disposed" }
            isDisposed = true
        }
    }

    private class TestInputConnection(view: View) : BaseInputConnection(view, false)

    private object NoopInputForTests : TextInputForTests {
        override fun inputTextForTest(text: String) = TODO("Not implemented for test")
        override fun submitTextForTest() = TODO("Not implemented for test")
    }
}