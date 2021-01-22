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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * This gesture detector has callbacks for when a press gesture starts and ends for the purposes of
 * displaying visual feedback for those two states.
 *
 * More specifically:
 * - It will call [onStart] if the first pointer down it receives during the
 * [PointerEventPass.Main] pass is not consumed.
 * - It will call [onStop] if [onStart] has been called and the last [PointerInputChange] it
 * receives during the [PointerEventPass.Main] pass has an up change, consumed or not, indicating
 * the press gesture indication should end.
 * - It will call [onCancel] if movement has been consumed by the time of the
 * [PointerEventPass.Final] pass, indicating that the press gesture indication should end because
 * something moved.
 *
 * This gesture detector always consumes the down change during the [PointerEventPass.Main] pass.
 */
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
@Deprecated(
    "Gesture filters are deprecated. Use Modifier.clickable or Modifier.pointerInput and " +
        "detectTapGestures instead",
    replaceWith = ReplaceWith(
        """
            pointerInput {
                detectTapGestures(onPress = {
                    onStart?.invoke(it)
                    val success = tryAwaitRelease()
                    if (success) {
                       onStop?.invoke()
                    } else {
                       onCancel?.invoke()
                    }
                })
            }""",
        "androidx.compose.ui.input.pointer.pointerInput",
        "androidx.compose.foundation.gestures.detectTapGestures"
    )
)
fun Modifier.pressIndicatorGestureFilter(
    onStart: ((Offset) -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "pressIndicatorGestureFilter"
        properties["onStart"] = onStart
        properties["onStop"] = onStop
        properties["onCancel"] = onCancel
        properties["enabled"] = enabled
    }
) {
    val filter = remember { PressIndicatorGestureFilter() }
    filter.onStart = onStart
    filter.onStop = onStop
    filter.onCancel = onCancel
    filter.setEnabled(enabled)
    PointerInputModifierImpl(filter)
}

internal class PressIndicatorGestureFilter : PointerInputFilter() {
    /**
     * Called if the first pointer's down change was not consumed by the time this gesture
     * filter receives it in the [PointerEventPass.Main] pass.
     *
     * This callback should be used to indicate that the press state should be shown.  An [Offset]
     * is provided to indicate where the first pointer made contact with this gesrure detector.
     */
    var onStart: ((Offset) -> Unit)? = null

    /**
     * Called if onStart was attempted to be called (it may have been null), no pointer movement
     * was consumed, and the last pointer went up (consumed or not).
     *
     * This should be used for removing visual feedback that indicates that the press has ended with
     * a completed press released gesture.
     */
    var onStop: (() -> Unit)? = null

    /**
     * Called if onStart was attempted to be called (it may have been null), and either:
     * 1. Pointer movement was consumed by the time [PointerEventPass.Final] reaches this
     * gesture filter.
     * 2. [setEnabled] is called with false.
     * 3. This [PointerInputFilter] is removed from the hierarchy, or it has no descendants
     * to define it's position or size.
     * 4. The Compose root is notified that it will no longer receive input, and thus onStop
     * will never be reached (For example, the Android View that hosts compose receives
     * MotionEvent.ACTION_CANCEL).
     *
     * This should be used for removing visual feedback that indicates that the press gesture was
     * cancelled.
     */
    var onCancel: (() -> Unit)? = null

    private var state = State.Idle

    /**
     * Sets whether this [PointerInputFilter] is enabled.  True by default.
     *
     * When enabled, this [PointerInputFilter] will act normally.
     *
     * When disabled, this [PointerInputFilter] will not process any input.  No aspects
     * of any [PointerInputChange]s will be consumed and no callbacks will be called.
     *
     * If the last callback that was attempted to be called was [onStart] ([onStart] may have
     * been false) and [enabled] is false, [onCancel] will be called.
     */
    // TODO(shepshapard): Remove 'setEnabled'.  It serves no purpose anymore.
    fun setEnabled(enabled: Boolean) {
        if (state == State.Started) {
            // If the state is Started and we were passed true, we don't want to change it to
            // Enabled.
            // If the state is Started and we were passed false, we can set to Disabled and
            // call the cancel callback.
            if (!enabled) {
                state = State.Disabled
                onCancel?.invoke()
            }
        } else {
            // If the state is anything but Started, just set the state according to the value
            // we were passed.
            state =
                if (enabled) {
                    State.Idle
                } else {
                    State.Disabled
                }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Initial && state == State.Started) {
            changes.fastForEach {
                if (it.changedToDown()) {
                    it.consumeDownChange()
                }
            }
        }

        if (pass == PointerEventPass.Main) {

            if (state == State.Idle && changes.all { it.changedToDown() }) {
                // If we have not yet started and all of the changes changed to down, we are
                // starting.
                state = State.Started
                onStart?.invoke(changes.first().position)
            } else if (state == State.Started) {
                if (changes.all { it.changedToUpIgnoreConsumed() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    state = State.Idle
                    onStop?.invoke()
                } else if (!changes.anyPointersInBounds(bounds)) {
                    // If all of the down pointers are currently out of bounds, we should cancel
                    // as this indicates that the user does not which to trigger a press based
                    // event.
                    state = State.Idle
                    onCancel?.invoke()
                }
            }

            if (state == State.Started) {
                changes.fastForEach {
                    it.consumeDownChange()
                }
            }
        }

        if (
            pass == PointerEventPass.Final &&
            state == State.Started &&
            changes.fastAny { it.anyPositionChangeConsumed() }
        ) {
            // On the final pass, if we have started and any of the changes had consumed
            // position changes, we cancel.
            state = State.Idle
            onCancel?.invoke()
        }
    }

    override fun onCancel() {
        if (state == State.Started) {
            state = State.Idle
            onCancel?.invoke()
        }
    }

    private enum class State {
        Disabled, Idle, Started
    }
}
