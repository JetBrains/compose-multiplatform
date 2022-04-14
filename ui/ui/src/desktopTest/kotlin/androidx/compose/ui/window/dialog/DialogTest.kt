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

package androidx.compose.ui.window.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@OptIn(ExperimentalComposeUiApi::class)
class DialogTest {
    @Test
    fun `open and close custom dialog`() = runApplicationTest {
        var window: ComposeDialog? = null

        launchApplication {
            var isOpen by remember { mutableStateOf(true) }

            fun createWindow() = ComposeDialog().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                Dialog(
                    create = ::createWindow,
                    dispose = ComposeDialog::dispose
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
    fun `update custom dialog`() = runApplicationTest {
        var window: ComposeDialog? = null

        var isOpen by mutableStateOf(true)
        var title by mutableStateOf("Title1")

        launchApplication {
            fun createWindow() = ComposeDialog().apply {
                size = Dimension(300, 200)

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        isOpen = false
                    }
                })
            }

            if (isOpen) {
                Dialog(
                    create = ::createWindow,
                    dispose = ComposeDialog::dispose,
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
    fun `open and close dialog`() = runApplicationTest {
        var window: ComposeDialog? = null

        launchApplication {
            Dialog(onCloseRequest = ::exitApplication) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isShowing).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `disable closing dialog`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isCloseCalled by mutableStateOf(false)
        var window: ComposeDialog? = null

        launchApplication {
            if (isOpen) {
                Dialog(
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
        var window1: ComposeDialog? = null
        var window2: ComposeDialog? = null

        var isOpen by mutableStateOf(true)
        var isLoading by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                if (isLoading) {
                    Dialog(onCloseRequest = {}) {
                        window1 = this.window
                        Box(Modifier.size(32.dp).background(Color.Red))
                    }
                } else {
                    Dialog(onCloseRequest = {}) {
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
    fun `open two dialogs`() = runApplicationTest {
        var window1: ComposeDialog? = null
        var window2: ComposeDialog? = null

        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Dialog(onCloseRequest = {}) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }

                Dialog(onCloseRequest = {}) {
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
    fun `open nested dialog`() = runApplicationTest {
        var window1: ComposeDialog? = null
        var window2: ComposeDialog? = null

        var isOpen by mutableStateOf(true)
        var isNestedOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Dialog(
                    onCloseRequest = {},
                    state = rememberDialogState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))

                    if (isNestedOpen) {
                        Dialog(
                            onCloseRequest = {},
                            state = rememberDialogState(
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
    fun `pass composition local to dialogs`() = runApplicationTest {
        var actualValue1: Int? = null
        var actualValue2: Int? = null

        var isOpen by mutableStateOf(true)
        var testValue by mutableStateOf(0)
        val localTestValue = compositionLocalOf { testValue }

        launchApplication {
            if (isOpen) {
                CompositionLocalProvider(localTestValue provides testValue) {
                    Dialog(
                        onCloseRequest = {},
                        state = rememberDialogState(
                            size = DpSize(600.dp, 600.dp),
                        )
                    ) {
                        actualValue1 = localTestValue.current
                        Box(Modifier.size(32.dp).background(Color.Red))

                        Dialog(
                            onCloseRequest = {},
                            state = rememberDialogState(
                                size = DpSize(300.dp, 300.dp),
                            )
                        ) {
                            actualValue2 = localTestValue.current
                            Box(Modifier.size(32.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(actualValue1).isEqualTo(0)
        assertThat(actualValue2).isEqualTo(0)

        testValue = 42
        awaitIdle()
        assertThat(actualValue1).isEqualTo(42)
        assertThat(actualValue2).isEqualTo(42)

        isOpen = false
    }

    @Test
    fun `DisposableEffect call order`() = runApplicationTest {
        var initCount = 0
        var disposeCount = 0

        var isOpen by mutableStateOf(true)

        launchApplication {
            if (isOpen) {
                Dialog(onCloseRequest = {}) {
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

    @Test
    fun `catch key handlers`() = runApplicationTest {
        var window: ComposeDialog? = null
        val onKeyEventKeys = mutableSetOf<Key>()
        val onPreviewKeyEventKeys = mutableSetOf<Key>()

        fun clear() {
            onKeyEventKeys.clear()
            onPreviewKeyEventKeys.clear()
        }

        launchApplication {
            Dialog(
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
        var window: ComposeDialog? = null
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
            Dialog(
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

    @Test(timeout = 30000)
    fun `should draw before dialog is visible`() = runApplicationTest {
        var isComposed = false
        var isDrawn = false
        var isVisibleOnFirstComposition = false
        var isVisibleOnFirstDraw = false

        launchApplication {
            Dialog(onCloseRequest = ::exitApplication) {
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
}