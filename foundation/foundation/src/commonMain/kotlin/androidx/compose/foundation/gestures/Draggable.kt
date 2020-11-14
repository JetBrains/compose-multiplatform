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
import androidx.compose.runtime.onDispose
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.Direction
import androidx.compose.ui.gesture.ScrollCallback
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.scrollGestureFilter
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density

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
 * @param canDrag callback to indicate whether or not dragging is allowed for given [Direction]
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
    canDrag: (Direction) -> Boolean = { enabled },
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: (velocity: Float) -> Unit = {},
    onDrag: Density.(Float) -> Unit
): Modifier = composed(
    factory = {
        val density = AmbientDensity.current
        onDispose {
            interactionState?.removeInteraction(Interaction.Dragged)
        }

        scrollGestureFilter(
            scrollCallback = object : ScrollCallback {

                override fun onStart(downPosition: Offset) {
                    if (enabled) {
                        interactionState?.addInteraction(Interaction.Dragged)
                        onDragStarted(downPosition)
                    }
                }

                override fun onScroll(scrollDistance: Float): Float {
                    if (!enabled) return scrollDistance
                    val toConsume = if (reverseDirection) scrollDistance * -1 else scrollDistance
                    with(density) { onDrag(toConsume) }
                    // we explicitly disallow nested scrolling in draggable, as it should be
                    // accessible via Modifier.scrollable. For drags, usually nested dragging is not
                    // required
                    return scrollDistance
                }

                override fun onCancel() {
                    if (enabled) {
                        interactionState?.removeInteraction(Interaction.Dragged)
                        onDragStopped(0f)
                    }
                }

                override fun onStop(velocity: Float) {
                    if (enabled) {
                        interactionState?.removeInteraction(Interaction.Dragged)
                        onDragStopped(if (reverseDirection) velocity * -1 else velocity)
                    }
                }
            },
            orientation = orientation,
            canDrag = canDrag,
            startDragImmediately = startDragImmediately
        )
    },
    inspectorInfo = debugInspectorInfo {
        name = "draggable"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionState"] = interactionState
        properties["startDragImmediately"] = startDragImmediately
        properties["canDrag"] = canDrag
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["onDrag"] = onDrag
    }
)
