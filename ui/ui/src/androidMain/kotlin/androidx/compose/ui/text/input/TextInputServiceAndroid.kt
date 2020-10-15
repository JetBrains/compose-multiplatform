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

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import kotlin.math.roundToInt

/**
 * Provide Android specific input service with the Operating System.
 */
@OptIn(ExperimentalTextApi::class)
internal class TextInputServiceAndroid(val view: View) : PlatformTextInputService {
    /** True if the currently editable composable has connected */
    private var editorHasFocus = false

    /**
     *  The following three observers are set when the editable composable has initiated the input
     *  session
     */
    private var onEditCommand: (List<EditOperation>) -> Unit = {}
    private var onImeActionPerformed: (ImeAction) -> Unit = {}

    private var state = TextFieldValue(text = "", selection = TextRange.Zero)
    private var keyboardType = KeyboardType.Text
    private var imeAction = ImeAction.Unspecified
    private var keyboardOptions = KeyboardOptions.Default
    private var ic: RecordingInputConnection? = null
    private var focusedRect: android.graphics.Rect? = null

    /**
     * The editable buffer used for BaseInputConnection.
     */
    private lateinit var imm: InputMethodManager

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
        fillEditorInfo(keyboardType, imeAction, keyboardOptions, outAttrs)

        return RecordingInputConnection(
            initState = state,
            autoCorrect = keyboardOptions.autoCorrect,
            eventListener = object : InputEventListener {
                override fun onEditOperations(editOps: List<EditOperation>) {
                    onEditCommand(editOps)
                }

                override fun onImeAction(imeAction: ImeAction) {
                    onImeActionPerformed(imeAction)
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
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        keyboardOptions: KeyboardOptions,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editorHasFocus = true
        state = value
        this.keyboardType = keyboardType
        this.imeAction = imeAction
        this.keyboardOptions = keyboardOptions
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed

        view.post {
            imm.restartInput(view)
            imm.showSoftInput(view, 0)
        }
    }

    override fun stopInput() {
        editorHasFocus = false
        onEditCommand = {}
        onImeActionPerformed = {}
        focusedRect = null

        imm.restartInput(view)
        editorHasFocus = false
    }

    override fun showSoftwareKeyboard() {
        imm.showSoftInput(view, 0)
    }

    override fun hideSoftwareKeyboard() {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onStateUpdated(value: TextFieldValue) {
        this.state = value
        ic?.updateInputState(this.state, imm, view)
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
    private fun fillEditorInfo(
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        keyboardOptions: KeyboardOptions,
        outInfo: EditorInfo
    ) {
        outInfo.imeOptions = when (imeAction) {
            ImeAction.Unspecified -> {
                if (keyboardOptions.singleLine) {
                    // this is the last resort to enable single line
                    // Android IME still show return key even if multi line is not send
                    // TextView.java#onCreateInputConnection
                    EditorInfo.IME_ACTION_DONE
                } else {
                    EditorInfo.IME_ACTION_UNSPECIFIED
                }
            }
            ImeAction.NoAction -> EditorInfo.IME_ACTION_NONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            else -> throw IllegalArgumentException("Unknown ImeAction: $imeAction")
        }
        when (keyboardType) {
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
            else -> throw IllegalArgumentException("Unknown KeyboardType: $keyboardType")
        }

        if (!keyboardOptions.singleLine) {
            if (hasFlag(outInfo.inputType, InputType.TYPE_CLASS_TEXT)) {
                // TextView.java#setInputTypeSingleLine
                outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            // TextView.java#onCreateInputConnection
            outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }

        if (hasFlag(outInfo.inputType, InputType.TYPE_CLASS_TEXT)) {
            when (keyboardOptions.capitalization) {
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

            if (keyboardOptions.autoCorrect) {
                outInfo.inputType = outInfo.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            }
        }

        outInfo.imeOptions = outInfo.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }

    private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag
}