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
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.isLinux
import androidx.compose.ui.isMacOs
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.junit.Assume.assumeTrue
import org.junit.Test

// Note that on Linux some tests are flaky. Swing event listener's on Linux has non-deterministic
// nature. To avoid flakiness we use delays
// (see description of `delay` parameter in TestUtils.runApplicationTest).
// It is not a good solution, but it works.

// TODO(demin): figure out how can we fix flaky tests on Linux

class WindowStateTest {
    @Test
    fun `manually close window`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = { isOpen = false }) {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window.isShowing).isTrue()

        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(window.isShowing).isFalse()
    }

    @Test
    fun `programmatically close window`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                Window(onCloseRequest = { isOpen = false }) {
                    window = this.window
                }
            }
        }

        awaitIdle()
        assertThat(window.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window.isShowing).isFalse()
    }

    @Test
    fun `programmatically open and close nested window`() = runApplicationTest {
        var parentWindow: ComposeWindow? = null
        var childWindow: ComposeWindow? = null
        var isParentOpen by mutableStateOf(true)
        var isChildOpen by mutableStateOf(false)

        launchTestApplication {
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

        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }
        
        awaitIdle()
        assertThat(window)
        assertThat(window.size).isEqualTo(Dimension(200, 200))
        assertThat(window.location).isEqualTo(Point(242, 242))
    }

    @Test
    fun `change position after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.position = WindowPosition(242.dp, (242).dp)
        awaitIdle()
        assertThat(window.location).isEqualTo(Point(242, 242))
    }

    @Test
    fun `change size after show`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(200.dp, 200.dp)
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.size = DpSize(250.dp, 200.dp)
        awaitIdle()
        assertThat(window.size).isEqualTo(Dimension(250, 200))
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
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.center() maxDistance window.screenCenter() < 250)
    }

    @Test
    fun `remember position after reattach`() = runApplicationTest(useDelay = isLinux) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null
        var isWindow1 by mutableStateOf(true)

        launchTestApplication {
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
    }

    @Test
    fun `state position should be specified after attach`() = runApplicationTest(
        useDelay = isLinux
    ) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
            }
        }

        assertThat(state.position.isSpecified).isFalse()

        awaitIdle()
        assertThat(state.position.isSpecified).isTrue()
    }

    @Test
    fun `enter fullscreen`() = runApplicationTest(
        useDelay = isLinux || isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.placement = WindowPlacement.Fullscreen
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Fullscreen)

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/3003
    @Test
    fun `WindowState placement after showing fullscreen window`() = runApplicationTest(
        useDelay = isLinux || isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowState(placement = WindowPlacement.Fullscreen)

        launchTestApplication {
            Window(onCloseRequest = {}, state) { }
        }

        awaitIdle()

        assertThat(state.placement).isEqualTo(WindowPlacement.Fullscreen)
    }

    @Test
    fun maximize() = runApplicationTest(useDelay = isLinux || isMacOs) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
    }

    @Test
    fun minimize() = runApplicationTest(useDelay = isMacOs, delayMillis = 1000) {
        val state = WindowState(size = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        awaitIdle()
        assertThat(window.isMinimized).isTrue()

        state.isMinimized = false
        awaitIdle()
        assertThat(window.isMinimized).isFalse()
    }

    @Test
    fun `maximize and minimize `() = runApplicationTest {
        // macOS can't be maximized and minimized at the same time
        assumeTrue(!isMacOs)

        val state = WindowState(size = DpSize(200.dp, 200.dp))
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()

        state.isMinimized = true
        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window.isMinimized).isTrue()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
    }

    @Test
    fun `restore size and position after maximize`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000
    ) {
//        Swing/Linux has animations and sometimes adds an offset to the size/position
        assumeTrue(!isLinux)

        val state = WindowState(
            size = DpSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.size).isEqualTo(Dimension(201, 203))
        assertThat(window.location).isEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Maximized
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
        assertThat(window.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window.location).isNotEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertThat(window.size).isEqualTo(Dimension(201, 203))
        assertThat(window.location).isEqualTo(Point(196, 257))
    }

    @Test
    fun `restore size and position after fullscreen`() = runApplicationTest(
        useDelay = isMacOs || isLinux,
        delayMillis = 1000,
    ) {
        val state = WindowState(
            size = DpSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp)
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.size).isEqualTo(Dimension(201, 203))
        assertThat(window.location).isEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Fullscreen
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Fullscreen)
        assertThat(window.size).isNotEqualTo(Dimension(201, 203))
        assertThat(window.location).isNotEqualTo(Point(196, 257))

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertThat(window.size).isEqualTo(Dimension(201, 203))
        assertThat(window.location).isEqualTo(Point(196, 257))
    }

    @Test
    fun `window state size and position determine unmaximized state`() = runApplicationTest(
        useDelay = true,
        delayMillis = 1000
    ) {
        // On Linux the behaviour generally works, but this test fails because the size after
        // un-maximizing it not exactly as expected. Perhaps the window insets are not included
        // somewhere in AWT.
        // Specifically on our CI (which is also Linux), however, it fails because the initial
        // placement fails to be Maximized. The `maximize window before show` test fails the same
        // way.
        // Haven't actually tested on Windows; if you run it, and it doesn't pass, replace with
        // assumeTrue(isMacOs), or investigate/fix.
        assumeTrue(!isLinux)

        val state = WindowState(
            size = DpSize(201.dp, 203.dp),
            position = WindowPosition(196.dp, 257.dp),
            placement = WindowPlacement.Maximized
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)

        state.placement = WindowPlacement.Floating
        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Floating)
        assertThat(window.size).isEqualTo(Dimension(201, 203))
        assertThat(window.location).isEqualTo(Point(196, 257))
    }

    @Test
    fun `maximize window before show`() = runApplicationTest(useDelay = isLinux) {
        // This fails on our Linux CI; the window reports WindowPlacement.Floating.
        // But testing in an actual Ubuntu 22 system, it succeeds.
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Maximized,
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Maximized)
    }

    @Test
    fun `minimize window before show`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000
    ) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            isMinimized = true
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.isMinimized).isTrue()
    }

    @Test
    fun `enter fullscreen before show`() = runApplicationTest(
        useDelay = isMacOs,
        delayMillis = 1000,
    ) {
        val state = WindowState(
            size = DpSize(200.dp, 200.dp),
            position = WindowPosition(Alignment.Center),
            placement = WindowPlacement.Fullscreen,
        )
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}, state) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.placement).isEqualTo(WindowPlacement.Fullscreen)
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

        var index by mutableStateOf(0)
        val states = mutableListOf<WindowState>()

        launchTestApplication {
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

            Window(onCloseRequest = {}) {}
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
    }

    @Test
    fun `set window height by its content`() = runApplicationTest(useDelay = isLinux) {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(300.dp, Dp.Unspecified))

        launchTestApplication {
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
    }

    @Test
    fun `set window width by its content`() = runApplicationTest() {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(Dp.Unspecified, 300.dp))

        launchTestApplication {
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
    }

    @Test
    fun `set window size by its content`() = runApplicationTest {
        assumeTrue(!isLinux) // Flaky on our CI

        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize.Unspecified)

        launchTestApplication {
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
    }

    @Test
    fun `set window size by its content when window is on the screen`() = runApplicationTest(
        useDelay = isLinux || isMacOs
    ) {
        lateinit var window: ComposeWindow
        val state = WindowState(size = DpSize(100.dp, 100.dp))

        launchTestApplication {
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
    }

    @Test
    fun `change visible`() = runApplicationTest {
        lateinit var window: ComposeWindow

        var visible by mutableStateOf(false)

        launchTestApplication {
            Window(onCloseRequest = ::exitApplication, visible = visible) {
                window = this.window
            }
        }

        awaitIdle()
        assertThat(window.isVisible).isEqualTo(false)

        visible = true
        awaitIdle()
        assertThat(window.isVisible).isEqualTo(true)
    }

    @Test
    fun `invisible window should be active`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
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
    }

    @Test
    fun `WindowInfo isFocused`() = runApplicationTest {
        lateinit var window1: ComposeWindow
        lateinit var window2: ComposeWindow
        lateinit var window1Info: WindowInfo
        lateinit var window2Info: WindowInfo

        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                window1 = window
                window1Info = LocalWindowInfo.current
            }

            Window(onCloseRequest = ::exitApplication) {
                window2 = window
                window2Info = LocalWindowInfo.current
            }
        }

        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)

        window1.requestFocus()
        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)

        window2.requestFocus()
        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)
    }

    @Test
    fun `start invisible undecorated window`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
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
    }

    private val Window.contentSize
        get() = Dimension(
            size.width - insets.left - insets.right,
            size.height - insets.top - insets.bottom,
        )
}
