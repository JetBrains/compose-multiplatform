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

package androidx.compose.foundation.text2.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.util.fastForEach

/**
 * Helper class to apply [EditCommand]s on an internal buffer. Used by TextField Composable
 * to combine TextFieldValue lifecycle with the editing operations.
 *
 * When a [TextFieldValue] is suggested by the developer, [reset] should be called.
 * When [TextInputService] provides [EditCommand]s, they should be applied to the internal
 * buffer using [apply].
 */
internal class EditProcessor(
    initialValue: TextFieldValue
) {

    constructor() : this(
        TextFieldValue(
            EmptyAnnotatedString,
            TextRange.Zero,
            null
        )
    )

    /**
     * The current state of the internal editing buffer as a [TextFieldValue] backed by Snapshot
     * state, so its readers can get updates in composition context.
     */
    var value: TextFieldValue by mutableStateOf(initialValue)
        private set

    // The editing buffer used for applying editor commands from IME.
    internal var mBuffer: EditingBuffer = EditingBuffer(
        text = initialValue.annotatedString,
        selection = initialValue.selection
    )
        private set

    /**
     * Must be called whenever new TextFieldValue arrives.
     *
     * This method updates the internal editing buffer with the given TextFieldValue.
     * This method may tell the IME about the selection offset changes or extracted text changes.
     *
     * Retro; this function seems straightforward but it actually does something very specific
     * for the previous state hoisting design of TextField. In each recomposition, this function
     * was supposed to be called from BasicTextField with the new value. However, this new value
     * wouldn't be new to the internal buffer since the changes coming from IME were already applied
     * in previous composition and sent through onValueChange to the hoisted state.
     *
     * Therefore, this function has to care for two scenarios. 1) Developer doesn't interfere with
     * the value and the editing buffer doesn't have to change because previous composition value
     * is sent back. 2) Developer interferes and the new value is different than the current buffer
     * state. The difference could be text, selection, or composition.
     *
     * In short, `reset` function used to compare newly arrived value in this composition with the
     * internal buffer for any differences. This won't be necessary anymore since the internal state
     * is going to be the only source of truth for the new BasicTextField. However, `reset` would
     * gain a new responsibility in the cases where developer filters the input or adds a template.
     * This would again introduce a need for sync between internal buffer and the state value.
     */
    fun reset(
        newValue: TextFieldValue,
        textInputSession: TextInputSession?,
    ) {
        var textChanged = false
        var selectionChanged = false
        val compositionChanged = newValue.composition != mBuffer.composition

        // TODO(halilibo): String equality check marker.
        if (value.annotatedString != newValue.annotatedString) {
            mBuffer = EditingBuffer(
                text = newValue.annotatedString,
                selection = newValue.selection
            )
            textChanged = true
        } else if (value.selection != newValue.selection) {
            mBuffer.setSelection(newValue.selection.min, newValue.selection.max)
            selectionChanged = true
        }

        val composition = newValue.composition
        if (composition == null) {
            mBuffer.commitComposition()
        } else if (!composition.collapsed) {
            mBuffer.setComposition(composition.min, composition.max)
        }

        // this is the same code as in TextInputServiceAndroid class where restartInput is decided.
        // if restartInput is going to be called the composition has to be cleared otherwise it
        // results in keyboards behaving strangely.
        val finalValue = if (textChanged || (!selectionChanged && compositionChanged)) {
            mBuffer.commitComposition()
            newValue.copy(composition = null)
        } else {
            newValue
        }

        val oldValue = value
        value = finalValue

        textInputSession?.updateState(oldValue, finalValue)
    }

    /**
     * Applies a set of [editCommands] to the internal text editing buffer.
     *
     * After applying the changes, returns the final state of the editing buffer as a
     * [TextFieldValue]
     *
     * @param editCommands [EditCommand]s to be applied to the editing buffer.
     *
     * @return the [TextFieldValue] representation of the final buffer state.
     */
    fun update(editCommands: List<EditCommand>): TextFieldValue {
        var lastCommand: EditCommand? = null
        try {
            editCommands.fastForEach {
                lastCommand = it
                mBuffer.update(it)
            }
        } catch (e: Exception) {
            throw RuntimeException(generateBatchErrorMessage(editCommands, lastCommand), e)
        }

        val newState = TextFieldValue(
            annotatedString = mBuffer.toAnnotatedString(),
            selection = mBuffer.selection,
            composition = mBuffer.composition
        )

        value = newState
        return newState
    }

    private fun generateBatchErrorMessage(
        editCommands: List<EditCommand>,
        failedCommand: EditCommand?,
    ): String = buildString {
        appendLine(
            "Error while applying EditCommand batch to buffer (" +
                "length=${mBuffer.length}, " +
                "composition=${mBuffer.composition}, " +
                "selection=${mBuffer.selection}):"
        )
        @Suppress("ListIterator")
        editCommands.joinTo(this, separator = "\n") {
            val prefix = if (failedCommand === it) " > " else "   "
            prefix + it.toStringForLog()
        }
    }
}

/**
 * Generate a description of the command that is suitable for logging – this should not include
 * any user-entered text, which may be sensitive.
 */
internal fun EditCommand.toStringForLog(): String = when (this) {
    is CommitTextCommand ->
        "CommitTextCommand(text.length=${text.length}, newCursorPosition=$newCursorPosition)"

    is SetComposingTextCommand ->
        "SetComposingTextCommand(text.length=${text.length}, " +
            "newCursorPosition=$newCursorPosition)"

    is SetComposingRegionCommand -> toString()
    is DeleteSurroundingTextCommand -> toString()
    is DeleteSurroundingTextInCodePointsCommand -> toString()
    is SetSelectionCommand -> toString()
    is FinishComposingTextCommand -> toString()
    is BackspaceCommand -> toString()
    is MoveCursorCommand -> toString()
    is DeleteAllCommand -> toString()
}

private val EmptyAnnotatedString = buildAnnotatedString { }