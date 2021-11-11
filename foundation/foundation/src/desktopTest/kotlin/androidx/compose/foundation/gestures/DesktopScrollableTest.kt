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

import androidx.compose.foundation.awtWheelEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import kotlin.math.sqrt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class DesktopScrollableTest {
    private val density = 2f

    private fun scrollLineLinux(bounds: Dp) = sqrt(bounds.value * density)
    private fun scrollLineWindows(bounds: Dp) = bounds.value * density / 20f
    private fun scrollLineMacOs() = density * 10f
    private fun scrollPage(bounds: Dp) = bounds.value * density

    @Test
    fun `linux, scroll vertical`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(density)
    ).use { scene ->
        val context = TestColumn()

        scene.setContent {
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

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, 3f),
            nativeEvent = awtWheelEvent(),
        )

        assertThat(context.offset).isWithin(0.1f).of(-3 * scrollLineLinux(20.dp))

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, 3f),
            nativeEvent = awtWheelEvent(),
        )

        assertThat(context.offset).isWithin(0.1f).of(-6 * scrollLineLinux(20.dp))
    }

    @Test
    fun `windows, scroll vertical`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(density)
    ).use { scene ->
        val context = TestColumn()

        scene.setContent {
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

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, -2f),
            nativeEvent = awtWheelEvent(),
        )

        assertThat(context.offset).isWithin(0.1f).of(2 * scrollLineWindows(20.dp))

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, 4f),
            nativeEvent = awtWheelEvent(),
        )

        assertThat(context.offset).isWithin(0.1f).of(-2 * scrollLineWindows(20.dp))
    }

    @Test
    fun `windows, scroll one page vertical`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(density)
    ).use { scene ->
        val context = TestColumn()

        scene.setContent {
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

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, 1f),
            nativeEvent = awtWheelEvent(isScrollByPages = true),
        )

        assertThat(context.offset).isWithin(0.1f).of(-scrollPage(20.dp))
    }

    @Test
    fun `macOS, scroll vertical`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(density)
    ).use { scene ->
        val context = TestColumn()

        scene.setContent {
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

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(0f, -5.5f),
            nativeEvent = awtWheelEvent(),
        )

        assertThat(context.offset).isWithin(0.1f).of(5.5f * scrollLineMacOs())
    }

    @Test
    fun `scroll with different orientation`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(density)
    ).use { scene ->
        val column = TestColumn()

        scene.setContent {
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

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(3f, 0f),
            nativeEvent = awtWheelEvent(),
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
}
