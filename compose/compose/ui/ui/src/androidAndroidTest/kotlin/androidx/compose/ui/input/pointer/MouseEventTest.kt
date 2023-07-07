/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@OptIn(ExperimentalTestApi::class)
class MouseEventTest {
    @get:Rule
    val rule = createComposeRule()

    val tag = "Tagged Layout"

    @Test
    fun absentEnterSent() {
        // Sometimes the first mouse event is an ACTION_DOWN. The "Enter" event should be sent
        // before that.

        val events = mutableListOf<PointerEventType>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                    .testTag(tag)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                events += awaitPointerEvent().type
                            }
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset.Zero)
            press()
        }

        assertThat(events).hasSize(2)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        assertThat(events[1]).isEqualTo(PointerEventType.Press)
    }

    @Test
    fun dontSendExitWhenWePress() {
        // Sometimes the ACTION_HOVER_EXIT isn't sent when a touch event comes after a mouse event.

        val events = mutableListOf<PointerEventType>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                    .testTag(tag)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                events += awaitPointerEvent().type
                            }
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset.Zero)
        }.performTouchInput {
            down(Offset.Zero)
            up()
        }

        assertThat(events).hasSize(4)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        assertThat(events[1]).isEqualTo(PointerEventType.Move)
        assertThat(events[2]).isEqualTo(PointerEventType.Press)
        assertThat(events[3]).isEqualTo(PointerEventType.Release)
    }

    @Test
    fun pressMoveRelease() {
        // There was a bug in which a pointer event handler wasn't removed from the hit test
        // if the pointer moved outside of the bounds while down.

        val events = mutableListOf<PointerEventType>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                        .testTag(tag)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    events += awaitPointerEvent().type
                                }
                            }
                        }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset.Zero)
            press()
            moveTo(Offset(-10f, -10f))
            release()
            press()
            release()
        }

        assertThat(events).hasSize(4)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        assertThat(events[1]).isEqualTo(PointerEventType.Press)
        assertThat(events[2]).isEqualTo(PointerEventType.Exit)
        assertThat(events[3]).isEqualTo(PointerEventType.Release)
    }

    @Test
    fun relayoutUnderCursor() {
        // When a mouse cursor is hovering and the layout that it is in moves, the hover
        // state should update.

        val alignment = mutableStateOf(Alignment.BottomCenter)
        val events = createRelayoutComposition(alignment)

        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset.Zero)
        }

        assertThat(events).hasSize(1)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        events.clear()

        alignment.value = Alignment.TopCenter

        waitForPointerUpdate()

        assertThat(events).hasSize(1)
        assertThat(events[0]).isEqualTo(PointerEventType.Exit)
    }

    private fun waitForPointerUpdate() {
        rule.waitForIdle()
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).postDelayed({ latch.countDown() }, 200)
        latch.await(2, TimeUnit.SECONDS)
        rule.waitForIdle()
    }

    @Test
    fun noSimulatedEventAfterUp() {
        // After an "up" event, there's no reason to send a simulated event

        val alignment = mutableStateOf(Alignment.BottomCenter)
        val events = createRelayoutComposition(alignment)

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset.Zero)
            up()
        }

        assertThat(events).hasSize(2)
        events.clear()

        alignment.value = Alignment.TopCenter

        waitForPointerUpdate()

        assertThat(events).hasSize(0)
    }

    @Test
    fun noSimulatedEventAfterHoverExit() {
        // After an "up" event, there's no reason to send a simulated event

        val alignment = mutableStateOf(Alignment.BottomCenter)
        val events = createRelayoutComposition(alignment)

        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset.Zero)
            exit(Offset(-1f, -1f))
        }

        assertThat(events).hasSize(2)
        assertThat(events[0]).isEqualTo(PointerEventType.Enter)
        assertThat(events[1]).isEqualTo(PointerEventType.Exit)
        events.clear()

        alignment.value = Alignment.TopCenter

        waitForPointerUpdate()

        assertThat(events).hasSize(0)
    }

    private fun createRelayoutComposition(
        alignment: State<Alignment>
    ): MutableList<PointerEventType> {
        val events = mutableListOf<PointerEventType>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(alignment.value).size(50.dp)
                        .testTag(tag)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    events += awaitPointerEvent().type
                                }
                            }
                        }
                )
            }
        }

        rule.waitForIdle()
        return events
    }
}