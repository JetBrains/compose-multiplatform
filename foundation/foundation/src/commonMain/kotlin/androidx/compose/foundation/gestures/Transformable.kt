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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException
import kotlin.math.PI
import kotlin.math.abs

/**
 * Enable transformation gestures of the modified UI element.
 *
 * Users should update their state themselves using default [TransformableState] and its
 * `onTransformation` callback or by implementing [TransformableState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * @sample androidx.compose.foundation.samples.TransformableSample
 *
 * @param state [TransformableState] of the transformable. Defines how transformation events will be
 * interpreted by the user land logic, contains useful information about on-going events and
 * provides animation capabilities.
 * @param lockRotationOnZoomPan If `true`, rotation is allowed only if touch slop is detected for
 * rotation before pan or zoom motions. If not, pan and zoom gestures will be detected, but rotation
 * gestures will not be. If `false`, once touch slop is reached, all three gestures are detected.
 * @param enabled whether zooming by gestures is enabled or not
 */
fun Modifier.transformable(
    state: TransformableState,
    lockRotationOnZoomPan: Boolean = false,
    enabled: Boolean = true
) = composed(
    factory = {
        val updatedState = rememberUpdatedState(state)
        val updatePanZoomLock = rememberUpdatedState(lockRotationOnZoomPan)
        val block: suspend PointerInputScope.() -> Unit = remember {
            {
                forEachGesture {
                    detectZoom(updatePanZoomLock, updatedState)
                }
            }
        }
        if (enabled) Modifier.pointerInput(Unit, block) else Modifier
    },
    inspectorInfo = debugInspectorInfo {
        name = "transformable"
        properties["state"] = state
        properties["enabled"] = enabled
        properties["lockRotationOnZoomPan"] = lockRotationOnZoomPan
    }
)

private suspend fun PointerInputScope.detectZoom(
    panZoomLock: State<Boolean>,
    state: State<TransformableState>
) {
    var rotation = 0f
    var zoom = 1f
    var pan = Offset.Zero
    var pastTouchSlop = false
    val touchSlop = viewConfiguration.touchSlop
    var lockedToPanZoom = false
    awaitPointerEventScope {
        awaitTwoDowns(requireUnconsumed = false)
    }
    try {
        state.value.transform(MutatePriority.UserInput) {
            awaitPointerEventScope {
                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.fastAny { it.isConsumed }
                    if (!canceled) {
                        val zoomChange = event.calculateZoom()
                        val rotationChange = event.calculateRotation()
                        val panChange = event.calculatePan()

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            rotation += rotationChange
                            pan += panChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize
                            val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                            val panMotion = pan.getDistance()

                            if (zoomMotion > touchSlop ||
                                rotationMotion > touchSlop ||
                                panMotion > touchSlop
                            ) {
                                pastTouchSlop = true
                                lockedToPanZoom = panZoomLock.value && rotationMotion < touchSlop
                            }
                        }

                        if (pastTouchSlop) {
                            val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                            if (effectiveRotation != 0f ||
                                zoomChange != 1f ||
                                panChange != Offset.Zero
                            ) {
                                transformBy(zoomChange, panChange, effectiveRotation)
                            }
                            event.changes.fastForEach {
                                if (it.positionChanged()) {
                                    it.consume()
                                }
                            }
                        }
                    }
                } while (!canceled && event.changes.fastAny { it.pressed })
            }
        }
    } catch (c: CancellationException) {
        // cancelled by higher priority, start listening over
    }
}

/**
 * Reads events until the first down is received. If [requireUnconsumed] is `true` and the first
 * down is consumed in the [PointerEventPass.Main] pass, that gesture is ignored.
 */
private suspend fun AwaitPointerEventScope.awaitTwoDowns(requireUnconsumed: Boolean = true) {
    var event: PointerEvent
    var firstDown: PointerId? = null
    do {
        event = awaitPointerEvent()
        var downPointers = if (firstDown != null) 1 else 0
        event.changes.fastForEach {
            val isDown =
                if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
            val isUp =
                if (requireUnconsumed) it.changedToUp() else it.changedToUpIgnoreConsumed()
            if (isUp && firstDown == it.id) {
                firstDown = null
                downPointers -= 1
            }
            if (isDown) {
                firstDown = it.id
                downPointers += 1
            }
        }
        val satisfied = downPointers > 1
    } while (!satisfied)
}