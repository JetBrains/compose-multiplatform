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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.gestures

import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.ScrollCallback
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import java.util.concurrent.CancellationException
import kotlin.math.sign

@Suppress("ModifierInspectorInfo")
internal actual fun Modifier.touchScrollable(
    scrollCallback: ScrollCallback,
    orientation: Orientation,
    enabled: Boolean,
    startScrollImmediately: Boolean
): Modifier = composed(
    factory = {
        val orientationState = rememberUpdatedState(orientation)
        val startScrollImmediatelyState = rememberUpdatedState(startScrollImmediately)
        val enabledState = rememberUpdatedState(enabled)
        val scrollCallbackState = rememberUpdatedState(scrollCallback)
        val scrollLambda: suspend PointerInputScope.() -> Unit = remember {
            {
                forEachGesture {
                    dragForEachGesture(
                        orientation = orientationState,
                        enabled = enabledState,
                        startScrollImmediately = startScrollImmediatelyState,
                        callback = scrollCallbackState
                    )
                }
            }
        }
        Modifier.pointerInput(scrollLambda)
    }
)

private suspend fun PointerInputScope.dragForEachGesture(
    orientation: State<Orientation>,
    enabled: State<Boolean>,
    startScrollImmediately: State<Boolean>,
    callback: State<ScrollCallback>
) {
    fun isVertical() = orientation.value == Orientation.Vertical
    fun PointerInputChange.consume(amount: Float) = this.consumePositionChange(
        consumedDx = if (isVertical()) 0f else amount,
        consumedDy = if (isVertical()) amount else 0f
    )

    var initialDelta = 0f
    val startEvent = awaitPointerEventScope {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled.value) {
            null
        } else if (startScrollImmediately.value) {
            // since we start immediately we don't wait for slop and set initial delta to 0
            initialDelta = 0f
            down
        } else {
            val onSlopPassed = { event: PointerInputChange, overSlop: Float ->
                event.consume(event.position.run { if (isVertical()) y else x })
                initialDelta = overSlop
            }
            val result = if (isVertical()) {
                awaitVerticalTouchSlopOrCancellation(down.id, onSlopPassed)
            } else {
                awaitHorizontalTouchSlopOrCancellation(down.id, onSlopPassed)
            }
            if (enabled.value) result else null
        }
    }
    startEvent?.let { drag ->
        try {
            awaitPointerEventScope {
                val overSlopOffset =
                    if (isVertical()) Offset(0f, initialDelta) else Offset(initialDelta, 0f)
                val adjustedStart = drag.position -
                    overSlopOffset * sign(drag.position.run { if (isVertical()) y else x })
                callback.value.onStart(adjustedStart)
                callback.value.onScroll(initialDelta)
                val velocityTracker = VelocityTracker()
                velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                val dragTick = { event: PointerInputChange ->
                    velocityTracker.addPosition(event.uptimeMillis, event.position)
                    val delta = event.positionChange().run { if (isVertical()) y else x }
                    callback.value.onScroll(delta)
                    event.consume(delta)
                }
                val isDragSuccessful = if (isVertical()) {
                    verticalDrag(drag.id, dragTick)
                } else {
                    horizontalDrag(drag.id, dragTick)
                }
                if (isDragSuccessful) {
                    callback.value.onStop(
                        velocityTracker.calculateVelocity().run { if (isVertical()) y else x }
                    )
                } else {
                    callback.value.onCancel()
                }
            }
        } catch (cancellation: CancellationException) {
            callback.value.onCancel()
            throw cancellation
        }
    }
}

@Suppress("DEPRECATION")
internal actual fun Modifier.mouseScrollable(
    scrollCallback: ScrollCallback,
    orientation: Orientation
): Modifier = this