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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text.input

import android.graphics.Rect as AndroidRect
import android.text.InputType
import android.util.Log
import android.view.Choreographer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextInputServiceAndroid.TextInputCommand.HideKeyboard
import androidx.compose.ui.text.input.TextInputServiceAndroid.TextInputCommand.ShowKeyboard
import androidx.compose.ui.text.input.TextInputServiceAndroid.TextInputCommand.StartInput
import androidx.compose.ui.text.input.TextInputServiceAndroid.TextInputCommand.StopInput
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.emoji2.text.EmojiCompat
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import kotlin.math.roundToInt

private const val DEBUG_CLASS = "TextInputServiceAndroid"

/**
 * Provide Android specific input service with the Operating System.
 *
 * @param inputCommandProcessorExecutor [Executor] used to schedule the [processInputCommands]
 * function when a input command is first requested for a frame.
 */
internal class TextInputServiceAndroid(
    val view: View,
    private val inputMethodManager: InputMethodManager,
    private val platformTextInput: PlatformTextInput? = null,
    private val inputCommandProcessorExecutor: Executor = Choreographer.getInstance().asExecutor(),
) : PlatformTextInputService {

    /**
     * Commands that can be sent into [textInputCommandQueue] to be processed by
     * [processInputCommands].
     */
    private enum class TextInputCommand {
        StartInput,
        StopInput,
        ShowKeyboard,
        HideKeyboard;
    }

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

    // RecordingInputConnection has strong reference to the View through TextInputServiceAndroid and
    // event callback. The connection should be closed when IME has changed and removed from this
    // list in onConnectionClosed callback, but not clear it is guaranteed the close connection is
    // called any time. So, keep it in WeakReference just in case.
    private var ics = mutableListOf<WeakReference<RecordingInputConnection>>()

    // used for sendKeyEvent delegation
    private val baseInputConnection by lazy(LazyThreadSafetyMode.NONE) {
        BaseInputConnection(view, false)
    }

    private var focusedRect: AndroidRect? = null

    /**
     * A channel that is used to debounce rapid operations such as showing/hiding the keyboard and
     * starting/stopping input, so we can make the minimal number of calls on the
     * [inputMethodManager]. The [TextInputCommand]s sent to this channel are processed by
     * [processInputCommands].
     */
    private val textInputCommandQueue = mutableVectorOf<TextInputCommand>()
    private var frameCallback: Runnable? = null

    constructor(view: View, context: PlatformTextInput? = null) : this(
        view,
        InputMethodManagerImpl(view),
        context
    )

    init {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.create")
        }
    }

    /**
     * Creates new input connection.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.update(imeOptions, state)
        outAttrs.updateWithEmojiCompat()

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

                override fun onConnectionClosed(ic: RecordingInputConnection) {
                    for (i in 0 until ics.size) {
                        if (ics[i].get() == ic) {
                            ics.removeAt(i)
                            return // No duplicated instances should be in the list.
                        }
                    }
                }
            }
        ).also {
            ics.add(WeakReference(it))
            if (DEBUG) {
                Log.d(TAG, "$DEBUG_CLASS.createInputConnection: $ics")
            }
        }
    }

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.startInput")
        }

        platformTextInput?.requestInputFocus()
        state = value
        this.imeOptions = imeOptions
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed

        // Don't actually send the command to the IME yet, it may be overruled by a subsequent call
        // to stopInput.
        sendInputCommand(StartInput)
    }

    override fun stopInput() {
        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.stopInput")

        platformTextInput?.releaseInputFocus()
        onEditCommand = {}
        onImeActionPerformed = {}
        focusedRect = null

        // Don't actually send the command to the IME yet, it may be overruled by a subsequent call
        // to startInput.
        sendInputCommand(StopInput)
    }

    override fun showSoftwareKeyboard() {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.showSoftwareKeyboard")
        }
        sendInputCommand(ShowKeyboard)
    }

    override fun hideSoftwareKeyboard() {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.hideSoftwareKeyboard")
        }
        sendInputCommand(HideKeyboard)
    }

    private fun sendInputCommand(command: TextInputCommand) {
        textInputCommandQueue += command
        if (frameCallback == null) {
            frameCallback = Runnable {
                frameCallback = null
                processInputCommands()
            }.also(inputCommandProcessorExecutor::execute)
        }
    }

    private fun processInputCommands() {
        // When focus changes to a non-Compose view, the system will take care of managing the
        // keyboard (via ImeFocusController) so we don't need to do anything. This can happen
        // when a Compose text field is focused, then the user taps on an EditText view.
        // And any commands that come in while we're not focused should also just be ignored,
        // since no unfocused view should be messing with the keyboard.
        // TODO(b/215761849) When focus moves to a different ComposeView than this one, this
        //  logic doesn't work and the keyboard is not hidden.
        if (!view.isFocused) {
            // All queued commands should be ignored.
            textInputCommandQueue.clear()
            return
        }

        // Multiple commands may have been queued up in the channel while this function was
        // waiting to be resumed. We don't execute the commands as they come in because making a
        // bunch of calls to change the actual IME quickly can result in flickers. Instead, we
        // manually coalesce the commands to figure out the minimum number of IME operations we
        // need to get to the desired final state.
        // The queued commands effectively operate on a simple state machine consisting of two
        // flags:
        //   1. Whether to start a new input connection (true), tear down the input connection
        //      (false), or leave the current connection as-is (null).
        var startInput: Boolean? = null
        //   2. Whether to show the keyboard (true), hide the keyboard (false), or leave the
        //      keyboard visibility as-is (null).
        var showKeyboard: Boolean? = null

        // And a function that performs the appropriate state transition given a command.
        fun TextInputCommand.applyToState() {
            when (this) {
                StartInput -> {
                    // Any commands before restarting the input are meaningless since they would
                    // apply to the connection we're going to tear down and recreate.
                    // Starting a new connection implicitly stops the previous connection.
                    startInput = true
                    // It doesn't make sense to start a new connection without the keyboard
                    // showing.
                    showKeyboard = true
                }

                StopInput -> {
                    startInput = false
                    // It also doesn't make sense to keep the keyboard visible if it's not
                    // connected to anything. Note that this is different than the Android
                    // default behavior for Views, which is to keep the keyboard showing even
                    // after the view that the IME was shown for loses focus.
                    // See this doc for some notes and discussion on whether we should auto-hide
                    // or match Android:
                    // https://docs.google.com/document/d/1o-y3NkfFPCBhfDekdVEEl41tqtjjqs8jOss6txNgqaw/edit?resourcekey=0-o728aLn51uXXnA4Pkpe88Q#heading=h.ieacosb5rizm
                    showKeyboard = false
                }

                ShowKeyboard,
                HideKeyboard -> {
                    // Any keyboard visibility commands sent after input is stopped but before
                    // input is started should be ignored.
                    // Otherwise, the last visibility command sent either before the last stop
                    // command, or after the last start command, is the one that should take
                    // effect.
                    if (startInput != false) {
                        showKeyboard = this == ShowKeyboard
                    }
                }
            }
        }

        // Feed all the queued commands into the state machine.
        textInputCommandQueue.forEach { command ->
            command.applyToState()
            if (DEBUG) {
                Log.d(
                    TAG,
                    "$DEBUG_CLASS.textInputCommandEventLoop.$command " +
                        "(startInput=$startInput, showKeyboard=$showKeyboard)"
                )
            }
        }

        // Now that we've calculated what operations we need to perform on the actual input
        // manager, perform them.
        // If the keyboard visibility was changed after starting a new connection, we need to
        // perform that operation change after starting it.
        // If the keyboard visibility was changed before closing the connection, we need to
        // perform that operation before closing the connection so it doesn't no-op.
        if (startInput == true) {
            restartInputImmediately()
        }
        showKeyboard?.also(::setKeyboardVisibleImmediately)
        if (startInput == false) {
            restartInputImmediately()
        }

        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.textInputCommandEventLoop.finished")
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        if (DEBUG) {
            Log.d(TAG, "$DEBUG_CLASS.updateState called: $oldValue -> $newValue")
        }

        // If the selection has changed from the last time, we need to update selection even though
        // the oldValue in EditBuffer is already in sync with the newValue.
        // Same holds for composition b/207800945
        val needUpdateSelection = (this.state.selection != newValue.selection) ||
            this.state.composition != newValue.composition
        this.state = newValue
        // update the latest TextFieldValue in InputConnection
        for (i in 0 until ics.size) {
            ics[i].get()?.mTextFieldValue = newValue
        }

        if (oldValue == newValue) {
            if (DEBUG) {
                Log.d(TAG, "$DEBUG_CLASS.updateState early return")
            }
            if (needUpdateSelection) {
                // updateSelection API requires -1 if there is no composition
                inputMethodManager.updateSelection(
                    selectionStart = newValue.selection.min,
                    selectionEnd = newValue.selection.max,
                    compositionStart = state.composition?.min ?: -1,
                    compositionEnd = state.composition?.max ?: -1
                )
            }
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
            restartInputImmediately()
        } else {
            for (i in 0 until ics.size) {
                ics[i].get()?.updateInputState(this.state, inputMethodManager)
            }
        }
    }

    @Deprecated("This method should not be called, used BringIntoViewRequester instead.")
    override fun notifyFocusedRect(rect: Rect) {
        focusedRect = AndroidRect(
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
        if (ics.isEmpty()) {
            focusedRect?.let {
                // Notice that view.requestRectangleOnScreen may modify the input Rect, we have to
                // create another Rect and then pass it.
                view.requestRectangleOnScreen(AndroidRect(it))
            }
        }
    }

    /** Immediately restart the IME connection, bypassing the [textInputCommandQueue]. */
    private fun restartInputImmediately() {
        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.restartInputImmediately")
        inputMethodManager.restartInput()
    }

    /** Immediately show or hide the keyboard, bypassing the [textInputCommandQueue]. */
    private fun setKeyboardVisibleImmediately(visible: Boolean) {
        if (DEBUG) Log.d(TAG, "$DEBUG_CLASS.setKeyboardVisibleImmediately(visible=$visible)")
        if (visible) {
            inputMethodManager.showSoftInput()
        } else {
            inputMethodManager.hideSoftInput()
        }
    }
}

/**
 * Call to update EditorInfo correctly when EmojiCompat is configured.
 */
private fun EditorInfo.updateWithEmojiCompat() {
    if (!EmojiCompat.isConfigured()) { return }

    EmojiCompat.get().updateEditorInfo(this)
}

/**
 * Fills necessary info of EditorInfo.
 */
internal fun EditorInfo.update(imeOptions: ImeOptions, textFieldValue: TextFieldValue) {
    this.imeOptions = when (imeOptions.imeAction) {
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
        KeyboardType.Text -> this.inputType = InputType.TYPE_CLASS_TEXT
        KeyboardType.Ascii -> {
            this.inputType = InputType.TYPE_CLASS_TEXT
            this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
        }
        KeyboardType.Number -> this.inputType = InputType.TYPE_CLASS_NUMBER
        KeyboardType.Phone -> this.inputType = InputType.TYPE_CLASS_PHONE
        KeyboardType.Uri ->
            this.inputType = InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        KeyboardType.Email ->
            this.inputType =
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password -> {
            this.inputType =
                InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
        KeyboardType.NumberPassword -> {
            this.inputType =
                InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
        }
        KeyboardType.Decimal -> {
            this.inputType =
                InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
        }
        else -> error("Invalid Keyboard Type")
    }

    if (!imeOptions.singleLine) {
        if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
            // TextView.java#setInputTypeSingleLine
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (imeOptions.imeAction == ImeAction.Default) {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        }
    }

    if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
        when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
            KeyboardCapitalization.Words -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            KeyboardCapitalization.Sentences -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            else -> {
                /* do nothing */
            }
        }

        if (imeOptions.autoCorrect) {
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
    }

    this.initialSelStart = textFieldValue.selection.start
    this.initialSelEnd = textFieldValue.selection.end

    EditorInfoCompat.setInitialSurroundingText(this, textFieldValue.text)

    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
}

internal fun Choreographer.asExecutor(): Executor = Executor { runnable ->
    postFrameCallback { runnable.run() }
}

private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag