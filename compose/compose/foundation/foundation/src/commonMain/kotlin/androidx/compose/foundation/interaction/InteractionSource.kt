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

package androidx.compose.foundation.interaction

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.js.JsName

/**
 * InteractionSource represents a stream of [Interaction]s corresponding to events emitted by a
 * component. These [Interaction]s can be used to change how components appear in different
 * states, such as when a component is pressed or dragged.
 *
 * A common use case is [androidx.compose.foundation.indication], where
 * [androidx.compose.foundation.Indication] implementations can subscribe to an [InteractionSource]
 * to draw indication for different [Interaction]s, such as a ripple effect for
 * [PressInteraction.Press] and a state overlay for [DragInteraction.Start].
 *
 * For simple cases where you are interested in the binary state of an [Interaction], such as
 * whether a component is pressed or not, you can use [InteractionSource.collectIsPressedAsState] and other
 * extension functions that subscribe and return a [Boolean] [State] representing whether the
 * component is in this state or not.
 *
 * @sample androidx.compose.foundation.samples.SimpleInteractionSourceSample
 *
 * For more complex cases, such as when building an [androidx.compose.foundation.Indication], the
 * order of the events can change how a component / indication should be drawn. For example, if a
 * component is being dragged and then becomes focused, the most recent [Interaction] is
 * [FocusInteraction.Focus], so the component should appear in a focused state to signal this
 * event to the user.
 *
 * InteractionSource exposes [interactions] to support these use cases - a
 * [Flow] representing the stream of all emitted [Interaction]s. This also provides more
 * information, such as the press position of [PressInteraction.Press], so you can show an effect
 * at the specific point the component was pressed, and whether the press was
 * [PressInteraction.Release] or [PressInteraction.Cancel], for cases when a component
 * should behave differently if the press was released normally or interrupted by another gesture.
 *
 * You can collect from [interactions] as you would with any other [Flow]:
 *
 * @sample androidx.compose.foundation.samples.InteractionSourceFlowSample
 *
 * To emit [Interaction]s so that consumers can react to them, see [MutableInteractionSource].
 *
 * @see MutableInteractionSource
 * @see Interaction
 */
@Stable
interface InteractionSource {
    /**
     * [Flow] representing the stream of all [Interaction]s emitted through this
     * [InteractionSource]. This can be used to see [Interaction]s emitted in order, and with
     * additional metadata, such as the press position for [PressInteraction.Press].
     *
     * @sample androidx.compose.foundation.samples.InteractionSourceFlowSample
     */
    val interactions: Flow<Interaction>
}

/**
 * MutableInteractionSource represents a stream of [Interaction]s corresponding to events emitted
 * by a component. These [Interaction]s can be used to change how components appear
 * in different states, such as when a component is pressed or dragged.
 *
 * Lower level interaction APIs such as [androidx.compose.foundation.clickable] and
 * [androidx.compose.foundation.gestures.draggable] have an [MutableInteractionSource] parameter,
 * which allows you to hoist an [MutableInteractionSource] and combine multiple interactions into
 * one event stream.
 *
 * MutableInteractionSource exposes [emit] and [tryEmit] functions. These emit the provided
 * [Interaction] to the underlying [interactions] [Flow], allowing consumers to react to these
 * new [Interaction]s.
 *
 * An instance of MutableInteractionSource can be created by using the
 * [MutableInteractionSource] factory function. This instance should be [remember]ed before it is
 * passed to other components that consume it.
 *
 * @see InteractionSource
 * @see Interaction
 */
@Stable
interface MutableInteractionSource : InteractionSource {
    /**
     * Emits [interaction] into [interactions].
     * This method is not thread-safe and should not be invoked concurrently.
     *
     * @see tryEmit
     */
    suspend fun emit(interaction: Interaction)

    /**
     * Tries to emit [interaction] into [interactions] without suspending. It returns `true` if the
     * value was emitted successfully.
     *
     * @see emit
     */
    fun tryEmit(interaction: Interaction): Boolean
}

/**
 * Return a new [MutableInteractionSource] that can be hoisted and provided to components,
 * allowing listening to [Interaction] changes inside those components.
 *
 * This should be [remember]ed before it is provided to components, so it can maintain its state
 * across compositions.
 *
 * @see InteractionSource
 * @see MutableInteractionSource
 */
@JsName("funMutableInteractionSource")
fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

@Stable
private class MutableInteractionSourceImpl : MutableInteractionSource {
    // TODO: consider replay for new indication instances during events?
    override val interactions = MutableSharedFlow<Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override suspend fun emit(interaction: Interaction) {
        interactions.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        return interactions.tryEmit(interaction)
    }
}
