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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.withRunningRecomposer
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.materialize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Manages suspending pointer input for a single gesture detector, passed in
 * [gestureDetector]. The [width] and [height] of the LayoutNode may
 * be provided.
 */
internal class SuspendingGestureTestUtil(
    val width: Int = 10,
    val height: Int = 10,
    private val gestureDetector: suspend PointerInputScope.() -> Unit,
) {
    private var nextPointerId = 0L
    private val activePointers = mutableMapOf<PointerId, PointerInputChange>()
    private var pointerInputFilter: PointerInputFilter? = null
    private var lastTime = 0L
    private var isExecuting = false

    /**
     * Executes the block in composition, creating a gesture detector from
     * [gestureDetector]. The [down], [moveTo], and [up] can then be
     * called within [block].
     *
     * This is not reentrant.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun executeInComposition(block: suspend SuspendingGestureTestUtil.() -> Unit) {
        check(!isExecuting) { "executeInComposition is not reentrant" }
        try {
            isExecuting = true
            runTest {
                val frameClock = TestFrameClock()

                withContext(frameClock) {
                    composeGesture(block)
                }
            }
        } finally {
            isExecuting = false
            pointerInputFilter = null
            lastTime = 0
            activePointers.clear()
        }
    }

    private suspend fun composeGesture(block: suspend SuspendingGestureTestUtil.() -> Unit) {
        withRunningRecomposer { recomposer ->
            compose(recomposer) {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalViewConfiguration provides TestViewConfiguration(
                        minimumTouchTargetSize = DpSize.Zero
                    )
                ) {
                    pointerInputFilter = currentComposer
                        .materialize(Modifier.pointerInput(Unit, gestureDetector)) as
                        PointerInputFilter
                }
            }
            yield()
            block()
            // Pointer input effects will loop indefinitely; fully cancel them.
            recomposer.cancel()
        }
    }

    /**
     * Creates a new pointer being down at [timeDiffMillis] from the previous event. The position
     * [x], [y] is used for the touch point. The [PointerInputChange] may be mutated
     * prior to invoking the change on all passes in [initial], if provided. All other "down"
     * pointers will also be included in the change event.
     */
    suspend fun down(
        x: Float,
        y: Float,
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiffMillis
        val change = PointerInputChange(
            id = PointerId(nextPointerId++),
            uptimeMillis = lastTime,
            position = Offset(x, y),
            pressed = true,
            previousUptimeMillis = lastTime,
            previousPosition = Offset(x, y),
            previousPressed = false,
            isInitiallyConsumed = false
        )
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        return change
    }

    /**
     * Creates a new pointer being down at [timeDiffMillis] from the previous event. The position
     * [offset] is used for the touch point. The [PointerInputChange] may be mutated
     * prior to invoking the change on all passes in [initial], if provided. All other "down"
     * pointers will also be included in the change event.
     */
    suspend fun down(
        offset: Offset = Offset.Zero,
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        return down(offset.x, offset.y, timeDiffMillis, main, final, initial)
    }

    /**
     * Raises the pointer. [initial] will be called on the [PointerInputChange] prior to the
     * event being invoked on all passes. After [up], the event will no longer participate
     * in other events. [timeDiffMillis] indicates the time from the previous event that
     * the [up] takes place.
     */
    suspend fun PointerInputChange.up(
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiffMillis
        val change = PointerInputChange(
            id = id,
            previousUptimeMillis = uptimeMillis,
            previousPressed = pressed,
            previousPosition = position,
            uptimeMillis = lastTime,
            pressed = false,
            position = position,
            isInitiallyConsumed = false
        )
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        activePointers.remove(change.id)
        return change
    }

    /**
     * Moves an existing [down] pointer to a new position at [timeDiffMillis] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveTo(
        x: Float,
        y: Float,
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiffMillis
        val change = PointerInputChange(
            id = id,
            previousUptimeMillis = uptimeMillis,
            previousPosition = position,
            previousPressed = pressed,
            uptimeMillis = lastTime,
            position = Offset(x, y),
            pressed = true,
            isInitiallyConsumed = false
        )
        initial(change)
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        return change
    }

    /**
     * Moves an existing [down] pointer to a new position at [timeDiffMillis] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveTo(
        offset: Offset,
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange = moveTo(offset.x, offset.y, timeDiffMillis, main, final, initial)

    /**
     * Moves an existing [down] pointer to a new position at [timeDiffMillis] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveBy(
        offset: Offset,
        timeDiffMillis: Long = 10,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange = moveTo(
        position.x + offset.x,
        position.y + offset.y,
        timeDiffMillis,
        main,
        final,
        initial
    )

    /**
     * Removes all pointers from the active pointers. This can simulate a faulty pointer stream
     * for robustness testing.
     */
    fun clearPointerStream() {
        activePointers.clear()
    }

    /**
     * Updates all changes so that all events are at the current time.
     */
    private fun updateCurrentTime() {
        val currentTime = lastTime
        activePointers.entries.forEach { entry ->
            val change = entry.value
            if (change.uptimeMillis != currentTime) {
                entry.setValue(
                    PointerInputChange(
                        id = change.id,
                        previousUptimeMillis = change.uptimeMillis,
                        previousPressed = change.pressed,
                        previousPosition = change.position,
                        uptimeMillis = currentTime,
                        pressed = change.pressed,
                        position = change.position,
                        isInitiallyConsumed = false
                    )
                )
            }
        }
    }

    /**
     * Invokes events for all passes.
     */
    private suspend fun invokeOverAllPasses(
        change: PointerInputChange,
        initial: PointerInputChange.() -> Unit,
        main: PointerInputChange.() -> Unit,
        final: PointerInputChange.() -> Unit
    ) {
        updateCurrentTime()
        val event = PointerEvent(activePointers.values.toList())
        val size = IntSize(width, height)

        change.initial()
        pointerInputFilter?.onPointerEvent(event, PointerEventPass.Initial, size)
        yield()
        change.main()
        pointerInputFilter?.onPointerEvent(event, PointerEventPass.Main, size)
        yield()
        change.final()
        pointerInputFilter?.onPointerEvent(event, PointerEventPass.Final, size)
        yield()
    }

    @OptIn(InternalComposeApi::class)
    private fun compose(
        recomposer: Recomposer,
        block: @Composable () -> Unit
    ) {
        ControlledComposition(
            EmptyApplier(),
            recomposer
        ).apply {
            composeContent {
                @Suppress("UNCHECKED_CAST")
                val fn = block as (Composer, Int) -> Unit
                fn(currentComposer, 0)
            }
            applyChanges()
            verifyConsistent()
        }
    }

    internal class TestFrameClock : MonotonicFrameClock {

        private val frameCh = Channel<Long>()

        @Suppress("unused")
        suspend fun frame(frameTimeNanos: Long) {
            frameCh.send(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCh.receive())
    }

    class EmptyApplier : Applier<Unit> {
        override val current: Unit = Unit
        override fun down(node: Unit) {}
        override fun up() {}
        override fun insertTopDown(index: Int, instance: Unit) {
            error("Unexpected")
        }
        override fun insertBottomUp(index: Int, instance: Unit) {
            error("Unexpected")
        }
        override fun remove(index: Int, count: Int) {
            error("Unexpected")
        }
        override fun move(from: Int, to: Int, count: Int) {
            error("Unexpected")
        }
        override fun clear() {}
    }
}