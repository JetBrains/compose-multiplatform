/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.isLinux
import androidx.compose.ui.isWindows
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.math.max

// Note that on Linux some tests are flaky. Swing event listener's on Linux has non-deterministic
// nature. To avoid flaky'ness we use delays
// (see description of `delay` parameter in TestUtils.runApplicationTest).
// It is not a good solution, but it works.

// TODO(demin): figure out how can we fix flaky tests on Linux
// TODO(demin): fix fullscreen tests on macOs

@OptIn(ExperimentalComposeUiApi::class)
class WindowStateTest {
    @Test
    fun `manually close window`() = runApplicationTest {
        var window: ComposeWindow? = null
        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = { isOpen = false }) {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
    }

    @Test
    fun `programmatically close window`() = runApplicationTest {
        var window: ComposeWindow? = null
        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = { isOpen = false }) {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
    }

    @Test
    fun `programmatically open and close nested window`() = runApplicationTest {
        var parentWindow: ComposeWindow? = null
        var childWindow: ComposeWindow? = null
        var isParentOpen by mutableStateOf(true)
        var isChildOpen by mutableStateOf(false)

        launchApplication {
            if (isParentOpen) {
                Window(onCloseRequest = {}) {
                    parentWindow = this.window

                    if (isChildOpen) {
                        Window(onCloseRequest = {}) {
                            childWindow = this.window
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()

        isChildOpen = true
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isTrue()

        isChildOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childWindow?.isShowing).isFalse()

        isParentOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isFalse()
    }

    @Test
    fun `set size and position before show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(242.dp, 242.dp)
        )

        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(200, 200))
        assertThat(window?.location).isEqualTo(Point(242, 242))

        exitApplication()
    }

    @Test
    fun `change position after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.position = WindowPosition(242.dp, (242).dp)
        awaitIdle()
        assertThat(window?.location).isEqualTo(Point(242, 242))

        exitApplication()
    }

    @Test
    fun `change size after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.size = DpSize(250.dp, 200.dp)
        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(250, 200))

        exitApplication()
    }

    @Test
    fun `center window`() = runApplicationTest {
        fun Rectangle.center() = Point(x + width / 2, y + height / 2)
        fun JFrame.center() = bounds.center()
        fun JFrame.screenCenter() = graphicsConfiguration.bounds.center()
        infix fun Point.maxDistance(other: Point) = max(abs(x - other.x), abs(y - other.y))

        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window!!.center() maxDistance window!!.screenCenter() < 250)

        exitApplication()
    }

    @Test
    fun `remember position after reattach`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null
        var isWindow1 by mutableStateOf(true)

        launchApplication {
            if (isWindow1) {
                Window(onCloseRequest = {}, state) {
                    window1 = this.window
                }
            } else {
                Window(onCloseRequest = {}, state) {
                    window2 = this.window
                }
            }
        }

        awaitIdle()

        state.position = WindowPosition(242.dp, 242.dp)
        awaitIdle()
        assertThat(window1?.location == Point(242, 242))

        isWindow1 = false
        awaitIdle()
        assertThat(window2?.location == Point(242, 242))

        exitApplication()
    }

    @Test
    fun `state position should be specified after attach`() = runApplicationTest {
        val state = WindowState(size = DpSize(200.dp, 200.dp))

        launchApplication {
            Window(onCloseRequest = {}, state) {
            }
        }

        assertThat(state.position.isSpecified).isFalse()

        awaitIdle()
        assertThat(state.position.isSpecified).isTrue()

        exitApplication()
    }

    @Test
    fun `enter fullscreen`() = runApplicationTest(useDelay = isLinux) {
        // TODO(demin): fix macOs. We disabled it because it is not deterministic.
        //  If we set in skiko SkiaLayer.setFullscreen(true) then isFullscreen still returns false
        assumeTrue(isWindows || isLinux)

        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.placement = WindowPlacement.Fullscreen
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Fullscreen)

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Floating)

        exitApplication()
    }

    @Test
    fun maximize() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Maximized)

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Floating)

        exitApplication()
    }

    @Test
    fun minimize() = runApplicationTest {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        awaitIdle()
        assertThat(window?.isMinimized).isTrue()

        state.isMinimized = false
        awaitIdle()
        assertThat(window?.isMinimized).isFalse()

        exitApplication()
    }

    @Test
    fun `maximize and minimize `() = runApplicationTest {
        // macOs can't be maximized and minimized at the same time
        assumeTrue(isWindows || isLinux)

        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window?.isMinimized).isTrue()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Maximized)

        exitApplication()
    }

    @Test
    fun `restore size and position after maximize`() = runApplicationTest {
        // Swing/macOs can't re-change isMaximized in a deterministic way:
//        fun main() = runBlocking(Dispatchers.Swing) {
//            val window = ComposeWindow()
//            window.size = Dimension(200, 200)
//            window.isVisible = true
//            window.isMaximized = true
//            delay(100)
//            window.isMaximized = false  // we cannot do that on macOs (window is still animating)
//            delay(1000)
//            println(window.isMaximized) // prints true
//        }
//        Swing/Linux has animations and sometimes adds an offset to the size/position
        assumeTrue(isWindows)

        val state = WindowState(
            size = DpSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Maximized)
        assertThat(window?.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window?.location).isNotEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Floating)
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        exitApplication()
    }

    @Test
    fun `restore size and position after fullscreen`() = runApplicationTest {
//        Swing/Linux has animations and sometimes adds an offset to the size/position
        assumeTrue(isWindows)

        val state = WindowState(
            size = DpSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Fullscreen
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Fullscreen)
        assertThat(window?.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window?.location).isNotEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Floating)
        assertThat(window?.size).isEqualTo(Dimension(201, 203))
        assertThat(window?.location).isEqualTo(Point(196, 257))

        exitApplication()
    }

    @Test
    fun `maximize window before show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Maximized,
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Maximized)

        exitApplication()
    }

    @Test
    fun `minimize window before show`() = runApplicationTest {
        // Linux/macos doesn't support this:
//        fun main() = runBlocking(Dispatchers.Swing) {
//            val window = ComposeWindow()
//            window.size = Dimension(200, 200)
//            window.isMinimized = true
//            window.isVisible = true
//            delay(2000)
//            println(window.isMinimized) // prints false
//        }
        // TODO(demin): can we minimize after window.isVisible?
        assumeTrue(isWindows)

        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            isMinimized = true
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.isMinimized).isTrue()

        exitApplication()
    }

    @Test
    fun `enter fullscreen before show`() = runApplicationTest {
        // TODO(demin): probably we have a bug in skiko (we can't change fullscreen on macOs before
        //  showing the window)
        assumeTrue(isLinux || isWindows)

        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Fullscreen,
        )
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window?.placement).isEqualTo(WindowPlacement.Fullscreen)

        exitApplication()
    }

    @Test
    fun `save state`() = runApplicationTest {
        val initialState = WindowState()
        val newState = WindowState(
            placement = WindowPlacement.Maximized,
            size = DpSize(42.dp, 42.dp),
            position = WindowPosition(3.dp, 3.dp),
            isMinimized = true,
        )

        var isOpen by mutableStateOf(true)
        var index by mutableStateOf(0)
        val states = mutableListOf<WindowState>()

        launchApplication {
            val saveableStateHolder = rememberSaveableStateHolder()
            saveableStateHolder.SaveableStateProvider(index) {
                val state = rememberWindowState()

                LaunchedEffect(Unit) {
                    state.placement = newState.placement
                    state.isMinimized = newState.isMinimized
                    state.size = newState.size
                    state.position = newState.position
                    states.add(state)
                }
            }

            if (isOpen) {
                Window(onCloseRequest = {}) {}
            }
        }

        awaitIdle()
        assertThat(states.size == 1)

        index = 1
        awaitIdle()
        assertThat(states.size == 2)

        index = 0
        awaitIdle()
        assertThat(states.size == 3)

        assertThat(states[0].placement == initialState.placement)
        assertThat(states[0].isMinimized == initialState.isMinimized)
        assertThat(states[0].size == initialState.size)
        assertThat(states[0].position == initialState.position)
        assertThat(states[2].placement == newState.placement)
        assertThat(states[2].isMinimized == newState.isMinimized)
        assertThat(states[2].size == newState.size)
        assertThat(states[2].position == newState.position)

        isOpen = false
    }

    @Test
    fun `set window height by its content`() = runApplicationTest(useDelay = isLinux) {
        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(300.dp, Dp.Unspecified))

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.width).isEqualTo(300)
        assertThat(window.contentSize.height).isEqualTo(200)
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))

        exitApplication()
    }

    @Test
    fun `set window width by its content`() = runApplicationTest(useDelay = isLinux) {
        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(Dp.Unspecified, 300.dp))

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.height).isEqualTo(300)
        assertThat(window.contentSize.width).isEqualTo(400)
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))

        exitApplication()
    }

    @Test
    fun `set window size by its content`() = runApplicationTest(useDelay = isLinux) {
        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(Dp.Unspecified, Dp.Unspecified))

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(window.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))

        exitApplication()
    }

    @Test
    fun `set window size by its content when window is on the screen`() = runApplicationTest(
        useDelay = isLinux
    ) {
        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(100.dp, 100.dp))

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = state
            ) {
                window = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()

        state.size = DpSize(Dp.Unspecified, Dp.Unspecified)
        awaitIdle()
        assertThat(window.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(window.size.width.dp, window.size.height.dp))

        exitApplication()
    }

    @Test
    fun `change visible`() = runApplicationTest {
        lateinit var window: ComposeWindow

        var visible by mutableStateOf(false)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, visible = visible) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.isVisible).isEqualTo(false)

        visible = true
        awaitIdle()
        assertThat(window.isVisible).isEqualTo(true)

        exitApplication()
    }

    @Test
    fun `invisible window should be active`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, visible = false) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))

        exitApplication()
    }

    @Test
    fun `start invisible undecorated window`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, visible = false, undecorated = true) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))

        exitApplication()
    }

    private val Window.contentSize
        get() = Dimension(
            size.width - insets.left - insets.right,
            size.height - insets.top - insets.bottom,
        )
}