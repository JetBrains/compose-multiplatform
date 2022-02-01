/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.foundation.gestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt

// TODO(demin): convert to ComposeScene instead of TestComposeWindow,
//  after that we won't need `window.render`
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
@Ignore // TODO(b/217238066) remove after migration to ImageComposeScene (it will be upstreamed from Compose MPP 1.0.0)
class DesktopScrollableTest {
    private val density = 2f

    private fun window() = TestComposeWindow(
        width = 100,
        height = 100,
        density = Density(density)
    )

    private fun scrollLineLinux(bounds: Dp) = sqrt(bounds.value * density)
    private fun scrollLineWindows(bounds: Dp) = bounds.value * density / 20f
    private fun scrollLineMacOs() = density * 10f
    private fun scrollPage(bounds: Dp) = bounds.value * density

    @Test
    fun `linux, scroll vertical`() {
        val window = window()
        val context = TestColumn()

        window.setContent {
            CompositionLocalProvider(
                LocalScrollConfig provides LinuxGnomeConfig
            ) {
                Box(
                    Modifier
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = context.controller()
                        )
                        .size(10.dp, 20.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(-3 * scrollLineLinux(20.dp))

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(-6 * scrollLineLinux(20.dp))
    }

    @Test
    fun `windows, scroll vertical`() {
        val window = window()
        val context = TestColumn()

        window.setContent {
            CompositionLocalProvider(
                LocalScrollConfig provides WindowsWinUIConfig
            ) {
                Box(
                    Modifier
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = context.controller()
                        )
                        .size(10.dp, 20.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(-2f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(2 * scrollLineWindows(20.dp))

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(4f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(-2 * scrollLineWindows(20.dp))
    }

    @Test
    fun `windows, scroll one page vertical`() {
        val window = window()
        val context = TestColumn()

        window.setContent {
            CompositionLocalProvider(
                LocalScrollConfig provides WindowsWinUIConfig
            ) {
                Box(
                    Modifier
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = context.controller()
                        )
                        .size(10.dp, 20.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Page(1f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(-scrollPage(20.dp))
    }

    @Test
    fun `macOS, scroll vertical`() {
        val window = window()
        val context = TestColumn()

        window.setContent {
            CompositionLocalProvider(
                LocalScrollConfig provides MacOSCocoaConfig
            ) {
                Box(
                    Modifier
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = context.controller()
                        )
                        .size(10.dp, 20.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(-5f), MouseScrollOrientation.Vertical)
        )
        window.render()

        assertThat(context.offset).isWithin(0.1f).of(5f * scrollLineMacOs())
    }

    @Test
    fun `scroll with different orientation`() {
        val window = window()
        val column = TestColumn()

        window.setContent {
            CompositionLocalProvider(
                LocalScrollConfig provides LinuxGnomeConfig
            ) {
                Box(
                    Modifier
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = column.controller()
                        )
                        .size(10.dp, 20.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Horizontal)
        )
        window.render()

        assertThat(column.offset).isEqualTo(0f)
    }

    private class TestColumn {
        var offset = 0f
            private set

        @Composable
        fun controller() = ScrollableState(::consumeScrollDelta)

        private fun consumeScrollDelta(delta: Float): Float {
            offset += delta
            return delta
        }
    }
}