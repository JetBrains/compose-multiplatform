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

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.emptyAnnotatedString

/**
 * The core editing implementation
 *
 * This class accepts latest text edit state from developer and also receives edit operations from
 * IME.
 *
 * @suppress
 */
@InternalTextApi
class EditProcessor {

    // The last known state of the EditingBuffer
    /*@VisibleForTesting*/
    var mBufferState: TextFieldValue = TextFieldValue(emptyAnnotatedString(), TextRange.Zero, null)
        private set

    // The editing buffer used for applying editor commands from IME.
    /*@VisibleForTesting*/
    internal var mBuffer: EditingBuffer = EditingBuffer(
        text = mBufferState.annotatedString,
        selection = mBufferState.selection
    )

    /**
     * Must be called whenever new editor model arrives.
     *
     * This method updates the internal editing buffer with the given editor model.
     * This method may tell the IME about the selection offset changes or extracted text changes.
     */
    fun onNewState(
        value: TextFieldValue,
        textInputService: TextInputService?,
        token: InputSessionToken
    ) {
        if (mBufferState.annotatedString != value.annotatedString) {
            mBuffer = EditingBuffer(
                text = value.annotatedString,
                selection = value.selection
            )
        } else if (mBufferState.selection != value.selection) {
            mBuffer.setSelection(value.selection.min, value.selection.max)
        }

        if (value.composition == null) {
            mBuffer.commitComposition()
        } else if (!value.composition.collapsed) {
            mBuffer.setComposition(value.composition.min, value.composition.max)
        }

        val oldValue = mBufferState
        mBufferState = value
        textInputService?.updateState(token, oldValue, value)
    }

    /**
     * Must be called whenever new edit operations sent from IMEs arrives.
     *
     * This method updates internal editing buffer with the given edit operations and returns the
     * latest editor state representation of the editing buffer.
     */
    fun onEditCommands(ops: List<EditCommand>): TextFieldValue {
        ops.forEach { it.applyTo(mBuffer) }

        val newState = TextFieldValue(
            annotatedString = mBuffer.toAnnotatedString(),
            selection = TextRange(mBuffer.selectionStart, mBuffer.selectionEnd),
            composition = if (mBuffer.hasComposition()) {
                TextRange(mBuffer.compositionStart, mBuffer.compositionEnd)
            } else {
                null
            }
        )

        mBufferState = newState
        return newState
    }
}