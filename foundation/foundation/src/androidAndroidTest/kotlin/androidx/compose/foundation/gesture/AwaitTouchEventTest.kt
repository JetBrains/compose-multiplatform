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

package androidx.compose.foundation.gesture

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AwaitTouchEventTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    // Long press tests; assumes long press timeout is 500 milliseconds
    @Test
    fun awaitLongPressOrCancellationTest_longClick_assertTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                longClick()
            }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_click_assertNotTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                click()
            }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_fingerDownShortDelayUp_assertNotTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(center)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                move()
                up()
            }

        rule.mainClock.advanceTimeBy(400L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_fingerDownShortDelayCancel_assertNotTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(center)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                move()
                cancel()
            }

        rule.mainClock.advanceTimeBy(400L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_multipleFingersDown_assertTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        val firstFingerId = 1
        val secondFingerId = 2

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(firstFingerId, center)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(secondFingerId, centerRight)
            }

        rule.mainClock.advanceTimeBy(400L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    // Tests this set of events:
    // 1. one finger down
    // 2. second finger down
    // 3. first finger up
    // 4. second finger down through long press timeout
    // Should still count the long press
    @Test
    fun awaitLongPressOrCancellationTest_multipleFingersDownFirstUp_assertTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        val firstFinger = 1
        val secondFinger = 2

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(firstFinger, center)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(secondFinger, centerRight)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                up(firstFinger)
            }

        rule.mainClock.advanceTimeBy(300L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_fingerDownMoveOutsideBounds_assertNotTriggers() {
        var counter = 0

        rule.setContent {
            Box {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(center)
            }

        rule.mainClock.advanceTimeBy(100L)

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                // Moves touch outside of bounds
                moveTo(Offset(right + 10, centerY))
            }

        // With this time change, time will have passed the long press threshold, but because the
        // touch moved outside the bounds, it won't have been counted.
        rule.mainClock.advanceTimeBy(400L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }
    }

    @Test
    fun awaitLongPressOrCancellationTest_fingerDownConsumedInParent_assertNotTriggers() {
        var counter = 0

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag("MyLongPressParent")
                    .pointerInput(Unit) {
                        forEachGesture() {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    if (event.type == PointerEventType.Move) {
                                        event.changes[0].consume()
                                    }
                                }
                            }
                        }
                    }
            ) {
                BasicText(
                    "LongPressText",
                    modifier = Modifier
                        .testTag("myLongPress")
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    awaitLongPressOrCancellation(down.id)?.let {
                                        counter++
                                    }
                                }
                            }
                        }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myLongPress")
            .performTouchInput {
                down(center)
                moveTo(centerRight)
            }

        rule.mainClock.advanceTimeBy(400L)

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(0)
        }
    }
}
