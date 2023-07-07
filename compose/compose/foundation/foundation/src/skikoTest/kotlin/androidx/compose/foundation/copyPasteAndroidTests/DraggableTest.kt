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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isLessThan
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.hasSize
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.animateTo
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalTestApi::class)
class DraggableTest {

    private val draggableBoxTag = "dragTag"

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun draggable_horizontalDrag() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_verticalDrag() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Vertical) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draggable_dragWithPrimaryMouseButton() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            dragAndDrop(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            dragAndDrop(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            dragAndDrop(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draggable_dragWithSecondaryMouseButton() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            dragAndDropSecondary(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle { // no drag expected
            assertThat(total).isEqualTo(0f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun MouseInjectionScope.dragAndDropSecondary(
        start: Offset,
        end: Offset,
        durationMillis: Long = 300L
    ) {
        updatePointerTo(start)
        press(MouseButton.Secondary)
        animateTo(end, durationMillis)
        release(MouseButton.Secondary)
    }

    @Test
    fun draggable_verticalDrag_newState() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Vertical) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_startStop() = runSkikoComposeUiTest {
        var startTrigger = 0
        var stopTrigger = 0
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStarted = { startTrigger += 1 },
                onDragStopped = { stopTrigger += 1 }
            ) {}
        }
        runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable_disabledWontCallLambda() = runSkikoComposeUiTest {
        var total = 0f
        val enabled = mutableStateOf(true)
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, enabled = enabled.value) {
                total += it
            }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val prevTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            enabled.value = false
            total
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun draggable_velocityProxy() = runSkikoComposeUiTest {
        var velocityTriggered = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = {
                    velocityTriggered = it
                }
            ) {}
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                durationMillis = 100

            )
        }
        runOnIdle {
            assertThat(velocityTriggered - 112f).isLessThan(0.1f)
        }
    }

    @Test
    fun draggable_startWithoutSlop_ifAnimating() = runSkikoComposeUiTest {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, startDragImmediately = true) {
                total += it
            }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun draggable_cancel_callsDragStop() = runSkikoComposeUiTest {
        var total = 0f
        var dragStopped = 0f
        setDraggableContent {
            if (total < 20f) {
                Modifier.draggable(
                    Orientation.Horizontal,
                    onDragStopped = { dragStopped += 1 },
                    startDragImmediately = true
                ) { total += it }
            } else Modifier
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isGreaterThan(0)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    // regression test for b/176971558
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draggable_immediateStart_callsStopWithoutSlop() = runSkikoComposeUiTest {
        var total = 0f
        var dragStopped = 0f
        var dragStarted = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                onDragStarted = { dragStarted += 1 },
                startDragImmediately = true
            ) { total += it }
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            this.press()
        }
        runOnIdle {
            assertThat(dragStarted).isEqualTo(1f)
        }
        onNodeWithTag(draggableBoxTag).performMouseInput {
            this.release()
        }
        runOnIdle {
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_callsDragStop_whenNewState() = runSkikoComposeUiTest {
        var total = 0f
        var dragStopped = 0f
        val state = mutableStateOf(
            DraggableState { total += it }
        )
        setDraggableContent {
            if (total < 20f) {
                Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    onDragStopped = { dragStopped += 1 },
                    state = state.value
                )
            } else Modifier
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            state.value = DraggableState { /* do nothing */ }
        }
        runOnIdle {
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun draggable_resumesNormally_whenInterruptedWithHigherPriority() = runSkikoComposeUiTest {
        kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) {
            var total = 0f
            var dragStopped = 0f
            val state = DraggableState {
                total += it
            }
            setDraggableContent {
                if (total < 20f) {
                    Modifier.draggable(
                        orientation = Orientation.Horizontal,
                        onDragStopped = { dragStopped += 1 },
                        state = state
                    )
                } else Modifier
            }
            onNodeWithTag(draggableBoxTag).performTouchInput {
                down(center)
                moveBy(Offset(100f, 100f))
            }
            val prevTotal = runOnIdle {
                assertThat(dragStopped).isEqualTo(0f)
                assertThat(total).isGreaterThan(0)
                total
            }
            state.drag(MutatePriority.PreventUserInput) {
                dragBy(123f)
            }

            runOnIdle {
                assertThat(total).isEqualTo(prevTotal + 123f)
                assertThat(dragStopped).isEqualTo(1f)
            }
            onNodeWithTag(draggableBoxTag).performTouchInput {
                up()
                down(center)
                moveBy(Offset(100f, 100f))
                up()
            }
            runOnIdle {
                assertThat(total).isGreaterThan(prevTotal + 123f)
            }
        }
    }


    @Test
    fun draggable_noNestedDrag()= runSkikoComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .testTag(draggableBoxTag)
                        .size(300.dp)
                        .draggable(Orientation.Horizontal) {
                            outerDrag += it
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .draggable(Orientation.Horizontal) { delta ->
                                innerDrag += delta / 2
                            }
                    )
                }
            }
        }
        onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }
        runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            // draggable doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(0f)
        }
    }

    @Test
    fun draggable_interactionSource() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setDraggableContent {
            scope = rememberCoroutineScope()
            Modifier.draggable(
                Orientation.Horizontal,
                interactionSource = interactionSource
            ) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is DragInteraction.Start }
        }

        onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                up()
            }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Stop }
            assertThat((interactions[1] as DragInteraction.Stop).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_interactionSource_resetWhenDisposed() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var emitDraggableBox by mutableStateOf(true)

        var scope: CoroutineScope? = null

        setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitDraggableBox) {
                    Box(
                        modifier = Modifier
                            .testTag(draggableBoxTag)
                            .size(100.dp)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                interactionSource = interactionSource
                            ) {}
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is DragInteraction.Start }
        }

        // Dispose draggable
        runOnIdle {
            emitDraggableBox = false
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Cancel }
            assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Ignore // TODO: the test fails on skiko targets
    fun draggable_cancelMidDown_shouldContinueWithNextDown() = runSkikoComposeUiTest {
        var total = 0f

        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }

        onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            cancel()
        }

        assertThat(total).isEqualTo(0f)
        onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }

        assertThat(total).isGreaterThan(0f)
    }

    @Test
    @Ignore // TODO: doesn't pass on skiko targets
    fun testInspectableValue() = runSkikoComposeUiTest {
        setContent {
            val modifier = Modifier.draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { }
            ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("draggable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "enabled",
                "canDrag",
                "reverseDirection",
                "interactionSource",
                "startDragImmediately",
                "onDragStarted",
                "onDragStopped",
                "state",
            )
        }
    }

    private fun SkikoComposeUiTest.setDraggableContent(draggableFactory: @Composable () -> Modifier) {
        setContent {
            Box {
                val draggable = draggableFactory()
                Box(
                    modifier = Modifier
                        .testTag(draggableBoxTag)
                        .size(100.dp)
                        .then(draggable)
                )
            }
        }
    }

    private fun Modifier.draggable(
        orientation: Orientation,
        enabled: Boolean = true,
        reverseDirection: Boolean = false,
        interactionSource: MutableInteractionSource? = null,
        startDragImmediately: Boolean = false,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: (velocity: Float) -> Unit = {},
        onDrag: (Float) -> Unit
    ): Modifier = composed {
        val state = rememberDraggableState(onDrag)
        draggable(
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = { onDragStarted(it) },
            onDragStopped = { onDragStopped(it) },
            state = state
        )
    }
}
