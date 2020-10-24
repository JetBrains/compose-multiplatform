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

import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.milliseconds
import androidx.test.filters.SmallTest
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.moveBy
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.swipe
import androidx.ui.test.swipeWithVelocity
import androidx.ui.test.up
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class DraggableTest {

    @get:Rule
    val rule = createComposeRule()

    private val draggableBoxTag = "dragTag"

    @Test
    fun draggable_horizontalDrag() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                duration = 100.milliseconds
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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                duration = 100.milliseconds
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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                duration = 100.milliseconds

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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
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
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        rule.runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_noNestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        rule.setContent {
            Box {
                Box(
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .testTag(draggableBoxTag)
                        .preferredSize(300.dp)
                        .draggable(Orientation.Horizontal) {
                            outerDrag += it
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .preferredSize(300.dp)
                            .draggable(Orientation.Horizontal) { delta ->
                                innerDrag += delta / 2
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                duration = 300.milliseconds
            )
        }
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            // draggable doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(0f)
        }
    }

    @Test
    fun draggable_interactionState() {
        val interactionState = InteractionState()

        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                interactionState = interactionState
            ) {}
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performGesture {
                up()
            }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    @Test
    fun draggable_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitDraggableBox by mutableStateOf(true)

        rule.setContent {
            Box {
                if (emitDraggableBox) {
                    Box(
                        modifier = Modifier
                            .testTag(draggableBoxTag)
                            .preferredSize(100.dp)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                interactionState = interactionState
                            ) {}
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        rule.onNodeWithTag(draggableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        // Dispose draggable
        rule.runOnIdle {
            emitDraggableBox = false
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    private fun setDraggableContent(draggableFactory: @Composable () -> Modifier) {
        rule.setContent {
            Box {
                val draggable = draggableFactory()
                Box(
                    modifier = Modifier
                        .testTag(draggableBoxTag)
                        .preferredSize(100.dp)
                        .then(draggable)
                )
            }
        }
    }
}