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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind
import platform.AppKit.NSEvent
import platform.CoreGraphics.CGEventCreateScrollWheelEvent2
import platform.CoreGraphics.CGScrollEventUnit
import platform.CoreGraphics.kCGScrollEventUnitLine
import platform.CoreGraphics.kCGScrollEventUnitPixel

@ExperimentalTestApi
class MacosScrollableTest {
    private val density = Density(2f)
    private val scrollLine = density.density * 10f

    @Test
    fun `scroll by pixels vertically`() = runSkikoComposeUiTest(density = density) {
        val context = TestColumn()
        setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        sendScrollingEvent(deltaY = 10, unit = kCGScrollEventUnitPixel)

        waitForIdle()
        assertEquals(10F, context.offset, 0.1F)
    }

    @Test
    fun `scroll by lines vertically`() = runSkikoComposeUiTest(density = density) {
        val context = TestColumn()

        setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        sendScrollingEvent(deltaY = 3, unit = kCGScrollEventUnitLine)

        waitForIdle()
        assertEquals(3F * scrollLine, context.offset, 0.1F)
    }

    @Test
    fun `scroll by pixels horizontally`() = runSkikoComposeUiTest(density = density) {
        val context = TestColumn()

        setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Horizontal,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        sendScrollingEvent(deltaX = 5, unit = kCGScrollEventUnitPixel)

        waitForIdle()
        assertEquals(5F, context.offset, 0.1F)
    }

    @Test
    fun `scroll by lines horizontally`() = runSkikoComposeUiTest(density = density) {
        val context = TestColumn()

        setContent {
            Box(
                Modifier
                    .scrollable(
                        orientation = Orientation.Horizontal,
                        state = context.controller()
                    )
                    .size(10.dp, 20.dp)
            )
        }

        sendScrollingEvent(deltaX = 2, unit = kCGScrollEventUnitLine)

        waitForIdle()
        assertEquals(2F * scrollLine, context.offset, 0.1F)
    }

    private fun SkikoComposeUiTest.sendScrollingEvent(
        deltaX: Int = 0,
        deltaY: Int = 0,
        unit: CGScrollEventUnit,
    ) {
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset.Zero,
            scrollDelta = Offset(x = deltaX.toFloat(), y = deltaY.toFloat()),
            nativeEvent = scrollingEvent(deltaX = deltaX, deltaY = deltaY, unit = unit),
        )
    }

    private fun scrollingEvent(
        deltaX: Int,
        deltaY: Int,
        unit: CGScrollEventUnit,
    ) = SkikoPointerEvent(
            x = deltaX.toDouble(),
            y = deltaY.toDouble(),
            deltaX = deltaX.toDouble(),
            deltaY = deltaY.toDouble(),
            kind = SkikoPointerEventKind.SCROLL,
            platform = NSEvent.eventWithCGEvent(
                CGEventCreateScrollWheelEvent2(
                    source = null,
                    units = unit,
                    wheelCount = 2,
                    wheel1 = deltaY,
                    wheel2 = deltaX,
                    wheel3 = 0,
                )
            ),
        )

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
