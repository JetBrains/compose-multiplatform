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

package androidx.compose.ui.gesture

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.customevents.DelayUpEvent
import androidx.compose.ui.gesture.customevents.DelayUpMessage
import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * This gesture detector fires a callback when a traditional press is being released.  This is
 * generally the same thing as "onTap" or "onClick".
 *
 * [onTap] is called with the position of the last pointer to go "up".
 *
 * More specifically, it will call [onTap] if:
 * - All of the first [PointerInputChange]s it receives during the [PointerEventPass.Main] pass
 *   have unconsumed down changes, thus representing new set of pointers, none of which have had
 *   their down events consumed.
 * - The last [PointerInputChange] it receives during the [PointerEventPass.Main] pass has
 *   an unconsumed up change.
 * - While it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the heirarchy moved and this a
 *   press should be cancelled.
 * - It also fully cooperates with [DelayUpEvent] [CustomEvent]s it receives such that it will delay
 *   calling [onTap] if all of it's up events are being blocked.  If it was being blocked and later
 *   is allowed to fire it's up event (which is [onTap]) it will do so and consume the delayed up
 *   custom event such that no other gesture filters will also respond to the delayed up.
 *
 *   @param onTap Called when a tap has occurred.
 */
// TODO(b/139020678): Probably has shared functionality with other press based detectors.

fun Modifier.tapGestureFilter(
    onTap: (Offset) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tapGestureFilter"
        this.properties["onTap"] = onTap
    }
) {
    val filter = remember { TapGestureFilter() }
    filter.onTap = onTap
    PointerInputModifierImpl(filter)
}

/**
 * This is a special internal implementation of TapGestureFilter that does not consume changes.  It
 * is used so that root level elements in an instance of Compose can be notified that an unblocked
 * tap has occurred, without blocking other things that are higher up.
 */
internal fun Modifier.noConsumptionTapGestureFilter(
    onTap: (Offset) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "noConsumptionTapGestureFilter"
        this.properties["onTap"] = onTap
    }
) {
    val filter = remember { TapGestureFilter() }
    filter.onTap = onTap
    filter.consumeChanges = false
    PointerInputModifierImpl(filter)
}

internal class TapGestureFilter : PointerInputFilter() {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    lateinit var onTap: (Offset) -> Unit

    /**
     * Whether or not to consume changes.
     */
    var consumeChanges: Boolean = true

    /**
     * True when we are primed to call [onTap] and may be consuming all down changes.
     */
    private var primed = false

    private var downPointers: MutableSet<PointerId> = mutableSetOf()
    private var upBlockedPointers: MutableSet<PointerId> = mutableSetOf()
    private var lastPxPosition: Offset? = null

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {

            if (primed &&
                changes.all { it.changedToUp() }
            ) {
                val pointerPxPosition: Offset = changes[0].previousPosition
                if (changes.fastAny { !upBlockedPointers.contains(it.id) }) {
                    // If we are primed, all pointers went up, and at least one of the pointers is
                    // not blocked, we can fire, reset, and consume all of the up events.
                    reset()
                    onTap.invoke(pointerPxPosition)
                    if (consumeChanges) {
                        changes.fastForEach {
                            it.consumeDownChange()
                        }
                    }
                    return
                } else {
                    lastPxPosition = pointerPxPosition
                }
            }

            if (changes.all { it.changedToDown() }) {
                // Reset in case we were incorrectly left waiting on a delayUp message.
                reset()
                // If all of the changes are down, can become primed.
                primed = true
            }

            if (primed) {
                changes.forEach {
                    if (it.changedToDown()) {
                        downPointers.add(it.id)
                    }
                    if (it.changedToUpIgnoreConsumed()) {
                        downPointers.remove(it.id)
                    }
                }
            }
        }

        if (pass == PointerEventPass.Final && primed) {

            val anyPositionChangeConsumed = changes.fastAny { it.anyPositionChangeConsumed() }

            val noPointersInBounds =
                upBlockedPointers.isEmpty() && !changes.anyPointersInBounds(bounds)

            if (anyPositionChangeConsumed || noPointersInBounds) {
                // If we are on the final pass, we are primed, and either we aren't blocked and
                // all pointers are out of bounds.
                reset()
            }
        }
    }

    override fun onCancel() {
        reset()
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        if (!primed || pass != PointerEventPass.Main || customEvent !is DelayUpEvent) {
            return
        }

        if (customEvent.message == DelayUpMessage.DelayUp) {
            // If the message is to DelayUp, track all currently down pointers that are also ones
            // we are supposed to block the up event for.
            customEvent.pointers.forEach {
                if (downPointers.contains(it)) {
                    upBlockedPointers.add(it)
                }
            }
            return
        }

        upBlockedPointers.removeAll(customEvent.pointers)
        if (upBlockedPointers.isEmpty() && downPointers.isEmpty()) {
            if (customEvent.message == DelayUpMessage.DelayedUpNotConsumed) {
                // If the up was not consumed, then we can fire our callback and consume it.
                onTap.invoke(lastPxPosition!!)
                customEvent.message = DelayUpMessage.DelayedUpConsumed
            }
            // At this point, we were primed, no pointers were down, and we are unblocked, so we
            // are at least resetting.
            reset()
        }
    }

    private fun reset() {
        primed = false
        upBlockedPointers.clear()
        downPointers.clear()
        lastPxPosition = null
    }
}