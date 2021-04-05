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

package androidx.compose.foundation.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.collect

// An interface, not a sealed class, to allow adding new types here in a safe way (and not break
// exhaustive when clauses)
/**
 * An interaction related to press events.
 *
 * @see androidx.compose.foundation.clickable
 * @see Press
 * @see Release
 * @see Cancel
 */
interface PressInteraction : Interaction {
    /**
     * An interaction representing a press event on a component.
     *
     * @property pressPosition the [Offset] describing where this press event occurred within the
     * component
     *
     * @see androidx.compose.foundation.clickable
     * @see Release
     * @see Cancel
     */
    class Press(val pressPosition: Offset) : PressInteraction

    /**
     * An interaction representing the release of a [Press] event on a component.
     *
     * @property press the source [Press] interaction that is being released
     *
     * @see androidx.compose.foundation.clickable
     * @see Press
     */
    class Release(val press: Press) : PressInteraction

    /**
     * An interaction representing the cancellation of a [Press] event on a component.
     *
     * @property press the source [Press] interaction that is being cancelled
     *
     * @see androidx.compose.foundation.clickable
     * @see Press
     */
    class Cancel(val press: Press) : PressInteraction
}

/**
 * Subscribes to this [MutableInteractionSource] and returns a [State] representing whether this
 * component is pressed or not.
 *
 * [PressInteraction] is typically set by [androidx.compose.foundation.clickable] and clickable
 * higher level components, such as buttons.
 *
 * @return [State] representing whether this component is being pressed or not
 */
@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> {
    val isPressed = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val pressInteractions = mutableListOf<PressInteraction.Press>()
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressInteractions.add(interaction)
                is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
            }
            isPressed.value = pressInteractions.isNotEmpty()
        }
    }
    return isPressed
}
