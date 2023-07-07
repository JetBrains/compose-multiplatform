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

package androidx.compose.ui.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * The [InputModeManager] is accessible as a CompositionLocal, that provides the current
 *  [InputMode].
 */
interface InputModeManager {
    /**
     * The current [InputMode].
     */
    val inputMode: InputMode

    /**
     * Send a request to change the [InputMode].
     *
     * @param inputMode The requested [InputMode].
     * @return true if the system is in the requested mode, after processing this request.
     */
    @ExperimentalComposeUiApi
    fun requestInputMode(inputMode: InputMode): Boolean
}

/**
 * This value is used to represent the InputMode that the system is currently in.
 */
@kotlin.jvm.JvmInline
value class InputMode internal constructor(@Suppress("unused") private val value: Int) {
    override fun toString() = when (this) {
        Touch -> "Touch"
        Keyboard -> "Keyboard"
        else -> "Error"
    }

    companion object {
        /**
         * The system is put into [Touch] mode when a user touches the screen.
         */
        val Touch = InputMode(1)

        /**
         * The system is put into [Keyboard] mode when a user presses a hardware key.
         */
        val Keyboard = InputMode(2)
    }
}

internal class InputModeManagerImpl(
    initialInputMode: InputMode,
    private val onRequestInputModeChange: (InputMode) -> Boolean
) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(initialInputMode)

    @ExperimentalComposeUiApi
    override fun requestInputMode(inputMode: InputMode) = onRequestInputModeChange.invoke(inputMode)
}