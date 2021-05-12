/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.channels.Channel
import kotlin.math.roundToInt

private const val DEBUG_CLASS = "TextInputServiceAndroid"

/**
 * Provide Android specific input service with the Operating System.
 */
internal class TextInputServiceAndroid(val view: View) : PlatformTextInputService {
    /** True if the currently editable composable has connected */
    private var editorHasFocus = false

    /**
     *  The following three observers are set when the editable composable has initiated the input
     *  session
     */
    private var onEditCommand: (List<EditCommand>) -> Unit = {}
    private var onImeActionPerformed: (ImeAction) -> Unit = {}

    // Visible for testing
    internal var state = TextFieldValue(text = "", selection = TextRange.Zero)
        private set
    private var imeOptions = ImeOptions.Default
    private var ic: RecordingInputConnection? = null
    // used for sendKeyEvent delegation
    private var baseInputConnection: BaseInputConnection = BaseInputConnection(view, false)
    private var focusedRect: android.graphics.Rect? = null

    private var _imm: InputMethodManager? = null
    /**
     * The editable buffer used for BaseInputConnection.
     */
    private val imm: InputMethodManager
        get() {
            if (_imm == null) {
                _imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                    InputMethodManager
            }
            return _imm!!
        }

    /**
     * A channel that is used to send ShowKeyboard/HideKeyboard commands. Send 'true' for
     * show Keyboard and 'false' to hide keyboard.
     */
    private val showKeyboardChannel = Channel<Boolean>(Channel.CONFLATED)

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // focusedRect is null if there is not ongoing text input session. So safe to request
        // latest focused rectangle whenever global layout has changed.
        focusedRect?.let { view.requestRectangleOnScreen(it) }
    }

    init {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                v?.rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
            }

            override fun onViewAttachedToWindow(v: View?) {
                v?.rootView?.viewTreeObserver?.addOnGlobalLayoutListener(layoutListener)
            }
        })
    }

    /**
     * Creates new input connection.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!editorHasFocus) {
            return null
        }
        fillEditorInfo(outAttrs)

        return RecordingInputConnection(
            initState = state,
            autoCorrect = imeOptions.autoCorrect,
            eventCallback = object : InputEventCallback2 {
                override fun onEditCommands(editCommands: List<EditCommand>) {
                    onEditCommand(editCommands)
                }

                override fun onImeAction(imeAction: ImeAction) {
                    onImeActionPerformed(imeAction)
                }

                override fun onKeyEvent(event: KeyEvent) {
                    baseInputConnection.sendKeyEvent(event)
                }
            }
        ).also { ic = it }
    }

    /**
     * Returns true if some editable component is focused.
     */
    fun isEditorFocused(): Boolean = editorHasFocus

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        editorHasFocus = true
        state = value
        this.imeOptions = imeOptions
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed

        view.post {
            restartInput()
            showSoftwareKeyboard()
        }
    }

    override fun stopInput() {
        editorHasFocus = false
        onEditCommand = {}
        onImeActionPerformed = {}
        focusedRect = null

        restartInput()
        editorHasFocus = false
    }

    private fun restartInput() {
        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.restartInput")
        imm.restartInput(view)
    }

    override fun showSoftwareKeyboard() {
        showKeyboardChannel.offer(true)
    }

    override fun hideSoftwareKeyboard() {
        showKeyboardChannel.offer(false)
    }

    suspend fun keyboardVisibilityEventLoop() {
        // TODO(b/180071033): Allow for more IMPLICIT flag to be passed.
        for (showKeyboard in showKeyboardChannel) {
            // Even though we are using a conflated channel, and the producers and consumers are
            // on the same thread, there is a possibility that we have a stale value in the channel
            // because we start consuming from it before we finish producing all the values. We poll
            // to make sure that we use the most recent value.
            if (showKeyboardChannel.poll() ?: showKeyboard) {
                imm.showSoftInput(view, 0)
            } else {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.updateState called: $oldValue -> $newValue")
        }

        this.state = newValue
        // update the latest TextFieldValue in InputConnection
        ic?.mTextFieldValue = newValue

        if (oldValue == newValue) {
            if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.updateState early return") }
            return
        }

        val restartInput = oldValue?.let {
            it.text != newValue.text ||
                // when selection is the same but composition has changed, need to reset the input.
                (it.selection == newValue.selection && it.composition != newValue.composition)
        } ?: false

        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.updateState: restart($restartInput), state: $state")
        }

        if (restartInput) {
            restartInput()
        } else {
            ic?.updateInputState(this.state, imm, view)
        }
    }

    override fun notifyFocusedRect(rect: Rect) {
        focusedRect = android.graphics.Rect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )

        // Requesting rectangle too early after obtaining focus may bring view into wrong place
        // probably due to transient IME inset change. We don't know the correct timing of calling
        // requestRectangleOnScreen API, so try to call this API only after the IME is ready to
        // use, i.e. InputConnection has created.
        // Even if we miss all the timing of requesting rectangle during initial text field focus,
        // focused rectangle will be requested when software keyboard has shown.
        if (ic == null) {
            view.requestRectangleOnScreen(focusedRect)
        }
    }

    /**
     * Fills necessary info of EditorInfo.
     */
    private fun fillEditorInfo(outInfo: EditorInfo) {
        outInfo.imeOptions = when (imeOptions.imeAction) {
            ImeAction.Default -> {
                if (imeOptions.singleLine) {
                    // this is the last resort to enable single line
                    // Android IME still show return key even if multi line is not send
                    // TextView.java#onCreateInputConnection
                    EditorInfo.IME_ACTION_DONE
                } else {
                    EditorInfo.IME_ACTION_UNSPECIFIED
                }
            }
            ImeAction.None -> EditorInfo.IME_ACTION_NONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            else -> error("invalid ImeAction")
        }
        when (imeOptions.keyboardType) {
            KeyboardType.Text -> outInfo.inputType = InputType.TYPE_CLASS_TEXT
            KeyboardType.Ascii -> {
                outInfo.inputType = InputType.TYPE_CLASS_TEXT
                outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
            }
            KeyboardType.Number -> outInfo.inputType = InputType.TYPE_CLASS_NUMBER
            KeyboardType.Phone -> outInfo.inputType = InputType.TYPE_CLASS_PHONE
            KeyboardType.Uri ->
                outInfo.inputType = InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
            KeyboardType.Email ->
                outInfo.inputType =
                    InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            KeyboardType.Password -> {
                outInfo.inputType =
                    InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }
            KeyboardType.NumberPassword -> {
                outInfo.inputType =
                    InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
            }
            else -> error("Invalid Keyboard Type")
        }

        if (!imeOptions.singleLine) {
            if (hasFlag(outInfo.inputType, InputType.TYPE_CLASS_TEXT)) {
                // TextView.java#setInputTypeSingleLine
                outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

                if (imeOptions.imeAction == ImeAction.Default) {
                    outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
                }
            }
        }

        if (hasFlag(outInfo.inputType, InputType.TYPE_CLASS_TEXT)) {
            when (imeOptions.capitalization) {
                KeyboardCapitalization.None -> {
                    /* do nothing */
                }
                KeyboardCapitalization.Characters -> {
                    outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                }
                KeyboardCapitalization.Words -> {
                    outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                }
                KeyboardCapitalization.Sentences -> {
                    outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
            }

            if (imeOptions.autoCorrect) {
                outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            }
        }

        outInfo.initialSelStart = state.selection.start
        outInfo.initialSelEnd = state.selection.end

        @SuppressLint("UnsafeNewApiCall")
        if (Build.VERSION.SDK_INT >= 30) {
            outInfo.setInitialSurroundingText(state.text)
        }

        outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }

    private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag
}