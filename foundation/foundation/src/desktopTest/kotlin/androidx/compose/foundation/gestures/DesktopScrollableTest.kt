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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class DesktopScrollableTest {
    private val density = 2f

    private fun window(
        platform: DesktopPlatform
    ) = TestComposeWindow(
        width = 100,
        height = 100,
        density = Density(density),
        desktopPlatform = platform
    )

    private fun scrollLineLinux(bounds: Dp) = sqrt(bounds.value * density)
    private fun scrollLineWindows(bounds: Dp) = bounds.value * density / 20f
    private fun scrollLineMacOs() = density * 10f
    private fun scrollPage(bounds: Dp) = bounds.value * density

    @Test
    fun `linux, scroll vertical`() {
        val window = window(platform = DesktopPlatform.Linux)
        val context = TestColumn()

        window.setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(-3 * scrollLineLinux(20.dp))

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(-6 * scrollLineLinux(20.dp))
    }

    @Test
    fun `windows, scroll vertical`() {
        val window = window(platform = DesktopPlatform.Windows)
        val context = TestColumn()

        window.setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(-2f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(2 * scrollLineWindows(20.dp))

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(4f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(-2 * scrollLineWindows(20.dp))
    }

    @Test
    fun `windows, scroll one page vertical`() {
        val window = window(platform = DesktopPlatform.Windows)
        val context = TestColumn()

        window.setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Page(1f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(-scrollPage(20.dp))
    }

    @Test
    fun `macOS, scroll vertical`() {
        val window = window(platform = DesktopPlatform.MacOS)
        val context = TestColumn()

        window.setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(-5.5f), Orientation.Vertical)
        )

        assertThat(context.offset).isWithin(0.1f).of(5.5f * scrollLineMacOs())
    }

    @Test
    fun `scroll with different orientation`() {
        val window = window(platform = DesktopPlatform.Linux)
        val column = TestColumn()

        window.setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = column.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), Orientation.Horizontal)
        )

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

    private class TestAnimationClock : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) = Unit
        override fun unsubscribe(observer: AnimationClockObserver) = Unit
    }
}