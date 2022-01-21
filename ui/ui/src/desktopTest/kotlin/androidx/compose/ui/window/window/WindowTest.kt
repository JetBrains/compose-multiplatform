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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LeakDetector
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Assume.assumeFalse
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class WindowTest {
    @Test
    fun `open and close custom window`() = runApplicationTest {
        var window: ComposeWindow? = null

        launchApplication {
            var isOpen by remember { mutableStateOf(true) }

            fun createWindow() = ComposeWindow().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                Window(
                    create = ::createWindow,
                    dispose = ComposeWindow::dispose
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `update custom window`() = runApplicationTest {
        var window: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var title by mutableStateOf("Title1")

        launchApplication {
            fun createWindow() = ComposeWindow().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                Window(
                    create = ::createWindow,
                    dispose = ComposeWindow::dispose,
                    update = { it.title = title }
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()
        assertThat(window?.title).isEqualTo(title)

        title = "Title2"
        awaitIdle()
        assertThat(window?.title).isEqualTo(title)

        isOpen = false
    }

    @Test
    fun `open and close window`() = runApplicationTest {
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = ::exitApplication) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `disable closing window`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isCloseCalled by mutableStateOf(false)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(
                    onCloseRequest = {
                        isCloseCalled = true
                    }
                ) {
                    window = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        awaitIdle()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(isCloseCalled).isTrue()
        assertThat(window?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window?.isShowing).isFalse()
    }

    @Test
    fun `show splash screen`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var isLoading by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                if (isLoading) {
                    Window(onCloseRequest = {}) {
                        window1 = this.window
                        Box(Modifier.size(32.dp).background(Color.Red))
                    }
                } else {
                    Window(onCloseRequest = {}) {
                        window2 = this.window
                        Box(Modifier.size(32.dp).background(Color.Blue))
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2).isNull()

        isLoading = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `open two windows`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }

                Window(onCloseRequest = {}) {
                    window2 = this.window
                    Box(Modifier.size(32.dp).background(Color.Blue))
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `open nested window`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen by mutableStateOf(true)
        var isNestedOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))

                    if (isNestedOpen) {
                        Window(
                            onCloseRequest = {},
                            state = rememberWindowState(
                                size = DpSize(300.dp, 300.dp),
                            )
                        ) {
                            window2 = this.window
                            Box(Modifier.size(32.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isNestedOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isFalse()

        isNestedOpen = true
        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @Test
    fun `pass composition local to windows`() = runApplicationTest {
        var actualValue1: Int? = null
        var actualValue2: Int? = null
        var actualValue3: Int? = null

        var isOpen by mutableStateOf(true)
        val local1TestValue = compositionLocalOf { 0 }
        val local2TestValue = compositionLocalOf { 0 }
        var locals by mutableStateOf(arrayOf(local1TestValue provides 1))

        launchApplication {
            if (isOpen) {
                CompositionLocalProvider(*locals) {
                    Window(
                        onCloseRequest = {},
                        state = rememberWindowState(
                            size = DpSize(600.dp, 600.dp),
                        )
                    ) {
                        actualValue1 = local1TestValue.current
                        actualValue2 = local2TestValue.current
                        Box(Modifier.size(32.dp).background(Color.Red))

                        Window(
                            onCloseRequest = {},
                            state = rememberWindowState(
                                size = DpSize(300.dp, 300.dp),
                            )
                        ) {
                            actualValue3 = local1TestValue.current
                            Box(Modifier.size(32.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(actualValue1).isEqualTo(1)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(1)

        locals = arrayOf(local1TestValue provides 42)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(42)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(42)

        locals = arrayOf(local1TestValue provides 43)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(43)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(43)

        locals = arrayOf(local1TestValue provides 43, local2TestValue provides 12)
        awaitIdle()
        assertThat(actualValue1).isEqualTo(43)
        assertThat(actualValue2).isEqualTo(12)
        assertThat(actualValue3).isEqualTo(43)

        locals = emptyArray()
        awaitIdle()
        assertThat(actualValue1).isEqualTo(0)
        assertThat(actualValue2).isEqualTo(0)
        assertThat(actualValue3).isEqualTo(0)

        isOpen = false
    }

    @Test
    fun `DisposableEffect call order`() = runApplicationTest {
        var initCount = 0
        var disposeCount = 0

        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    DisposableEffect(Unit) {
                        initCount++
                        onDispose {
                            disposeCount++
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(initCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        isOpen = false
        awaitIdle()
        assertThat(initCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(1)
    }

    @Test(timeout = 30000)
    fun `window dispose should not cause a memory leak`() {
        assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val leakDetector = LeakDetector()

        val oldRecomposers = Recomposer.runningRecomposers.value

        runBlocking(Dispatchers.Swing) {
            repeat(10) {
                val window = ComposeWindow()
                window.size = Dimension(200, 200)
                window.isVisible = true
                window.setContent {
                    Button({}) {}
                    Slider(0f, {})
                }
                window.dispose()
                leakDetector.observeObject(window)
            }

            while (Recomposer.runningRecomposers.value != oldRecomposers) {
                delay(100)
            }

            assertThat(leakDetector.noLeak()).isTrue()
        }
    }

    @Test(timeout = 30000)
    fun `should draw before window is visible`() = runApplicationTest {
        var isComposed = false
        var isDrawn = false
        var isVisibleOnFirstComposition = false
        var isVisibleOnFirstDraw = false

        launchApplication {
            Window(onCloseRequest = ::exitApplication) {
                if (!isComposed) {
                    isVisibleOnFirstComposition = window.isVisible
                    isComposed = true
                }

                Canvas(Modifier.fillMaxSize()) {
                    if (!isDrawn) {
                        isVisibleOnFirstDraw = window.isVisible
                        isDrawn = true
                    }
                }
            }
        }

        awaitIdle()
        assertThat(isVisibleOnFirstComposition).isFalse()
        assertThat(isVisibleOnFirstDraw).isFalse()

        exitApplication()
    }

    @Test
    fun `LaunchedEffect should end before application exit`() = runApplicationTest {
        var isApplicationEffectEnded = false
        var isWindowEffectEnded = false

        val job = launchApplication {
            if (isOpen) {
                Window(onCloseRequest = ::exitApplication) {
                    LaunchedEffect(Unit) {
                        try {
                            delay(1000000)
                        } finally {
                            isWindowEffectEnded = true
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                try {
                    delay(1000000)
                } finally {
                    isApplicationEffectEnded = true
                }
            }
        }

        awaitIdle()
        exitApplication()
        job.cancelAndJoin()

        assertThat(isApplicationEffectEnded).isTrue()
        assertThat(isWindowEffectEnded).isTrue()
    }
}