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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This tests the behavior of the [PlatformTextInputPluginRegistryImpl] class and its interaction
 * with adapter factories and composition.
 *
 * It does *not* test platform-specific behavior or integration with composition hosts.
 */
@OptIn(InternalTextApi::class, ExperimentalTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputAdapterRegistryTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var registry: PlatformTextInputPluginRegistryImpl

    @Test
    fun rememberAdapter_sharesInstance_inSameComposition() {
        lateinit var adapter1: TestAdapter
        lateinit var adapter2: TestAdapter
        setContent {
            adapter1 = registry.rememberAdapter(TestPlugin)
            adapter2 = registry.rememberAdapter(TestPlugin)
        }

        rule.runOnIdle {
            assertThat(adapter1).isSameInstanceAs(adapter2)
            assertThat(adapter1.isDisposed).isFalse()
        }
    }

    @Test
    fun rememberAdapter_sharesInstance_whenRemovedAndAddedInSameComposition() {
        var branch by mutableStateOf(false)
        var adapter1: TestAdapter? = null
        var adapter2: TestAdapter? = null
        setContent {
            if (branch) {
                adapter1 = registry.rememberAdapter(TestPlugin)
            } else {
                adapter2 = registry.rememberAdapter(TestPlugin)
            }
        }

        rule.runOnIdle {
            assertThat(adapter2).isNotNull()
            branch = true
        }

        rule.runOnIdle {
            assertThat(adapter1).isNotNull()
            assertThat(adapter1).isSameInstanceAs(adapter2)
            assertThat(adapter1!!.isDisposed).isFalse()
        }
    }

    @Test
    fun rememberAdapter_disposesInstance_whenRemoved() {
        var createAdapter by mutableStateOf(true)
        lateinit var adapter: TestAdapter
        setContent {
            if (createAdapter) {
                adapter = registry.rememberAdapter(TestPlugin)
            }
        }

        rule.runOnIdle {
            assertThat(adapter.isDisposed).isFalse()
            createAdapter = false
        }

        rule.runOnIdle {
            assertThat(adapter.isDisposed).isTrue()
        }
    }

    @Test
    fun rememberAdapter_newInstance_whenFullyRemoved() {
        var branch by mutableStateOf(0)
        var adapter1: TestAdapter? = null
        var adapter2: TestAdapter? = null
        setContent {
            when (branch) {
                0 -> adapter1 = registry.rememberAdapter(TestPlugin)

                1 -> {
                    // Let the adapter be disposed.
                }

                2 -> adapter2 = registry.rememberAdapter(TestPlugin)
            }
        }

        rule.runOnIdle {
            assertThat(adapter1).isNotNull()
            branch = 1
        }

        rule.runOnIdle {
            assertThat(adapter1).isNotNull()
            branch = 2
        }

        rule.runOnIdle {
            assertThat(adapter2).isNotNull()
            assertThat(adapter1).isNotSameInstanceAs(adapter2)
            assertThat(adapter1!!.isDisposed).isTrue()
            assertThat(adapter2!!.isDisposed).isFalse()
        }
    }

    @Test
    fun multipleFactories() {
        lateinit var adapter1: TestAdapter
        lateinit var adapter2: TestAdapter
        var createAdapter1 by mutableStateOf(true)
        var createAdapter2 by mutableStateOf(true)
        setContent {
            if (createAdapter1) {
                adapter1 = registry.rememberAdapter(TestPlugin)
            }
            if (createAdapter2) {
                adapter2 = registry.rememberAdapter(AlternatePlugin)
            }
        }

        rule.runOnIdle {
            assertThat(adapter1).isNotSameInstanceAs(adapter2)
            assertThat(adapter1.isDisposed).isFalse()
            assertThat(adapter2.isDisposed).isFalse()
            createAdapter1 = false
        }

        rule.runOnIdle {
            assertThat(adapter1.isDisposed).isTrue()
            assertThat(adapter2.isDisposed).isFalse()
            createAdapter1 = true
            createAdapter2 = false
        }

        rule.runOnIdle {
            assertThat(adapter1.isDisposed).isFalse()
            assertThat(adapter2.isDisposed).isTrue()
        }
    }

    @Test
    fun initialFocus() {
        setContent {
            registry.rememberAdapter(TestPlugin)
            registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            assertThat(registry.focusedAdapter).isNull()
        }
    }

    @Test
    fun requestInitialFocus() {
        lateinit var adapter1: TestAdapter
        lateinit var adapter2: TestAdapter
        setContent {
            adapter1 = registry.rememberAdapter(TestPlugin)
            adapter2 = registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            adapter1.context.requestInputFocus()
            assertThat(registry.focusedAdapter).isSameInstanceAs(adapter1)
            adapter2.context.requestInputFocus()

            assertThat(registry.focusedAdapter).isSameInstanceAs(adapter2)
        }
    }

    @Test
    fun requestFocusTransfer() {
        lateinit var adapter1: TestAdapter
        lateinit var adapter2: TestAdapter
        setContent {
            adapter1 = registry.rememberAdapter(TestPlugin)
            adapter2 = registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            adapter1.context.requestInputFocus()
            adapter2.context.requestInputFocus()

            assertThat(registry.focusedAdapter).isSameInstanceAs(adapter2)
        }
    }

    @Test
    fun releaseFocus_whileHeld() {
        lateinit var adapter1: TestAdapter
        setContent {
            adapter1 = registry.rememberAdapter(TestPlugin)
            registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            adapter1.context.requestInputFocus()
            adapter1.context.releaseInputFocus()

            assertThat(registry.focusedAdapter).isNull()
        }
    }

    @Test
    fun releaseFocus_whileNotHeld() {
        lateinit var adapter1: TestAdapter
        lateinit var adapter2: TestAdapter
        setContent {
            adapter1 = registry.rememberAdapter(TestPlugin)
            adapter2 = registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            adapter1.context.requestInputFocus()
            adapter2.context.requestInputFocus()
            // Should be a noop because adapter1 already lost focus.
            adapter1.context.releaseInputFocus()

            assertThat(registry.focusedAdapter).isSameInstanceAs(adapter2)
        }
    }

    @Test
    fun focusReleased_whenDisposed() {
        lateinit var adapter1: TestAdapter
        var createAdapter1 by mutableStateOf(true)
        setContent {
            if (createAdapter1) {
                adapter1 = registry.rememberAdapter(TestPlugin)
            }
            registry.rememberAdapter(AlternatePlugin)
        }

        rule.runOnIdle {
            adapter1.context.requestInputFocus()
        }
        rule.runOnIdle {
            assertThat(registry.focusedAdapter).isSameInstanceAs(adapter1)
            createAdapter1 = false
        }

        rule.runOnIdle {
            assertThat(registry.focusedAdapter).isNull()
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            val view = LocalView.current
            registry = remember {
                PlatformTextInputPluginRegistryImpl { factory, context ->
                    factory.createAdapter(context, view)
                }
            }
            content()
        }
    }

    private object TestPlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput)
    }

    private object AlternatePlugin : PlatformTextInputPlugin<TestAdapter> {
        override fun createAdapter(
            platformTextInput: PlatformTextInput,
            view: View
        ): TestAdapter = TestAdapter(platformTextInput)
    }

    private class TestAdapter(val context: PlatformTextInput) : PlatformTextInputAdapter {
        var isDisposed: Boolean = false
            private set

        override val inputForTests: TextInputForTests = NoopInputForTests

        override fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
            // Not testing android stuff in this test.
            TODO("Not implemented for test")
        }

        override fun onDisposed() {
            check(!isDisposed) { "TestAdapter already disposed" }
            isDisposed = true
        }
    }

    private object NoopInputForTests : TextInputForTests {
        override fun inputTextForTest(text: String) = TODO("Not implemented for test")
        override fun submitTextForTest() = TODO("Not implemented for test")
    }
}