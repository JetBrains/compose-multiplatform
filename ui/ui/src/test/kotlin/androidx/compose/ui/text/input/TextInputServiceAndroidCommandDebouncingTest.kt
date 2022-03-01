/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.IBinder
import android.view.View
import android.view.inputmethod.ExtractedText
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TextInputServiceAndroidCommandDebouncingTest {

    private val view = mock<View>()
    private val inputMethodManager = TestInputMethodManager()
    private val service = TextInputServiceAndroid(view, inputMethodManager)
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher + Job())

    @Before
    fun setUp() {
        // Default the view to focused because when it's not focused commands should be ignored.
        whenever(view.isFocused).thenReturn(true)
        scope.launch { service.textInputCommandEventLoop() }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun showKeyboard_callsShowKeyboard() {
        service.showSoftwareKeyboard()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).hasSize(1)
        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.hideSoftInputCalls).isEmpty()
    }

    @Test
    fun hideKeyboard_callsHideKeyboard() {
        service.hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(1)
        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
    }

    @Test
    fun startInput_callsRestartInput() {
        service.startInput()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.restartCalls).hasSize(1)
    }

    @Test
    fun startInput_callsShowKeyboard() {
        service.startInput()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).hasSize(1)
    }

    @Test
    fun stopInput_callsRestartInput() {
        service.stopInput()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.restartCalls).hasSize(1)
    }

    @Test
    fun stopInput_callsHideKeyboard() {
        service.stopInput()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(1)
    }

    @Test
    fun startThenStopInput_onlyCallsRestartOnce() {
        service.startInput()
        service.stopInput()
        scope.advanceUntilIdle()

        // Both startInput and stopInput restart the IMM. So calling those two methods back-to-back,
        // in either order, should debounce to a single restart call. If they aren't de-duped, the
        // keyboard may flicker if one of the calls configures the IME in a non-default way (e.g.
        // number input).
        assertThat(inputMethodManager.restartCalls).hasSize(1)
    }

    @Test
    fun stopThenStartInput_onlyCallsRestartOnce() {
        service.stopInput()
        service.startInput()
        scope.advanceUntilIdle()

        // Both startInput and stopInput restart the IMM. So calling those two methods back-to-back,
        // in either order, should debounce to a single restart call. If they aren't de-duped, the
        // keyboard may flicker if one of the calls configures the IME in a non-default way (e.g.
        // number input).
        assertThat(inputMethodManager.restartCalls).hasSize(1)
    }

    @Test
    fun showKeyboard_afterStopInput_isIgnored() {
        service.stopInput()
        service.showSoftwareKeyboard()
        scope.advanceUntilIdle()

        // After stopInput, there's no input connection, so any calls to show the keyboard should
        // be ignored until the next call to startInput.
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
    }

    @Test
    fun hideKeyboard_afterStopInput_isIgnored() {
        service.stopInput()
        service.hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        // stopInput will hide the keyboard implicitly, so both stopInput and hideSoftwareKeyboard
        // have the effect "hide the keyboard". These two effects should be debounced and the IMM
        // should only get a single hide call instead of two redundant calls.
        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(1)
    }

    @Test
    fun multipleShowCallsAreDebounced() {
        repeat(10) {
            service.showSoftwareKeyboard()
        }
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).hasSize(1)
    }

    @Test
    fun multipleHideCallsAreDebounced() {
        repeat(10) {
            service.hideSoftwareKeyboard()
        }
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(1)
    }

    @Test
    fun showThenHideAreDebounced() {
        service.showSoftwareKeyboard()
        service.hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).hasSize(0)
        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(1)
    }

    @Test
    fun hideThenShowAreDebounced() {
        service.hideSoftwareKeyboard()
        service.showSoftwareKeyboard()
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).hasSize(1)
        assertThat(inputMethodManager.hideSoftInputCalls).hasSize(0)
    }

    @Test fun stopInput_isNotProcessedImmediately() {
        service.stopInput()

        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
        assertThat(inputMethodManager.hideSoftInputCalls).isEmpty()
    }

    @Test fun startInput_isNotProcessedImmediately() {
        service.startInput()

        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
        assertThat(inputMethodManager.hideSoftInputCalls).isEmpty()
    }

    @Test fun showSoftwareKeyboard_isNotProcessedImmediately() {
        service.showSoftwareKeyboard()

        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
        assertThat(inputMethodManager.hideSoftInputCalls).isEmpty()
    }

    @Test fun hideSoftwareKeyboard_isNotProcessedImmediately() {
        service.hideSoftwareKeyboard()

        assertThat(inputMethodManager.restartCalls).isEmpty()
        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
        assertThat(inputMethodManager.hideSoftInputCalls).isEmpty()
    }

    @Test fun commandsAreIgnored_ifFocusLostBeforeProcessing() {
        // Send command while view still has focus.
        service.showSoftwareKeyboard()
        // Blur the view.
        whenever(view.isFocused).thenReturn(false)
        // Process the queued commands.
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
    }

    @Test fun commandsAreDrained_whenProcessedWithoutFocus() {
        whenever(view.isFocused).thenReturn(false)
        service.showSoftwareKeyboard()
        service.hideSoftwareKeyboard()
        scope.advanceUntilIdle()
        whenever(view.isFocused).thenReturn(true)
        scope.advanceUntilIdle()

        assertThat(inputMethodManager.showSoftInputCalls).isEmpty()
    }

    private fun TextInputServiceAndroid.startInput() {
        startInput(
            TextFieldValue(),
            ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )
    }

    private class TestInputMethodManager : InputMethodManager {
        val restartCalls = mutableListOf<View>()
        val showSoftInputCalls = mutableListOf<View>()
        val hideSoftInputCalls = mutableListOf<IBinder?>()

        override fun restartInput(view: View) {
            restartCalls += view
        }

        override fun showSoftInput(view: View) {
            showSoftInputCalls += view
        }

        override fun hideSoftInputFromWindow(windowToken: IBinder?) {
            hideSoftInputCalls += windowToken
        }

        override fun updateExtractedText(view: View, token: Int, extractedText: ExtractedText) {
        }

        override fun updateSelection(
            view: View,
            selectionStart: Int,
            selectionEnd: Int,
            compositionStart: Int,
            compositionEnd: Int
        ) {
        }
    }
}