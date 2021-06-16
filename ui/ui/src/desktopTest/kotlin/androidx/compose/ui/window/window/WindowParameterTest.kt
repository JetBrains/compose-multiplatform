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

import androidx.compose.desktop.ComposeWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage

@OptIn(ExperimentalComposeUiApi::class)
class WindowParameterTest {
    @Test
    fun `change title`() = runApplicationTest {
        var window: ComposeWindow? = null

        var title by mutableStateOf("Title1")

        launchApplication {
            Window(onCloseRequest = ::exitApplication, title = title) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.title).isEqualTo("Title1")

        title = "Title2"
        awaitIdle()
        assertThat(window?.title).isEqualTo("Title2")

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `change icon`() = runApplicationTest {
        var window: ComposeWindow? = null

        val redIcon = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).apply {
            val graphics = createGraphics()
            graphics.background = java.awt.Color.RED
            graphics.clearRect(0, 0, 100, 100)
        }

        val blueIcon = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).apply {
            val graphics = createGraphics()
            graphics.background = java.awt.Color.BLUE
            graphics.clearRect(0, 0, 100, 100)
        }

        var icon by mutableStateOf(redIcon)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, icon = icon) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.iconImage).isEqualTo(redIcon)

        icon = blueIcon
        awaitIdle()
        assertThat(window?.iconImage).isEqualTo(blueIcon)

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    // Swing doesn't support changing isUndecorated
    @Test
    fun `set undecorated`() = runApplicationTest {
        var window: ComposeWindow? = null

        launchApplication {
            Window(onCloseRequest = ::exitApplication, undecorated = false) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isUndecorated).isFalse()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `change undecorated`() = runApplicationTest {
        var window: ComposeWindow? = null

        var resizable by mutableStateOf(false)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, resizable = resizable) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isResizable).isFalse()

        resizable = true
        awaitIdle()
        assertThat(window?.isResizable).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `change enabled`() = runApplicationTest {
        var window: ComposeWindow? = null

        var enabled by mutableStateOf(false)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, enabled = enabled) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isEnabled).isFalse()

        enabled = true
        awaitIdle()
        assertThat(window?.isEnabled).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `change focusable`() = runApplicationTest {
        var window: ComposeWindow? = null

        var focusable by mutableStateOf(false)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, focusable = focusable) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isFocusable).isFalse()

        focusable = true
        awaitIdle()
        assertThat(window?.isFocusable).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }

    @Test
    fun `change alwaysOnTop`() = runApplicationTest {
        var window: ComposeWindow? = null

        var alwaysOnTop by mutableStateOf(false)

        launchApplication {
            Window(onCloseRequest = ::exitApplication, alwaysOnTop = alwaysOnTop) {
                window = this.window
                Box(Modifier.size(32.dp).background(Color.Red))
            }
        }

        awaitIdle()
        assertThat(window?.isAlwaysOnTop).isFalse()

        alwaysOnTop = true
        awaitIdle()
        assertThat(window?.isAlwaysOnTop).isTrue()

        window?.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }
}