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
import androidx.compose.ui.TransformOrigin
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.ConsumedData
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputData
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientViewConfiguration
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Uptime
import androidx.compose.ui.unit.milliseconds
import androidx.compose.ui.platform.WindowManager
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
@OptIn(ExperimentalPointerInput::class)
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
                    LayoutNode(0, 0, width, height, pointerInputFilter!! as Modifier)
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

    @Suppress("SameParameterValue")
    @OptIn(ExperimentalLayoutNodeApi::class)
    private fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int, modifier: Modifier = Modifier) =
        LayoutNode().apply {
            this.modifier = modifier
            measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks("not supported") {
                override fun measure(
                    measureScope: MeasureScope,
                    measurables: List<Measurable>,
                    constraints: Constraints
                ): MeasureResult =
                    measureScope.layout(x2 - x, y2 - y) {}
            }
            attach(MockOwner())
            measure(Constraints.fixed(x2 - x, y2 - y))
            place(x, y)
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

    @OptIn(
        ExperimentalFocus::class,
        ExperimentalLayoutNodeApi::class,
        InternalCoreApi::class
    )
    private class MockOwner(
        val position: IntOffset = IntOffset.Zero,
        override val root: LayoutNode = LayoutNode()
    ) : Owner {
        val onRequestMeasureParams = mutableListOf<LayoutNode>()
        val onAttachParams = mutableListOf<LayoutNode>()
        val onDetachParams = mutableListOf<LayoutNode>()

        override val hapticFeedBack: HapticFeedback
            get() = TODO("Not yet implemented")
        override val clipboardManager: ClipboardManager
            get() = TODO("Not yet implemented")
        override val textToolbar: TextToolbar
            get() = TODO("Not yet implemented")
        override val autofillTree: AutofillTree
            get() = TODO("Not yet implemented")
        override val autofill: Autofill?
            get() = TODO("Not yet implemented")
        override val density: Density
            get() = Density(1f)
        override val semanticsOwner: SemanticsOwner
            get() = TODO("Not yet implemented")
        override val textInputService: TextInputService
            get() = TODO("Not yet implemented")
        override val focusManager: FocusManager
            get() = TODO("Not yet implemented")
        override val windowManager: WindowManager
            get() = TODO("Not yet implemented")
        override val fontLoader: Font.ResourceLoader
            get() = TODO("Not yet implemented")
        override val layoutDirection: LayoutDirection
            get() = LayoutDirection.Ltr
        override var showLayoutBounds: Boolean = false
        override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }

        override fun onRequestMeasure(layoutNode: LayoutNode) {
            onRequestMeasureParams += layoutNode
        }

        override fun onRequestRelayout(layoutNode: LayoutNode) {
        }

        override val hasPendingMeasureOrLayout = false

        override fun onAttach(node: LayoutNode) {
            onAttachParams += node
        }

        override fun onDetach(node: LayoutNode) {
            onDetachParams += node
        }

        override fun calculatePosition(): IntOffset = position

        override fun requestFocus(): Boolean = false

        @ExperimentalKeyInput
        override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = false

        override fun measureAndLayout() {
        }

        override fun createLayer(
            drawBlock: (Canvas) -> Unit,
            invalidateParentLayer: () -> Unit
        ): OwnedLayer {
            return object : OwnedLayer {
                override val layerId: Long
                    get() = 0

                override fun updateLayerProperties(
                    scaleX: Float,
                    scaleY: Float,
                    alpha: Float,
                    translationX: Float,
                    translationY: Float,
                    shadowElevation: Float,
                    rotationX: Float,
                    rotationY: Float,
                    rotationZ: Float,
                    cameraDistance: Float,
                    transformOrigin: TransformOrigin,
                    shape: Shape,
                    clip: Boolean
                ) {
                }

                override fun move(position: IntOffset) {
                }

                override fun resize(size: IntSize) {
                }

                override fun drawLayer(canvas: Canvas) {
                    drawBlock(canvas)
                }

                override fun updateDisplayList() {
                }

                override fun invalidate() {
                }

                override fun destroy() {
                }

                override fun getMatrix(matrix: Matrix) {
                }
            }
        }

        override fun onSemanticsChange() {
        }

        override val measureIteration: Long = 0
        override val viewConfiguration: ViewConfiguration
            get() = TestViewConfiguration()
    }

    @OptIn(ExperimentalComposeApi::class)
    class EmptyApplier : Applier<Unit> {
        override val current: Unit = Unit
        override fun down(node: Unit) {}
        override fun up() {}
        override fun insert(index: Int, instance: Unit) {
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