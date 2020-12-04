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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.dispatch.MonotonicFrameClock
import androidx.compose.runtime.withRunningRecomposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.ConsumedData
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputData
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.materialize
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime
import androidx.compose.ui.unit.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runBlockingTest
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
    private var lastTime = Duration.Zero
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
            runBlockingTest {
                val frameClock = TestFrameClock()

                withContext(frameClock) {
                    composeGesture(block)
                }
            }
        } finally {
            isExecuting = false
            pointerInputFilter = null
            lastTime = Duration.Zero
            activePointers.clear()
        }
    }

    private suspend fun composeGesture(block: suspend SuspendingGestureTestUtil.() -> Unit) {
        withRunningRecomposer { recomposer ->
            compose(recomposer) {
                Providers(
                    AmbientDensity provides Density(1f),
                    AmbientViewConfiguration provides TestViewConfiguration()
                ) {
                    pointerInputFilter = currentComposer
                        .materialize(Modifier.pointerInput(gestureDetector)) as
                        PointerInputFilter
                }
            }
            yield()
            block()
        }
    }

    /**
     * Creates a new pointer being down at [timeDiff] from the previous event. The position
     * [x], [y] is used for the touch point. The [PointerInputChange] may be mutated
     * prior to invoking the change on all passes in [initial], if provided. All other "down"
     * pointers will also be included in the change event.
     */
    suspend fun down(
        x: Float,
        y: Float,
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiff
        val change = PointerInputChange(
            PointerId(nextPointerId++),
            PointerInputData(
                Uptime.Boot + lastTime,
                Offset(x, y),
                true
            ),
            PointerInputData(
                Uptime.Boot + lastTime,
                Offset(x, y),
                false
            ),
            ConsumedData(Offset.Zero, false)
        )
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        return change
    }

    /**
     * Creates a new pointer being down at [timeDiff] from the previous event. The position
     * [offset] is used for the touch point. The [PointerInputChange] may be mutated
     * prior to invoking the change on all passes in [initial], if provided. All other "down"
     * pointers will also be included in the change event.
     */
    suspend fun down(
        offset: Offset = Offset.Zero,
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        return down(offset.x, offset.y, timeDiff, main, final, initial)
    }

    /**
     * Raises the pointer. [initial] will be called on the [PointerInputChange] prior to the
     * event being invoked on all passes. After [up], the event will no longer participate
     * in other events. [timeDiff] indicates the [Duration] from the previous event that
     * the [up] takes place.
     */
    suspend fun PointerInputChange.up(
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiff
        val change = copy(
            previous = current,
            current = PointerInputData(
                Uptime.Boot + lastTime,
                current.position,
                false
            ),
            consumed = ConsumedData()
        )
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        activePointers.remove(change.id)
        return change
    }

    /**
     * Moves an existing [down] pointer to a new position at [timeDiff] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveTo(
        x: Float,
        y: Float,
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange {
        lastTime += timeDiff
        val change = copy(
            previous = current,
            current = PointerInputData(
                Uptime.Boot + lastTime,
                Offset(x, y),
                true
            ),
            consumed = ConsumedData()
        )
        initial(change)
        activePointers[change.id] = change
        invokeOverAllPasses(change, initial, main, final)
        return change
    }

    /**
     * Moves an existing [down] pointer to a new position at [timeDiff] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveTo(
        offset: Offset,
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange = moveTo(offset.x, offset.y, timeDiff, main, final, initial)

    /**
     * Moves an existing [down] pointer to a new position at [timeDiff] from the most recent
     * event. [initial] will be called on the [PointerInputChange] prior to invoking the event
     * on all passes.
     */
    suspend fun PointerInputChange.moveBy(
        offset: Offset,
        timeDiff: Duration = 10.milliseconds,
        main: PointerInputChange.() -> Unit = {},
        final: PointerInputChange.() -> Unit = {},
        initial: PointerInputChange.() -> Unit = {}
    ): PointerInputChange = moveTo(
        current.position.x + offset.x,
        current.position.y + offset.y,
        timeDiff,
        main,
        final,
        initial
    )

    /**
     * Updates all changes so that all events are at the current time.
     */
    private fun updateCurrentTime() {
        val currentTime = Uptime.Boot + lastTime
        activePointers.entries.forEach { entry ->
            val change = entry.value
            if (change.current.uptime != currentTime) {
                entry.setValue(
                    change.copy(
                        previous = change.current,
                        current = change.current.copy(uptime = currentTime),
                        consumed = ConsumedData()
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

    @OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)
    private fun compose(
        recomposer: Recomposer,
        block: @Composable () -> Unit
    ): Composer<Unit> {
        return Composer(
            SlotTable(),
            EmptyApplier(),
            recomposer
        ).apply {
            composeInitial {
                @Suppress("UNCHECKED_CAST")
                val fn = block as (Composer<*>, Int) -> Unit
                fn(this, 0)
            }
            applyChanges()
            slotTable.verifyWellFormed()
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

    @OptIn(ExperimentalComposeApi::class)
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

    private class TestViewConfiguration : ViewConfiguration {
        override val longPressTimeout: Duration
            get() = 500.milliseconds

        override val doubleTapTimeout: Duration
            get() = 300.milliseconds

        override val doubleTapMinTime: Duration
            get() = 40.milliseconds

        override val touchSlop: Float
            get() = 18f
    }
}