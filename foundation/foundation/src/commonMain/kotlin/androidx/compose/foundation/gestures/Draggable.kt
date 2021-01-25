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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sign

/**
 * Configure touch dragging for the UI element in a single [Orientation]. The drag distance is
 * reported to [onDrag] as a single [Float] value in pixels.
 *
 * The common usecase for this component is when you need to be able to drag something
 * inside the component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [dragGestureFilter] instead.
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.compose.foundation.samples.DraggableSample
 *
 * @param orientation orientation of the drag
 * @param enabled whether or not drag is enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param interactionState [InteractionState] that will be updated when this draggable is
 * being dragged, using [Interaction.Dragged].
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 * prevent other gesture detectors from reacting to "down" events (in order to block composed
 * press-based gestures).  This is intended to allow end users to "catch" an animating widget by
 * pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag has been started after touch slop
 * has been passed, with starting position provided
 * @param onDragStopped callback that will be invoked when drag stops, with velocity provided
 * @param onDrag callback to be invoked when the drag occurs with the delta dragged from the
 * previous event. [Density] provided in the scope for the convenient conversion between px and dp
 */
fun Modifier.draggable(
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionState: InteractionState? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: (velocity: Float) -> Unit = {},
    onDrag: Density.(Float) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "draggable"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionState"] = interactionState
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["onDrag"] = onDrag
    }
) {
    DisposableEffect(interactionState) {
        onDispose {
            interactionState?.removeInteraction(Interaction.Dragged)
        }
    }
    val orientationState = rememberUpdatedState(orientation)
    val enabledState = rememberUpdatedState(enabled)
    val reverseDirectionState = rememberUpdatedState(reverseDirection)
    val startImmediatelyState = rememberUpdatedState(startDragImmediately)
    val interactionStateState = rememberUpdatedState(interactionState)
    val onDragStartedState = rememberUpdatedState(onDragStarted)
    val onDragLambdaState =
        rememberUpdatedState<Density.(Float) -> Unit> { onDrag(it) }
    val onDragStoppedState = rememberUpdatedState(onDragStopped)
    val dragBlock: suspend PointerInputScope.() -> Unit = remember {
        {
            forEachGesture {
                dragForEachGesture(
                    orientation = orientationState,
                    enabled = enabledState,
                    interactionState = interactionStateState,
                    reverseDirection = reverseDirectionState,
                    startDragImmediately = startImmediatelyState,
                    onDragStarted = onDragStartedState,
                    onDragStopped = onDragStoppedState,
                    onDrag = onDragLambdaState
                )
            }
        }
    }
    Modifier.pointerInput(dragBlock)
}

private suspend fun PointerInputScope.dragForEachGesture(
    orientation: State<Orientation>,
    enabled: State<Boolean>,
    reverseDirection: State<Boolean>,
    interactionState: State<InteractionState?>,
    startDragImmediately: State<Boolean>,
    onDragStarted: State<(startedPosition: Offset) -> Unit>,
    onDragStopped: State<(velocity: Float) -> Unit>,
    onDrag: State<Density.(Float) -> Unit>
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
        } else if (startDragImmediately.value) {
            // since we start immediately we don't wait for slop and set initial delta to 0
            initialDelta = 0f
            down
        } else {
            val postTouchSlop = { event: PointerInputChange, offset: Float ->
                event.consume(event.position.run { if (isVertical()) y else x })
                initialDelta = offset
            }
            val afterSlopResult = if (isVertical()) {
                awaitVerticalTouchSlopOrCancellation(down.id, postTouchSlop)
            } else {
                awaitHorizontalTouchSlopOrCancellation(down.id, postTouchSlop)
            }
            if (enabled.value) afterSlopResult else null
        }
    }
    startEvent?.let { drag ->
        try {
            awaitPointerEventScope {
                val overSlopOffset =
                    if (isVertical()) Offset(0f, initialDelta) else Offset(initialDelta, 0f)
                val adjustedStart = drag.position -
                    overSlopOffset * sign(drag.position.run { if (isVertical()) y else x })
                if (enabled.value) onDragStarted.value.invoke(adjustedStart)
                if (enabled.value) interactionState.value?.addInteraction(Interaction.Dragged)
                onDrag.value.invoke(
                    this,
                    if (reverseDirection.value) initialDelta * -1 else initialDelta
                )
                val velocityTracker = VelocityTracker()
                velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                val dragTick = { event: PointerInputChange ->
                    velocityTracker.addPosition(event.uptimeMillis, event.position)
                    val delta = event.positionChange().run { if (isVertical()) y else x }
                    event.consume(delta)
                    if (enabled.value) {
                        onDrag.value.invoke(this, if (reverseDirection.value) delta * -1 else delta)
                    }
                }
                val isDragSuccessful = if (isVertical()) {
                    verticalDrag(drag.id, dragTick)
                } else {
                    horizontalDrag(drag.id, dragTick)
                }
                if (enabled.value) {
                    interactionState.value?.removeInteraction(Interaction.Dragged)
                    val velocity =
                        if (isDragSuccessful) {
                            velocityTracker.calculateVelocity().run { if (isVertical()) y else x }
                        } else {
                            0f
                        }
                    onDragStopped.value.invoke(
                        if (reverseDirection.value) velocity * -1 else velocity
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            if (enabled.value) {
                interactionState.value?.removeInteraction(Interaction.Dragged)
                onDragStopped.value.invoke(0f)
            }
            throw cancellation
        }
    }
}
