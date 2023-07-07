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
import kotlinx.coroutines.flow.collect

// An interface, not a sealed class, to allow adding new types here in a safe way (and not break
// exhaustive when clauses)
/**
 * An interaction related to focus events.
 *
 * @see androidx.compose.foundation.focusable
 * @see Focus
 * @see Unfocus
 */
interface FocusInteraction : Interaction {
    /**
     * An interaction representing a focus event on a component.
     *
     * @see androidx.compose.foundation.focusable
     * @see Unfocus
     */
    class Focus : FocusInteraction

    /**
     * An interaction representing a [Focus] event being released on a component.
     *
     * @property focus the source [Focus] interaction that is being released
     *
     * @see androidx.compose.foundation.focusable
     * @see Focus
     */
    class Unfocus(val focus: Focus) : FocusInteraction
}

/**
 * Subscribes to this [MutableInteractionSource] and returns a [State] representing whether this
 * component is focused or not.
 *
 * [FocusInteraction] is typically set by [androidx.compose.foundation.focusable] and focusable
 * components, such as [androidx.compose.foundation.text.BasicTextField].
 *
 * @return [State] representing whether this component is being focused or not
 */
@Composable
fun InteractionSource.collectIsFocusedAsState(): State<Boolean> {
    val isFocused = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
        interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> focusInteractions.add(interaction)
                is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
            }
            isFocused.value = focusInteractions.isNotEmpty()
        }
    }
    return isFocused
}
