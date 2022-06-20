/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.awaitPress
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastAll
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gesture detector with [matcher] that waits for pointer down matching [matcher] and
 * touch slop in any direction and then calls [onDrag] for each drag event.
 * It follows the touch slop detection of [awaitTouchSlopOrCancellation]
 * but will consume the position change automatically once the touch slop has been crossed.
 * [onDragStart] will be called when touch slop in passed with the last known pointer position provided.
 * [onDragEnd] is called after pointer matching [matcher] gets released,
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling this gesture.
 */
@ExperimentalFoundationApi
suspend fun PointerInputScope.detectDragGestures(
    matcher: PointerMatcher = PointerMatcher.Primary,
    onDragStart: (Offset) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit
) {
    val filter: (PointerEvent) -> Boolean = {
        matcher.matches(it)
    }

    while (currentCoroutineContext().isActive) {
        coroutineScope {
            var dragStartedContinuation: Continuation<Boolean>? = null

            // Here we launch 2 coroutines:
            // 1st to wait for drag start and for processing the drag events (dragJob);
            // 2nd to wait for release event matching the filter - meaning the drag should complete;
            // We do this because:
            // 1st coroutine can't always know that drag was completed (fun drag finishes only when all pointers go up).
            // In case of mouse, it means that fun drag will not complete if any mouse button pressed.
            // Therefore, 2nd coroutine will cancel the 1st coroutine (dragJob) when necessary.

            val dragJob = launch {
                awaitPointerEventScope {
                    val press = awaitPress(filter = filter, requireUnconsumed = false)

                    val overSlop = awaitDragStartOnSlop(press)
                    val pointerId = press.changes[0].id

                    if (overSlop != null) {
                        dragStartedContinuation?.resume(true)
                        onDragStart(press.changes[0].position)

                        if (overSlop != Offset.Zero) {
                            onDrag(overSlop)
                        }

                        try {
                            drag(pointerId) {
                                onDrag(it.positionChange())
                                it.consume()
                            }
                        } finally {
                            if (currentEvent.isReleased() && filter(currentEvent)) {
                                onDragEnd()
                            } else {
                                onDragCancel()
                            }
                        }
                    } else {
                        dragStartedContinuation?.resume(false)
                    }
                }
            }

            launch {
                val dragStarted = suspendCoroutine<Boolean> { dragStartedContinuation = it }
                if (dragStarted) {
                    awaitPointerEventScope {
                        while (dragJob.isActive) {
                            val event = awaitPointerEvent()
                            if (event.isReleased() && filter(currentEvent)) {
                                dragJob.cancel()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adds a gesture detector with [matcher] that waits for pointer down matching [matcher] and
 * touch slop in any direction and then calls [onDrag] for each drag event.
 * It follows the touch slop detection of [awaitTouchSlopOrCancellation]
 * but will consume the position change automatically once the touch slop has been crossed.
 * [onDragStart] will be called when touch slop in passed with the last known pointer position provided.
 * [onDragEnd] is called after pointer matching [matcher] gets released,
 * and [onDragCancel] is called if another gesture has consumed pointer input, canceling this gesture.
 */
@ExperimentalFoundationApi
fun Modifier.onDrag(
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    onDragStart: (Offset) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit
): Modifier = composed(
    inspectorInfo = {
        name = "onDrag"
        properties["enabled"] = enabled
        properties["matcher"] = matcher
        properties["onDragStart"] = onDragStart
        properties["onDragCancel"] = onDragCancel
        properties["onDragEnd"] = onDragEnd
        properties["onDrag"] = onDrag
    },
    factory = {
        if (!enabled) return@composed Modifier

        val onDragState = rememberUpdatedState(onDrag)
        val onDragStartState = rememberUpdatedState(onDragStart)
        val onDragEndState = rememberUpdatedState(onDragEnd)
        val onDragCancelState = rememberUpdatedState(onDragCancel)

        Modifier.pointerInput(matcher) {
            detectDragGestures(
                matcher = matcher,
                onDragStart = { onDragStartState.value(it) },
                onDrag = { onDragState.value(it) },
                onDragEnd = { onDragEndState.value() },
                onDragCancel = { onDragCancelState.value() }
            )
        }
    }
)

private fun PointerEvent.isReleased() = type == PointerEventType.Release &&
    changes.fastAll { it.type == PointerType.Mouse } || changes.fastAll { it.changedToUpIgnoreConsumed() }

private suspend fun AwaitPointerEventScope.awaitDragStartOnSlop(initialDown: PointerEvent): Offset? {
    var overSlop = Offset.Zero
    var drag: PointerInputChange?
    do {
        drag = awaitPointerSlopOrCancellation(
            initialDown.changes[0].id,
            initialDown.changes[0].type
        ) { change, over ->
            change.consume()
            overSlop = over
        }
    } while (drag != null && !drag.isConsumed)

    return if (drag == null) {
        null
    } else {
        overSlop
    }
}
