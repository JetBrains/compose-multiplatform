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

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo

internal const val DEBUG = false
internal const val TAG = "RecordingIC"
private const val DEBUG_CLASS = "RecordingInputConnection"

/**
 * [InputConnection] implementation that binds Android IME to Compose.
 *
 * @param initState The initial input state.
 * @param eventCallback An input event listener.
 * @param autoCorrect Whether autoCorrect is enabled.
 */
internal class RecordingInputConnection(
    initState: TextFieldValue,
    val eventCallback: InputEventCallback2,
    val autoCorrect: Boolean
) : InputConnection {

    // The depth of the batch session. 0 means no session.
    private var batchDepth: Int = 0

    // The input state.
    internal var mTextFieldValue: TextFieldValue = initState
        set(value) {
            if (DEBUG) { logDebug("mTextFieldValue : $field -> $value") }
            field = value
        }

    /**
     * The token to be used for reporting updateExtractedText API.
     *
     * 0 if no token was specified from IME.
     */
    private var currentExtractedTextRequestToken = 0

    /**
     * True if IME requested extracted text monitor mode.
     *
     * If extracted text monitor mode is ON, need to call updateExtractedText API whenever the text
     * is changed.
     */
    private var extractedTextMonitorMode = false

    // The recoding editing ops.
    private val editCommands = mutableListOf<EditCommand>()

    private var isActive: Boolean = true

    private inline fun ensureActive(block: () -> Unit): Boolean {
        return isActive.also { applying ->
            if (applying) {
                block()
            }
        }
    }

    /**
     * Updates the input state and tells it to the IME.
     *
     * This function may emits updateSelection and updateExtractedText to notify IMEs that the text
     * contents has changed if needed.
     */
    fun updateInputState(
        state: TextFieldValue,
        inputMethodManager: InputMethodManager,
        view: View
    ) {
        if (!isActive) return

        if (DEBUG) { logDebug("RecordingInputConnection.updateInputState: $state") }

        mTextFieldValue = state

        if (extractedTextMonitorMode) {
            inputMethodManager.updateExtractedText(
                view,
                currentExtractedTextRequestToken,
                state.toExtractedText()
            )
        }

        // updateSelection API requires -1 if there is no composition
        val compositionStart = state.composition?.min ?: -1
        val compositionEnd = state.composition?.max ?: -1
        if (DEBUG) {
            logDebug(
                "updateSelection(" +
                    "selection = (${state.selection.min},${state.selection.max}), " +
                    "composition = ($compositionStart, $compositionEnd))"
            )
        }
        inputMethodManager.updateSelection(
            view, state.selection.min, state.selection.max, compositionStart, compositionEnd
        )
    }

    // Add edit op to internal list with wrapping batch edit.
    private fun addEditCommandWithBatch(editCommand: EditCommand) {
        beginBatchEditInternal()
        try {
            editCommands.add(editCommand)
        } finally {
            endBatchEditInternal()
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for text editing session
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun beginBatchEdit(): Boolean = ensureActive {
        if (DEBUG) { logDebug("beginBatchEdit()") }
        return beginBatchEditInternal()
    }

    private fun beginBatchEditInternal(): Boolean {
        batchDepth++
        return true
    }

    override fun endBatchEdit(): Boolean {
        if (DEBUG) { logDebug("endBatchEdit()") }
        return endBatchEditInternal()
    }

    private fun endBatchEditInternal(): Boolean {
        batchDepth--
        if (batchDepth == 0 && editCommands.isNotEmpty()) {
            eventCallback.onEditCommands(editCommands.toMutableList())
            editCommands.clear()
        }
        return batchDepth > 0
    }

    override fun closeConnection() {
        if (DEBUG) { logDebug("closeConnection()") }
        editCommands.clear()
        batchDepth = 0
        isActive = false
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for text editing
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("commitText(\"$text\", $newCursorPosition)") }
        addEditCommandWithBatch(CommitTextCommand(text.toString(), newCursorPosition))
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("setComposingRegion($start, $end)") }
        addEditCommandWithBatch(SetComposingRegionCommand(start, end))
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean =
        ensureActive {
            if (DEBUG) {
                logDebug("setComposingText(\"$text\", $newCursorPosition)")
            }
            addEditCommandWithBatch(SetComposingTextCommand(text.toString(), newCursorPosition))
        }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        ensureActive {
            if (DEBUG) {
                logDebug("deleteSurroundingTextInCodePoints($beforeLength, $afterLength)")
            }
            addEditCommandWithBatch(
                DeleteSurroundingTextInCodePointsCommand(beforeLength, afterLength)
            )
            return true
        }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
        ensureActive {
            if (DEBUG) { logDebug("deleteSurroundingText($beforeLength, $afterLength)") }
            addEditCommandWithBatch(DeleteSurroundingTextCommand(beforeLength, afterLength))
            return true
        }

    override fun setSelection(start: Int, end: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("setSelection($start, $end)") }
        addEditCommandWithBatch(SetSelectionCommand(start, end))
        return true
    }

    override fun finishComposingText(): Boolean = ensureActive {
        if (DEBUG) { logDebug("finishComposingText()") }
        addEditCommandWithBatch(FinishComposingTextCommand())
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean = ensureActive {
        if (DEBUG) { logDebug("sendKeyEvent($event)") }
        eventCallback.onKeyEvent(event)
        return true
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Callbacks for retrieving editing buffers
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun getTextBeforeCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = mTextFieldValue.getTextBeforeSelection(maxChars).toString()
        if (DEBUG) { logDebug("getTextBeforeCursor($maxChars, $flags): $result") }
        return result
    }

    override fun getTextAfterCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = mTextFieldValue.getTextAfterSelection(maxChars).toString()
        if (DEBUG) { logDebug("getTextAfterCursor($maxChars, $flags): $result") }
        return result
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        // https://source.chromium.org/chromium/chromium/src/+/master:content/public/android/java/src/org/chromium/content/browser/input/TextInputState.java;l=56;drc=0e20d1eb38227949805a4c0e9d5cdeddc8d23637
        val result: CharSequence? = if (mTextFieldValue.selection.collapsed) {
            null
        } else {
            // TODO(b/135556699) should return styled text
            mTextFieldValue.getSelectedText().toString()
        }
        if (DEBUG) { logDebug("getSelectedText($flags): $result") }
        return result
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("requestCursorUpdates($cursorUpdateMode)") }
        Log.w(TAG, "requestCursorUpdates is not supported")
        return false
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        if (DEBUG) { logDebug("getExtractedText($request, $flags)") }
        extractedTextMonitorMode = (flags and InputConnection.GET_EXTRACTED_TEXT_MONITOR) != 0
        if (extractedTextMonitorMode) {
            currentExtractedTextRequestToken = request?.token ?: 0
        }
        // TODO(b/135556699) should return styled text
        val extractedText = mTextFieldValue.toExtractedText()

        if (DEBUG) {
            with(extractedText) {
                logDebug(
                    "getExtractedText() return: text: $text" +
                        ",partialStartOffset $partialStartOffset" +
                        ",partialEndOffset $partialEndOffset" +
                        ",selectionStart $selectionStart" +
                        ",selectionEnd $selectionEnd" +
                        ",flags $flags"
                )
            }
        }

        return extractedText
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Editor action and Key events.
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun performContextMenuAction(id: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("performContextMenuAction($id)") }
        when (id) {
            android.R.id.selectAll -> {
                addEditCommandWithBatch(SetSelectionCommand(0, mTextFieldValue.text.length))
            }
            // TODO(siyamed): Need proper connection to cut/copy/paste
            android.R.id.cut -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_CUT)
            android.R.id.copy -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_COPY)
            android.R.id.paste -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_PASTE)
            android.R.id.startSelectingText -> {} // not supported
            android.R.id.stopSelectingText -> {} // not supported
            android.R.id.copyUrl -> {} // not supported
            android.R.id.switchInputMethod -> {} // not supported
            else -> {
                // not supported
            }
        }
        return false
    }

    private fun sendSynthesizedKeyEvent(code: Int) {
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    override fun performEditorAction(editorAction: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("performEditorAction($editorAction)") }
        val imeAction = when (editorAction) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> ImeAction.Default
            EditorInfo.IME_ACTION_DONE -> ImeAction.Done
            EditorInfo.IME_ACTION_SEND -> ImeAction.Send
            EditorInfo.IME_ACTION_SEARCH -> ImeAction.Search
            EditorInfo.IME_ACTION_PREVIOUS -> ImeAction.Previous
            EditorInfo.IME_ACTION_NEXT -> ImeAction.Next
            EditorInfo.IME_ACTION_GO -> ImeAction.Go
            else -> {
                Log.w(TAG, "IME sends unsupported Editor Action: $editorAction")
                ImeAction.Default
            }
        }
        eventCallback.onImeAction(imeAction)
        return true
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Unsupported callbacks
    // /////////////////////////////////////////////////////////////////////////////////////////////

    override fun commitCompletion(text: CompletionInfo?): Boolean = ensureActive {
        if (DEBUG) { logDebug("commitCompletion(${text?.text})") }
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = ensureActive {
        if (DEBUG) { logDebug("commitCorrection($correctionInfo),autoCorrect:$autoCorrect") }
        // Should add an event here so that we can implement the autocorrect highlight
        // Bug: 170647219
        return autoCorrect
    }

    override fun getHandler(): Handler? {
        if (DEBUG) { logDebug("getHandler()") }
        return null // Returns null means using default Handler
    }

    override fun clearMetaKeyStates(states: Int): Boolean = ensureActive {
        if (DEBUG) { logDebug("clearMetaKeyStates($states)") }
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        if (DEBUG) { logDebug("reportFullscreenMode($enabled)") }
        return false // This value is ignored according to the API docs.
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        if (DEBUG) { logDebug("getCursorCapsMode($reqModes)") }
        return TextUtils.getCapsMode(mTextFieldValue.text, mTextFieldValue.selection.min, reqModes)
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = ensureActive {
        if (DEBUG) { logDebug("performPrivateCommand($action, $data)") }
        return true // API doc says we should return true even if we didn't understand the command.
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean = ensureActive {
        if (DEBUG) { logDebug("commitContent($inputContentInfo, $flags, $opts)") }
        return false // We don't accept any contents.
    }

    private fun logDebug(message: String) {
        if (DEBUG) { Log.d(TAG, "$DEBUG_CLASS.$message, $isActive") }
    }
}