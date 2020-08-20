/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * InteractionState represents a [Set] of [Interaction]s present on a given component. This
 * allows you to build higher level components comprised of lower level interactions such as
 * [clickable] and [androidx.compose.foundation.gestures.draggable], and react to [Interaction]
 * changes driven by these components in one place. For [Interaction]s with an associated
 * position, such as [Interaction.Pressed], you can retrieve this position by using
 * [interactionPositionFor].
 *
 * Creating an [InteractionState] and passing it to these lower level interactions will cause a
 * recomposition when there are changes to the state of [Interaction], such as when a [clickable]
 * becomes [Interaction.Pressed].
 *
 * For cases when you are only interested in one [Interaction], or you have a priority for cases
 * when multiple [Interaction]s are present, you can use [contains], such as in the following
 * example:
 *
 * @sample androidx.compose.foundation.samples.PriorityInteractionStateSample
 *
 * Often it is important to respond to the most recently added [Interaction], as this corresponds
 * to the user's most recent interaction with a component. To enable such cases, [value] is
 * guaranteed to have its ordering preserved, with the most recent [Interaction] added to the end.
 * As a result, you can simply iterate / filter [value] from the end, until you find an
 * [Interaction] you are interested in, such as in the following example:
 *
 * @sample androidx.compose.foundation.samples.MultipleInteractionStateSample
 */
@Stable
class InteractionState : State<Set<Interaction>> {

    private var map: Map<Interaction, Offset?> by mutableStateOf(emptyMap())

    /**
     * The [Set] containing all [Interaction]s present in this [InteractionState]. Note that this
     * set is ordered, and the most recently added [Interaction] will be the last element in the
     * set. For representing the most recent [Interaction] in a component, you should iterate over
     * the set in reversed order, until you find an [Interaction] that you are interested in.
     */
    override val value: Set<Interaction>
        get() = map.keys

    /**
     * Adds the provided [interaction] to this InteractionState.
     * Since InteractionState represents a [Set], duplicate [interaction]s will not be added, and
     * hence will not cause a recomposition.
     *
     * @param interaction interaction to add
     * @param position position at which the interaction occurred, if relevant. For example, for
     * [Interaction.Pressed], this will be the position of the pointer input that triggered the
     * pressed state.
     */
    fun addInteraction(interaction: Interaction, position: Offset? = null) {
        if (interaction !in this) map = map + (interaction to position)
    }

    /**
     * Removes the provided [interaction], if it is present, from this InteractionState.
     */
    fun removeInteraction(interaction: Interaction) {
        if (interaction in this) map = map - interaction
    }

    /**
     * Returns the position for a particular [Interaction], if there is a position associated
     * with the interaction.
     *
     * @return position associated with the interaction, or `null` if the interaction is not
     * present in this state, or there is no associated position with the given interaction.
     */
    fun interactionPositionFor(interaction: Interaction): Offset? = map[interaction]

    /**
     * @return whether the provided [interaction] exists inside this InteractionState.
     */
    operator fun contains(interaction: Interaction): Boolean = map.contains(interaction)
}
