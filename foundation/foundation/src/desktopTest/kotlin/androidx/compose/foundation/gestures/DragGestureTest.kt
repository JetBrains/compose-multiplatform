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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.DefaultViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import kotlin.math.ceil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
class DragGestureTest {

    @Test
    fun `draggable by mouse primary button`() {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: Offset? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            var dragOffset = Offset.Zero

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .onDrag(
                            enabled = true,
                            onDragStart = { offset -> dragStartResult = offset },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                dragOffset = it
                                onDragCounter++
                            }
                        )
                )
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = PointerButton.Primary)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop, 5f)
            )

            assertEquals(Offset(5f, 5f), dragStartResult)
            assertEquals(ceil(viewConfiguration.touchSlop), ceil(dragOffset.x))
            assertEquals(1, onDragCounter)
            assertEquals(0f, 0f)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop, 15f)
            )
            assertEquals(0f, dragOffset.x)
            assertEquals(10f, dragOffset.y)
            assertEquals(2, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop, 25f)
            )
            assertEquals(0f, dragOffset.x)
            assertEquals(10f, dragOffset.y)
            assertEquals(3, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = Offset(viewConfiguration.touchSlop, 15f),
                button = PointerButton.Primary,
            )
            assertEquals(-5f, dragOffset.x)
            assertEquals(-10f, dragOffset.y)
            assertTrue(dragEnded)
            assertFalse(dragCanceled)
            assertEquals(4, onDragCounter)
        }
    }

    @Test
    fun `draggable by mouse secondary button, ignores primary button`() {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: Offset? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            var dragOffset = Offset.Zero

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .onDrag(
                            enabled = true,
                            matcher = PointerMatcher.mouse(PointerButton.Secondary),
                            onDragStart = { offset -> dragStartResult = offset },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                dragOffset = it
                                onDragCounter++
                            }
                        )
                )
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = PointerButton.Primary)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop, 5f)
            )
            assertEquals(null, dragStartResult)

            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = PointerButton.Secondary)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop, 5f)
            )
            assertEquals(Offset(5f, 5f), dragStartResult)
        }
    }

    @Test
    fun `draggable by touch`() {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: Offset? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            var dragOffset = Offset.Zero

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .onDrag(
                            enabled = true,
                            matcher = PointerMatcher.touch,
                            onDragStart = { offset -> dragStartResult = offset },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                dragOffset = it
                                onDragCounter++
                            }
                        )
                )
            }

            // Note: touch slop is different from mouse slop,
            // so values a bit different in this test comparing with mouse drag
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f), type = PointerType.Touch)
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), type = PointerType.Touch)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop + 5f, 5f),
                type = PointerType.Touch
            )

            assertEquals(Offset(5f, 5f), dragStartResult)
            assertEquals(5f, ceil(dragOffset.x))
            assertEquals(1, onDragCounter)
            assertEquals(0f, 0f)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(10f + viewConfiguration.touchSlop, 15f),
                type = PointerType.Touch
            )
            assertEquals(0f, dragOffset.x)
            assertEquals(10f, dragOffset.y)
            assertEquals(2, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(10f + viewConfiguration.touchSlop, 25f),
                type = PointerType.Touch
            )
            assertEquals(0f, dragOffset.x)
            assertEquals(10f, dragOffset.y)
            assertEquals(3, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = Offset(5f + viewConfiguration.touchSlop, 15f),
                type = PointerType.Touch
            )
            assertEquals(-5f, dragOffset.x)
            assertEquals(-10f, dragOffset.y)
            assertTrue(dragEnded)
            assertFalse(dragCanceled)
            assertEquals(4, onDragCounter)
        }
    }

    @Test
    fun `draggable by touch, ignores mouse`() {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: Offset? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            var dragOffset = Offset.Zero

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .onDrag(
                            enabled = true,
                            matcher = PointerMatcher.touch,
                            onDragStart = { offset -> dragStartResult = offset },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                dragOffset = it
                                onDragCounter++
                            }
                        )
                )
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f), type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), type = PointerType.Mouse)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop + 5f, 5f),
                type = PointerType.Mouse
            )
            assertEquals(null, dragStartResult)
            // Now release the button to complete the mouse gesture. Otherwise, the touch gesture won't be started.
            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), type = PointerType.Mouse)

            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), type = PointerType.Touch)
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(5f + viewConfiguration.touchSlop + 5f, 5f),
                type = PointerType.Touch
            )
            assertEquals(Offset(5f, 5f), dragStartResult)
        }
    }

    @Test
    @Ignore // remove Ignore if needed later when startDragOnLongPress mode supported
    fun `draggable by mouse OnLongPress primary button`() = runBlocking {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)

        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->

            var dragStartResult: Offset? = null
            var dragCanceled = false
            var dragEnded = false
            var onDragCounter = 0
            var dragOffset = Offset.Zero

            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .onDrag(
                            enabled = true,
                            onDragStart = { offset -> dragStartResult = offset },
                            onDragCancel = { dragCanceled = true },
                            onDragEnd = { dragEnded = true },
                            onDrag = {
                                dragOffset = it
                                onDragCounter++
                            }
                        )
                )
            }

            val downButtons = PointerButtons(isPrimaryPressed = true)
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), buttons = downButtons)

            delay(viewConfiguration.longPressTimeoutMillis * 2)

            assertEquals(
                Offset(5f, 5f),
                dragStartResult
            )
            assertEquals(0, onDragCounter)
            assertEquals(0f, 0f)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 10f), buttons = downButtons)
            assertEquals(0f, dragOffset.x)
            assertEquals(5f, dragOffset.y)
            assertEquals(1, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 10f), buttons = downButtons)
            assertEquals(10f, dragOffset.x)
            assertEquals(0f, dragOffset.y)
            assertEquals(2, onDragCounter)
            assertFalse(dragCanceled)
            assertFalse(dragEnded)

            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f))
            assertEquals(-10f, dragOffset.x)
            assertEquals(-5f, dragOffset.y)
            assertTrue(dragEnded)
            assertFalse(dragCanceled)
            assertEquals(3, onDragCounter)
        }
    }
}
