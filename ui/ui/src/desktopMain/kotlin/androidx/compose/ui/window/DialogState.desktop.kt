/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/**
 * Creates a [DialogState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param position the initial value for [DialogState.position]
 * @param size the initial value for [DialogState.size]
 */
@Composable
fun rememberDialogState(
    position: WindowPosition = WindowPosition(Alignment.Center),
    size: WindowSize = WindowSize(400.dp, 300.dp),
): DialogState = rememberSaveable(saver = DialogStateImpl.Saver(position)) {
    DialogStateImpl(
        position,
        size
    )
}

/**
 * A state object that can be hoisted to control and observe dialog attributes
 * (size/position).
 *
 * In most cases, this will be created via [rememberDialogState].
 *
 * @param position the initial value for [DialogState.position]
 * @param size the initial value for [DialogState.size]
 */
fun DialogState(
    position: WindowPosition = WindowPosition(Alignment.Center),
    size: WindowSize = WindowSize(400.dp, 300.dp)
): DialogState = DialogStateImpl(
    position, size
)

/**
 * A state object that can be hoisted to control and observe dialog attributes
 * (size/position).
 *
 * In most cases, this will be created via [rememberDialogState].
 */
interface DialogState {
    /**
     * Current position of the dialog. If position is not specified ([WindowPosition.isSpecified]
     * is false) then once the dialog shows on the screen the position will be set to
     * absolute values [WindowPosition.Absolute].
     */
    var position: WindowPosition

    /**
     * Current size of the dialog.
     */
    var size: WindowSize
}

private class DialogStateImpl(
    position: WindowPosition,
    size: WindowSize
) : DialogState {
    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)

    companion object {
        /**
         * The default [Saver] implementation for [DialogStateImpl].
         */
        fun Saver(unspecifiedPosition: WindowPosition) = listSaver<DialogState, Any>(
            save = {
                listOf(
                    it.position.isSpecified,
                    it.position.x.value,
                    it.position.y.value,
                    it.size.width.value,
                    it.size.height.value,
                )
            },
            restore = { state ->
                DialogStateImpl(
                    position = if (state[0] as Boolean) {
                        WindowPosition((state[1] as Float).dp, (state[2] as Float).dp)
                    } else {
                        unspecifiedPosition
                    },
                    size = WindowSize((state[3] as Float).dp, (state[4] as Float).dp),
                )
            }
        )
    }
}