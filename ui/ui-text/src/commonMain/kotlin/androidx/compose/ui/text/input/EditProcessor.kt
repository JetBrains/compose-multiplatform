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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.emptyAnnotatedString
import androidx.compose.ui.util.fastForEach

/**
 * Helper class to apply [EditCommand]s on an internal buffer. Used by TextField Composable
 * to combine TextFieldValue lifecycle with the editing operations.
 *
 * * When a [TextFieldValue] is suggested by the developer, [reset] should be called.
 * * When [TextInputService] provides [EditCommand]s, they should be applied to the internal
 * buffer using [apply].
 */
class EditProcessor {

    /**
     * The current state of the internal editing buffer as a [TextFieldValue].
     */
    /*@VisibleForTesting*/
    internal var mBufferState: TextFieldValue = TextFieldValue(
        emptyAnnotatedString(),
        TextRange.Zero,
        null
    )
        private set

    // The editing buffer used for applying editor commands from IME.
    /*@VisibleForTesting*/
    internal var mBuffer: EditingBuffer = EditingBuffer(
        text = mBufferState.annotatedString,
        selection = mBufferState.selection
    )
        private set

    /**
     * Must be called whenever new editor model arrives.
     *
     * This method updates the internal editing buffer with the given editor model.
     * This method may tell the IME about the selection offset changes or extracted text changes.
     */
    fun reset(
        value: TextFieldValue,
        textInputSession: TextInputSession?,
    ) {
        var textChanged = false
        var selectionChanged = false
        val compositionChanged = value.composition != mBuffer.composition

        if (mBufferState.annotatedString != value.annotatedString) {
            mBuffer = EditingBuffer(
                text = value.annotatedString,
                selection = value.selection
            )
            textChanged = true
        } else if (mBufferState.selection != value.selection) {
            mBuffer.setSelection(value.selection.min, value.selection.max)
            selectionChanged = true
        }

        if (value.composition == null) {
            mBuffer.commitComposition()
        } else if (!value.composition.collapsed) {
            mBuffer.setComposition(value.composition.min, value.composition.max)
        }

        // this is the same code as in TextInputServiceAndroid class where restartInput is decided
        // if restartInput is going to be called the composition has to be cleared otherwise it
        // results in keyboards behaving strangely.
        val newValue = if (textChanged || (!selectionChanged && compositionChanged)) {
            mBuffer.commitComposition()
            value.copy(composition = null)
        } else {
            value
        }

        val oldValue = mBufferState
        mBufferState = newValue

        textInputSession?.updateState(oldValue, newValue)
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
    fun apply(editCommands: List<EditCommand>): TextFieldValue {
        var lastCommand: EditCommand? = null
        try {
            editCommands.fastForEach {
                lastCommand = it
                it.applyTo(mBuffer)
            }
        } catch (e: Exception) {
            throw RuntimeException(generateBatchErrorMessage(editCommands, lastCommand), e)
        }

        val newState = TextFieldValue(
            annotatedString = mBuffer.toAnnotatedString(),
            selection = mBuffer.selection,
            composition = mBuffer.composition
        )

        mBufferState = newState
        return newState
    }

    /**
     * Returns the current state of the internal editing buffer as a [TextFieldValue].
     */
    fun toTextFieldValue(): TextFieldValue = mBufferState

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