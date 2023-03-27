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
import androidx.compose.foundation.gestures.TransformEvent.TransformDelta
import androidx.compose.foundation.gestures.TransformEvent.TransformStarted
import androidx.compose.foundation.gestures.TransformEvent.TransformStopped
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

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
        val updatePanZoomLock = rememberUpdatedState(lockRotationOnZoomPan)
        val channel = remember { Channel<TransformEvent>(capacity = Channel.UNLIMITED) }
        if (enabled) {
            LaunchedEffect(state) {
                while (isActive) {
                    var event = channel.receive()
                    if (event !is TransformStarted) continue
                    try {
                        state.transform(MutatePriority.UserInput) {
                            while (event !is TransformStopped) {
                                (event as? TransformDelta)?.let {
                                    transformBy(it.zoomChange, it.panChange, it.rotationChange)
                                }
                                event = channel.receive()
                            }
                        }
                    } catch (_: CancellationException) {
                        // ignore the cancellation and start over again.
                    }
                }
            }
        }
        val block: suspend PointerInputScope.() -> Unit = remember {
            {
                coroutineScope {
                    awaitEachGesture {
                        try {
                            detectZoom(updatePanZoomLock, channel)
                        } catch (exception: CancellationException) {
                            if (!isActive) throw exception
                        } finally {
                            channel.trySend(TransformStopped)
                        }
                    }
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

private sealed class TransformEvent {
    object TransformStarted : TransformEvent()
    object TransformStopped : TransformEvent()
    class TransformDelta(
        val zoomChange: Float,
        val panChange: Offset,
        val rotationChange: Float
    ) : TransformEvent()
}

private suspend fun AwaitPointerEventScope.detectZoom(
    panZoomLock: State<Boolean>,
    channel: Channel<TransformEvent>
) {
    var rotation = 0f
    var zoom = 1f
    var pan = Offset.Zero
    var pastTouchSlop = false
    val touchSlop = viewConfiguration.touchSlop
    var lockedToPanZoom = false
    awaitFirstDown(requireUnconsumed = false)
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
                    channel.trySend(TransformStarted)
                }
            }

            if (pastTouchSlop) {
                val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                if (effectiveRotation != 0f ||
                    zoomChange != 1f ||
                    panChange != Offset.Zero
                ) {
                    channel.trySend(TransformDelta(zoomChange, panChange, effectiveRotation))
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
