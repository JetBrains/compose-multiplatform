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

package androidx.compose.ui.gesture.customevents

import androidx.compose.ui.gesture.DoubleTapGestureFilter
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.gesture.TapGestureFilter
import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputFilter

/**
 * Dispatched by a [PointerInputFilter] when it wants other [PointerInputFilter]s to delay
 * "responding to up events" (calling callbacks because, for example, a finger lifted off of the
 * screen).
 *
 * Senders and receivers of this message must follow a strict contract such that disambiguation
 * between multiple [PointerInputFilter]s in regards tapping multiple times works correctly and
 * only one responds correctly at a time.  See  [DelayUpMessage] for details.
 *
 * Note: This system and contract currently only works to disambiguate between
 * [DoubleTapGestureFilter] and [TapGestureFilter].  Further multiple tap has not yet been
 * implemented.
 *
 * @param message The [DelayUpMessage] being sent.
 * @param pointers The pointers whose up events are being requested to be delayed.
 */
@Suppress("EqualsOrHashCode")
@ExperimentalPointerInput
data class DelayUpEvent(var message: DelayUpMessage, val pointers: Set<PointerId>) : CustomEvent {

    // Only generating hash code with immutable property.
    override fun hashCode(): Int {
        return pointers.hashCode()
    }
}

/**
 * The types of messages that can be dispatched.
 */
@ExperimentalPointerInput
enum class DelayUpMessage {
    /**
     * Reports that future "up events" should not result in any normally related callbacks at
     * this time.
     *
     * When a [PointerInputFilter] dispatches this message, it must later dispatch
     * [DelayedUpConsumed] or [DelayedUpNotConsumed], even when it is about to be removed from
     * the hierarchy.
     *
     * As an example of how this works in practice.  When a [DoubleTapGestureFilter] sees the
     * last finger leave the screen, it dispatches this message.  When [TapGestureFilter]
     * receives this message, it knows not to fire its [TapGestureFilter.onTap] callback when it
     * later also sees the last finger leave the screen.
     */
    DelayUp,
    /**
     * Reports that previously delayed "up events" have been consumed by another
     * [PointerInputFilter] and thus the receiving [PointerInputFilter] should not fire any
     * associated callbacks.
     *
     * When this message is received, the delaying relationship is concluded and the next message
     * that can be sent is [DelayUp].
     *
     * Continuing the example above, if a new finger "touches" the [DoubleTapGestureFilter]
     * before a timer expires, [DoubleTapGestureFilter] will dispatch this message so that
     * [TapGestureFilter] knows to stop waiting to possibly call [TapGestureFilter.onTap].
     */
    DelayedUpConsumed,
    /**
     * Reports that a receiver can now respond to previously delayed up events.
     *
     * If a receiver does so, they must change the message to DelayedUpConsumed so that when the
     * associated [DelayUpEvent] is dispatched to other future [PointerInputFilter]s, they don't
     * also respond to the delayed event.
     *
     * When this message is received, the delaying relationship is concluded and the next message
     * that can be sent is [DelayUp].
     *
     * Continuing the example above, if a timer expires before a new finger "touches" the
     * [DoubleTapGestureFilter], it will dispatch this message such that [TapGestureFilter] will
     * know it can fire its [TapGestureFilter.onTap] callback.  When it does so, it should change
     * the message to [DelayedUpConsumed] so that any other possible [PointerInputFilter]s will
     * not fire their callbacks associated with the "up" event.
     */
    DelayedUpNotConsumed
}