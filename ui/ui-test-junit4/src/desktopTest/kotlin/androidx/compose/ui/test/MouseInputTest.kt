/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test


/**
 * Tests the mouse-event sending functionality of the test framework.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
class MouseInputTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testPerformClick() {
        var clicked = false
        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .clickable {
                        clicked = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performClick()
            assertTrue(clicked, "Click event not received")
        }
    }

    @Test
    fun testMouseClick() {
        var clicked = false
        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .clickable {
                        clicked = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                click()
            }
            assertTrue(clicked, "Mouse click event not received")
        }
    }

    @Test
    fun testMousePressDragAndRelease() {
        var pressDetected = false
        var dragDetected = false
        var releaseDetected = false

        fun assertState(expectedPress: Boolean, expectedDrag: Boolean, expectedRelease: Boolean) {
            assertEquals(expectedPress, pressDetected, "Press detection mismatch")
            assertEquals(expectedDrag, dragDetected, "Drag detection mismatch")
            assertEquals(expectedRelease, releaseDetected, "Release detection mismatch")
        }

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown().also { it.consume() }
                            pressDetected = true

                            awaitDragOrCancellation(down.id) ?: return@awaitEachGesture
                            dragDetected = true

                            waitForUpOrCancellation() ?: return@awaitEachGesture
                            releaseDetected = true
                        }
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                press()
            }
            assertState(expectedPress = true, expectedDrag = false, expectedRelease = false)

            performMouseInput {
                moveBy(Offset(x = 50f, y = 50f))
            }
            assertState(expectedPress = true, expectedDrag = true, expectedRelease = false)

            performMouseInput {
                release()
            }
            assertState(expectedPress = true, expectedDrag = true, expectedRelease = true)
        }
    }

    private fun Modifier.onPointerEvent(
        eventType: PointerEventType,
        onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
    ) = pointerInput(eventType, onEvent) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == eventType) {
                    onEvent(event)
                }
            }
        }
    }

    @Test
    fun testMouseEnterExit() {
        var mouseEnterDetected = false
        var mouseExitDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        mouseEnterDetected = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        mouseExitDetected = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                enter()
            }
            assertTrue(mouseEnterDetected, "Mouse entered event not detected")

            performMouseInput {
                exit()
            }
            assertTrue(mouseExitDetected, "Mouse exited event not detected")
        }
    }

    @Test
    fun updatePointerToDoesNotSendMoveEvent() {
        var mouseMoveDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onPointerEvent(PointerEventType.Move) {
                        mouseMoveDetected = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                updatePointerTo(Offset(10f, 10f))
                press()
                release()
            }
            assertFalse(mouseMoveDetected, "Mouse move detected")
        }
    }

    @Test
    fun testScroll() {
        var scrollDelta = Offset.Unspecified

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                scrollDelta = event.changes.first().scrollDelta
                            }
                        }
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                scroll(50f, ScrollWheel.Vertical)
            }
            assertEquals(Offset(0f, 50f), scrollDelta, "Wrong vertical scroll delta detected")

            performMouseInput {
                scroll(30f, ScrollWheel.Horizontal)
            }
            assertEquals(Offset(30f, 0f), scrollDelta, "Wrong horizontal scroll delta detected")
        }
    }

    @Test
    fun testClick() {
        var clickDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                        clickDetected = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                click()
            }
            assertTrue(clickDetected, "Mouse click not detected")
        }
    }

    @Test
    fun testRightClick() {
        var rightClickDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                        rightClickDetected = true
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                rightClick()
            }
            assertTrue(rightClickDetected, "Mouse right-click not detected")
        }
    }

    @Test
    fun testDoubleClick() {
        var doubleClickDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onDoubleClick = {
                            doubleClickDetected = true
                        },
                        onClick = {}
                    )
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                doubleClick()
            }
            assertTrue(doubleClickDetected, "Mouse double-click not detected")
        }
    }

    @Test
    fun testTripleClick() {
        var clickCount = 0

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onClick = {
                            clickCount += 1
                        }
                    )
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                tripleClick()
            }
            assertEquals(3, clickCount, "Mouse triple-click not detected")
        }
    }

    @Test
    fun testLongClick() {
        var longClickDetected = false

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onLongClick = {
                            longClickDetected = true
                        },
                        onClick = {}
                    )
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                longClick()
            }
            assertTrue(longClickDetected, "Mouse long-click not detected")
        }
    }

    @Test
    fun testDragAndDrop() {
        var dragOffset = Offset.Zero

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit){
                        detectDragGestures { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                dragAndDrop(
                    start = Offset(10f, 10f),
                    end = Offset(20f, 30f),
                )
            }
            assertOffsetEquals(
                expected = Offset(10f, 20f),
                actual = dragOffset,
                message = "Wrong drag-and-drop offset detected"
            )
        }
    }

    @Test
    fun testSmoothScroll() {
        var scrollDelta = Offset.Zero

        rule.setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                scrollDelta += event.changes.first().scrollDelta
                            }
                        }
                    }
            )
        }

        with(rule.onNodeWithTag("tag")) {
            performMouseInput {
                smoothScroll(50f, scrollWheel = ScrollWheel.Vertical)
            }
            assertOffsetEquals(
                expected = Offset(0f, 50f),
                actual = scrollDelta,
                message = "Wrong vertical scroll delta detected"
            )
            scrollDelta = Offset.Zero

            performMouseInput {
                scroll(30f, ScrollWheel.Horizontal)
            }
            assertOffsetEquals(
                Offset(30f, 0f),
                scrollDelta,
                message = "Wrong horizontal scroll delta detected"
            )
        }
    }

    private fun assertOffsetEquals(
        expected: Offset,
        actual: Offset,
        toleratedDistance: Float = 0.5f,
        message: String? = null
    ) {
        assertTrue(
            actual = (expected - actual).getDistance() < toleratedDistance,
            message = (if (message == null) "" else "$message; ") +
                "expected=$expected, actual=$actual, toleratedDistance=$toleratedDistance"
        )
    }
}