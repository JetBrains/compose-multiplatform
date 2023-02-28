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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.foundation.demos.text

import android.content.Context.INPUT_METHOD_SERVICE
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.demos.text.WackyTextInputPlugin.createAdapter
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PlatformTextInput
import androidx.compose.ui.text.input.PlatformTextInputAdapter
import androidx.compose.ui.text.input.PlatformTextInputPlugin
import androidx.compose.ui.text.input.TextInputForTests
import androidx.compose.ui.unit.dp
import androidx.core.view.inputmethod.EditorInfoCompat
import kotlinx.coroutines.launch

private const val TAG = "WackyInput"

@Composable
fun PlatformTextInputAdapterDemo() {
    Column {
        Row {
            var value by remember { mutableStateOf("") }
            Text("Standard text field: ")
            TextField(value = value, onValueChange = { value = it })
        }
        Divider()
        Row {
            val value = remember { WackyTextState("") }
            Text("From-scratch text field: ")
            WackyTextField(value, Modifier.weight(1f))
        }
    }
}

@Composable
fun WackyTextField(state: WackyTextState, modifier: Modifier) {
    // rememberAdapter returns an instance of WackyTextInputService as created by
    // WackyTextInputPlugin. If this is the first call, it will instantiate the service and cache
    // it, otherwise it will return the cached instance.
    val service = LocalPlatformTextInputPluginRegistry.current
        .rememberAdapter(WackyTextInputPlugin)
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val borderWidth = remember { Animatable(0f) }

    BasicText(
        text = state.toString(),
        modifier = modifier
            .border(borderWidth.value.dp, Color.Gray.copy(alpha = 0.5f))
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (isFocused == focusState.hasFocus) return@onFocusChanged
                isFocused = focusState.hasFocus
                if (focusState.hasFocus) {
                    scope.launch {
                        borderWidth.animateTo(
                            2f, infiniteRepeatable(tween(500), RepeatMode.Reverse)
                        )
                    }
                    service.startInput(state)
                } else {
                    scope.launch {
                        borderWidth.animateTo(0f)
                    }
                    service.endInput()
                }
            }
            .clickable { focusRequester.requestFocus() }
            .focusable()
    )
}

class WackyTextState(initialValue: String) {
    var refresh by mutableStateOf(Unit, neverEqualPolicy())
    val buffer = StringBuilder(initialValue)

    override fun toString(): String {
        refresh
        return buffer.toString()
    }
}

/**
 * This is an object because it is stateless and the plugin instance is used as the key into the
 * plugin registry to determine whether to reuse an existing adapter or create a new one.
 *
 * Even in a real library this object could be private or internal. It's only used by the text field
 * composable to get instances of [WackyTextInputService]. And if that is also private/internal,
 * then the internal implementation details of this text field are completely inaccessible to
 * external code.
 *
 * If this code were to support multiplatform, this would be an `expect` object with `actual`
 * implementations that had a different [createAdapter] call for each platform. They would probably
 * all return the same type ([WackyTextInputService]), but that type would either be expect/actual
 * itself or contain an instance of an expect/actual class.
 */
private object WackyTextInputPlugin :
    PlatformTextInputPlugin<WackyTextInputService> {

    override fun createAdapter(
        platformTextInput: PlatformTextInput,
        view: View
    ): WackyTextInputService = WackyTextInputService(platformTextInput, view)
}

/**
 * A _very_ incomplete, toy input service that just demonstrates another input service can handoff.
 *
 * Even in a real library, this class could be private or internal. It is only used as an
 * implementation detail inside the text field composable.
 *
 * If this code were to support multiplatform, this would be either an `expect` class with `actual`
 * implementations for each platform, OR contain an instance of an expect/actual class.
 */
private class WackyTextInputService(
    private val platformTextInput: PlatformTextInput,
    private val view: View,
) : PlatformTextInputAdapter {
    private val imm: InputMethodManager =
        view.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    private var currentSession: WackyTextState? = null
    private var currentConnection: WackyInputConnection? = null

    override val inputForTests: TextInputForTests
        get() = currentConnection ?: error("WackyTextInputService is not active")

    fun startInput(state: WackyTextState) {
        Log.d(TAG, "starting input for $state")
        platformTextInput.requestInputFocus()
        currentSession = state
        view.post {
            imm.showSoftInput(view, 0)
        }
    }

    fun endInput() {
        Log.d(TAG, "ending input")
        platformTextInput.releaseInputFocus()
        imm.restartInput(view)
    }

    // TODO input is broken when field is focused first
    override fun createInputConnection(outAttrs: EditorInfo): InputConnection {
        val state = currentSession
        Log.d(TAG, "creating input connection for $state")
        checkNotNull(state)

        outAttrs.initialSelStart = state.buffer.length
        outAttrs.initialSelEnd = state.buffer.length
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
        EditorInfoCompat.setInitialSurroundingText(outAttrs, state.toString())
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN
        state.refresh = Unit
        return WackyInputConnection(state).also {
            currentConnection = it
        }
    }

    /**
     * This class can mostly be ignored for the sake of this demo.
     *
     * This is where most of the actual communication with the Android IME system APIs is. It is
     * an implementation of the Android interface [InputConnection], which is a very large and
     * complex interface to implement. Here we use the [BaseInputConnection] class to avoid
     * implementing the whole thing from scratch, and then only make very weak attempts at handling
     * all the edge cases a real-world text editor would need to handle.
     */
    private inner class WackyInputConnection(
        private val state: WackyTextState
    ) : BaseInputConnection(view, false), TextInputForTests {
        private var selection: TextRange = TextRange(state.buffer.length)
        private var composition: TextRange? = null

        private val batch = mutableVectorOf<() -> Unit>()

        // region InputConnection

        override fun beginBatchEdit(): Boolean = true

        override fun endBatchEdit(): Boolean {
            Log.d(TAG, "ending batch edit")
            batch.forEach { it() }
            batch.clear()
            state.refresh = Unit
            return true
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            Log.d(TAG, "committing text: text=\"$text\", newCursorPosition=$newCursorPosition")
            @Suppress("NAME_SHADOWING")
            val text = text.toString()
            batch += {
                selection = if (composition != null) {
                    state.buffer.replace(composition!!.start, composition!!.end, text)
                    TextRange(composition!!.end)
                } else {
                    state.buffer.replace(selection.start, selection.end, text)
                    TextRange(selection.start + text.length)
                }
            }
            return true
        }

        override fun setComposingRegion(start: Int, end: Int): Boolean {
            Log.d(TAG, "setting composing region: start=$start, end=$end")
            batch += {
                composition =
                    TextRange(
                        start.coerceIn(0, state.buffer.length),
                        end.coerceIn(0, state.buffer.length)
                    )
            }
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            Log.d(
                TAG,
                "setting composing text: text=\"$text\", newCursorPosition=$newCursorPosition"
            )
            @Suppress("NAME_SHADOWING")
            val text = text.toString()
            batch += {
                if (composition != null) {
                    state.buffer.replace(composition!!.start, composition!!.end, text)
                    if (text.isNotEmpty()) {
                        composition =
                            TextRange(composition!!.start, composition!!.start + text.length)
                    }
                    selection = TextRange(composition!!.end)
                } else {
                    state.buffer.replace(selection.start, selection.end, text)
                    if (text.isNotEmpty()) {
                        composition = TextRange(selection.start, selection.start + text.length)
                    }
                    selection = TextRange(selection.start + text.length)
                }
            }
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            Log.d(
                TAG,
                "deleting surrounding text: beforeLength=$beforeLength, afterLength=$afterLength"
            )
            batch += {
                state.buffer.delete(
                    selection.end.coerceIn(0, state.buffer.length),
                    (selection.end + afterLength).coerceIn(0, state.buffer.length)
                )
                state.buffer.delete(
                    (selection.start - beforeLength).coerceIn(0, state.buffer.length),
                    selection.start.coerceIn(0, state.buffer.length)
                )
            }
            return false
        }

        override fun setSelection(start: Int, end: Int): Boolean {
            Log.d(TAG, "setting selection: start=$start, end=$end")
            batch += {
                selection = TextRange(
                    start.coerceIn(0, state.buffer.length),
                    end.coerceIn(0, state.buffer.length)
                )
            }
            return true
        }

        override fun finishComposingText(): Boolean {
            Log.d(TAG, "finishing composing text")
            batch += {
                composition = null
            }
            return true
        }

        override fun closeConnection() {
            Log.d(TAG, "closing input connection")
            currentSession = null
            currentConnection = null
            // This calls finishComposingText, so don't clear the batch until after.
            super.closeConnection()
            batch.clear()
        }

        // endregion
        // region TextInputForTests

        override fun inputTextForTest(text: String) {
            beginBatchEdit()
            commitText(text, 0)
            endBatchEdit()
        }

        override fun submitTextForTest() {
            throw UnsupportedOperationException("just a test")
        }

        // endregion
    }
}