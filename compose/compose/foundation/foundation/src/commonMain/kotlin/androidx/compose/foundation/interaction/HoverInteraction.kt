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
 * An interaction related to hover events.
 *
 * @see androidx.compose.foundation.hoverable
 * @see Enter
 * @see Exit
 */
interface HoverInteraction : Interaction {
    /**
     * An interaction representing a hover event on a component.
     *
     * @see androidx.compose.foundation.hoverable
     * @see Exit
     */
    class Enter : HoverInteraction

    /**
     * An interaction representing a [Enter] event being released on a component.
     *
     * @property enter the source [Enter] interaction that is being released
     *
     * @see androidx.compose.foundation.hoverable
     * @see Enter
     */
    class Exit(val enter: Enter) : HoverInteraction
}

/**
 * Subscribes to this [MutableInteractionSource] and returns a [State] representing whether this
 * component is hovered or not.
 *
 * [HoverInteraction] is typically set by [androidx.compose.foundation.hoverable] and hoverable
 * components.
 *
 * @return [State] representing whether this component is being hovered or not
 */
@Composable
fun InteractionSource.collectIsHoveredAsState(): State<Boolean> {
    val isHovered = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val hoverInteractions = mutableListOf<HoverInteraction.Enter>()
        interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> hoverInteractions.add(interaction)
                is HoverInteraction.Exit -> hoverInteractions.remove(interaction.enter)
            }
            isHovered.value = hoverInteractions.isNotEmpty()
        }
    }
    return isHovered
}
