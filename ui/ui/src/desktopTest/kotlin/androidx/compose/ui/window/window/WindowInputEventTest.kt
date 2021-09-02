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

package androidx.compose.ui.window.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.density
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

@OptIn(ExperimentalComposeUiApi::class)
class WindowInputEventTest {
    @Test
    fun `catch key handlers`() = runApplicationTest {
        var window: ComposeWindow? = null
        val onKeyEventKeys = mutableSetOf<Key>()
        val onPreviewKeyEventKeys = mutableSetOf<Key>()

        fun clear() {
            onKeyEventKeys.clear()
            onPreviewKeyEventKeys.clear()
        }

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                onPreviewKeyEvent = {
                    onPreviewKeyEventKeys.add(it.key)
                    it.key == Key.Q
                },
                onKeyEvent = {
                    onKeyEventKeys.add(it.key)
                    it.key == Key.W
                }
            ) {
                window = this.window
            }
        }

        awaitIdle()

        window?.sendKeyEvent(KeyEvent.VK_Q)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.Q))
        assertThat(onKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_W)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onKeyEventKeys).isEqualTo(setOf(Key.W))

        clear()
        window?.sendKeyEvent(KeyEvent.VK_E)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onKeyEventKeys).isEqualTo(setOf(Key.E))

        exitApplication()
    }

    @Test
    fun `catch key handlers with focused node`() = runApplicationTest {
        var window: ComposeWindow? = null
        val onWindowKeyEventKeys = mutableSetOf<Key>()
        val onWindowPreviewKeyEventKeys = mutableSetOf<Key>()
        val onNodeKeyEventKeys = mutableSetOf<Key>()
        val onNodePreviewKeyEventKeys = mutableSetOf<Key>()

        fun clear() {
            onWindowKeyEventKeys.clear()
            onWindowPreviewKeyEventKeys.clear()
            onNodeKeyEventKeys.clear()
            onNodePreviewKeyEventKeys.clear()
        }

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                onPreviewKeyEvent = {
                    onWindowPreviewKeyEventKeys.add(it.key)
                    it.key == Key.Q
                },
                onKeyEvent = {
                    onWindowKeyEventKeys.add(it.key)
                    it.key == Key.W
                },
            ) {
                window = this.window

                val focusRequester = remember(::FocusRequester)
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent {
                            onNodePreviewKeyEventKeys.add(it.key)
                            it.key == Key.E
                        }
                        .onKeyEvent {
                            onNodeKeyEventKeys.add(it.key)
                            it.key == Key.R
                        }
                )
            }
        }

        awaitIdle()

        window?.sendKeyEvent(KeyEvent.VK_Q)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.Q))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onNodeKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_W)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onWindowKeyEventKeys).isEqualTo(setOf(Key.W))

        clear()
        window?.sendKeyEvent(KeyEvent.VK_E)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onNodeKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_R)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_T)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onWindowKeyEventKeys).isEqualTo(setOf(Key.T))

        exitApplication()
    }

    @Test
    fun `catch mouse press + move + release`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<PointerEvent>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier.fillMaxSize().pointerInput(events) {
                        while (true) {
                            awaitPointerEventScope {
                                events += awaitPointerEvent()
                            }
                        }
                    }
                )
            }
        }
        val density by lazy { window.density.density }

        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 100, y = 50)
        awaitIdle()
        assertThat(events.size).isEqualTo(1)
        assertThat(events.last().pressed).isEqualTo(true)
        assertThat(events.last().position).isEqualTo(Offset(100 * density, 50 * density))

        window.sendMouseEvent(MouseEvent.MOUSE_DRAGGED, x = 90, y = 40)
        awaitIdle()
        assertThat(events.size).isEqualTo(2)
        assertThat(events.last().pressed).isEqualTo(true)
        assertThat(events.last().position).isEqualTo(Offset(90 * density, 40 * density))

        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 80, y = 30)
        awaitIdle()
        assertThat(events.size).isEqualTo(3)
        assertThat(events.last().pressed).isEqualTo(false)
        assertThat(events.last().position).isEqualTo(Offset(80 * density, 30 * density))

        exitApplication()
    }

    @Test
    fun `catch mouse move`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val onMoves = mutableListOf<Offset>()
        var onEnters = 0
        var onExits = 0

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier.fillMaxSize().pointerMoveFilter(
                        onMove = { onMoves.add(it); false },
                        onEnter = { onEnters++; false },
                        onExit = { onExits++; false }
                    )
                )
            }
        }
        val density by lazy { window.density.density }

        awaitIdle()
        assertThat(onMoves.size).isEqualTo(0)
        assertThat(onEnters).isEqualTo(0)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_ENTERED, x = 100, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(0)
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_MOVED, x = 90, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(1)
        assertThat(onMoves.last()).isEqualTo(Offset(90 * density, 50 * density))
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 90, y = 50)
        window.sendMouseEvent(MouseEvent.MOUSE_DRAGGED, x = 80, y = 50)
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 80, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(2)
        assertThat(onMoves.last()).isEqualTo(Offset(80 * density, 50 * density))
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        // TODO(https://github.com/JetBrains/compose-jb/issues/1176) fix catching exit event
//        window.sendMouseEvent(MouseEvent.MOUSE_EXITED, x = 900, y = 500)
//        awaitIdle()
//        assertThat(onMoves.size).isEqualTo(2)
//        assertThat(onEnters).isEqualTo(1)
//        assertThat(onExits).isEqualTo(1)

        exitApplication()
    }

    @Test
    fun `catch mouse scroll`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<MouseScrollEvent>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier.fillMaxSize().mouseScrollFilter { event, _ ->
                        events.add(event)
                        false
                    }
                )
            }
        }

        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.sendMouseWheelEvent(
            MouseEvent.MOUSE_WHEEL,
            x = 100,
            y = 50,
            scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
            scrollAmount = 3
        )
        awaitIdle()
        assertThat(events.size).isEqualTo(1)
        assertThat(events.last().delta).isEqualTo(MouseScrollUnit.Line(3f))

        window.sendMouseWheelEvent(
            MouseEvent.MOUSE_WHEEL,
            x = 100,
            y = 50,
            scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
            scrollAmount = 4
        )
        awaitIdle()
        assertThat(events.size).isEqualTo(2)
        assertThat(events.last().delta).isEqualTo(MouseScrollUnit.Line(4f))

        exitApplication()
    }

    private val PointerEvent.pressed get() = changes.first().pressed
    private val PointerEvent.position get() = changes.first().position
}