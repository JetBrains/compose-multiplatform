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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DraggableTest {

    @get:Rule
    val rule = createComposeRule()

    private val draggableBoxTag = "dragTag"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun draggable_horizontalDrag() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_verticalDrag() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Vertical) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_verticalDrag_newState() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Vertical) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun draggable_startStop() {
        var startTrigger = 0f
        var stopTrigger = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStarted = { startTrigger += 1 },
                onDragStopped = { stopTrigger += 1 }
            ) {}
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable_disabledWontCallLambda() {
        var total = 0f
        val enabled = mutableStateOf(true)
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, enabled = enabled.value) {
                total += it
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun draggable_velocityProxy() {
        var velocityTriggered = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = {
                    velocityTriggered = it
                }
            ) {}
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                durationMillis = 100

            )
        }
        rule.runOnIdle {
            assertThat(velocityTriggered - 112f).isLessThan(0.1f)
        }
    }

    @Test
    fun draggable_startWithoutSlop_ifAnimating() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, startDragImmediately = true) {
                total += it
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun draggable_cancel_callsDragStop() {
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
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    // regression test for b/176971558
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draggable_immediateStart_callsStopWithoutSlop() {
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
        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            this.press()
        }
        rule.runOnIdle {
            assertThat(dragStarted).isEqualTo(1f)
        }
        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            this.release()
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_callsDragStop_whenNewState() {
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
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            state.value = DraggableState { /* do nothing */ }
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_resumesNormally_whenInterruptedWithHigherPriority() = runBlocking {
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
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        val prevTotal = rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            assertThat(total).isGreaterThan(0f)
            total
        }
        state.drag(MutatePriority.PreventUserInput) {
            dragBy(123f)
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123f)
            assertThat(dragStopped).isEqualTo(1f)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            up()
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(total).isGreaterThan(prevTotal + 123f)
        }
    }

    @Test
    fun draggable_noNestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        rule.setContent {
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
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            // draggable doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(0f)
        }
    }

    @Test
    fun draggable_interactionSource() {
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

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                up()
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitDraggableBox by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
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

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose draggable
        rule.runOnIdle {
            emitDraggableBox = false
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draggable_cancelMidDown_shouldContinueWithNextDown() {
        var total = 0f

        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }

        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            cancel()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }

        assertThat(total).isGreaterThan(0f)
    }

    @Test
    fun testInspectableValue() {
        rule.setContent {
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
                "stateFactory",
            )
        }
    }

    private fun setDraggableContent(draggableFactory: @Composable () -> Modifier) {
        rule.setContent {
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