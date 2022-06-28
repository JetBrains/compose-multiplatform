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

package androidx.compose.ui.benchmark.input.pointer

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for precise stylus tapping (down, move, and up) on an item in Compose created from a
 * real device.
 *
 * The intent is to measure the speed of all parts necessary for a normal stylus tap and move
 * starting from [MotionEvent]s getting dispatched to a particular view.  The test therefore
 * includes hit testing and dispatch.
 *
 * This is intended to be a more through benchmark of [ComposeTapIntegrationBenchmark] and a stylus
 * version of [ComposePreciseFingerTapIntegrationBenchmark].
 *
 * The hierarchy is set up to look like:
 * rootView
 *   -> Column
 *     -> Text (with click listener)
 *     -> Text (with click listener)
 *     -> Text (with click listener)
 *     -> ...
 *
 * MotionEvents are dispatched to rootView as an ACTION_DOWN, an ACTION_MOVE, and finally
 * an ACTION_UP.  The validity of the test is verified inside the click listener with
 * com.google.common.truth.Truth.assertThat and by counting the clicks in the click listener and
 * later verifying that they count is sufficiently high.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposePreciseStylusTapIntegrationBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun clickOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(0, "0")
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @Test
    fun clickOnEarlyItemFyi() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(lastItem, "$lastItem")
    }

    private fun clickOnItem(item: Int, expectedLabel: String) {
        val xDown = 0f
        // half height of an item + top of the chosen item = middle of the chosen item
        val yDown = (ItemHeightPx / 2) + (item * ItemHeightPx)

        val xMove = xDown + MOVE_AMOUNT_PX
        val yMove = yDown + MOVE_AMOUNT_PX

        val xUp = xMove + MOVE_AMOUNT_PX
        val yUp = yMove + MOVE_AMOUNT_PX

        benchmarkRule.runBenchmarkFor({ ComposeTapTestCase() }) {
            doFramesUntilNoChangesPending()

            val case = getTestCase()
            case.expectedLabel = expectedLabel

            val rootView = getHostView()

            // Precise Stylus MotionEvents (Down, Move, Up)
            // Based on real MotionEvents pulled from a device.
            val stylusDownMotionEvent = android.view.MotionEvent.obtain(
                346709L,
                346709L,
                android.view.MotionEvent.ACTION_DOWN,
                1,
                arrayOf(
                    PointerProperties(0).apply {
                        toolType = android.view.MotionEvent.TOOL_TYPE_STYLUS
                    }
                ),
                arrayOf(
                    PointerCoords(xDown, yDown).apply {
                        pressure = 0.18339439f
                        size = 0.0f
                    }
                ),
                0,
                0,
                1.000625f,
                1.0003906f,
                7,
                0x0, // Edge Flags value of 0.
                0x5002, // Source of the event value of 20482
                0x2 // Motion Event Flags value of 2
            )

            val stylusMoveMotionEvent = android.view.MotionEvent.obtain(
                346709L,
                347222L,
                android.view.MotionEvent.ACTION_MOVE,
                1,
                arrayOf(
                    PointerProperties(0).apply {
                        toolType = android.view.MotionEvent.TOOL_TYPE_STYLUS
                    }
                ),
                arrayOf(
                    PointerCoords(xMove, yMove).apply {
                        pressure = 0.2947497f
                        size = 0.0f
                    }
                ),
                0,
                0,
                1.000625f,
                1.0003906f,
                7,
                0x0, // Edge Flags value of 0.
                0x5002, // Source of the event value of 20482
                0x2 // Motion Event Flags value of 2
            )

            val stylusUpMotionEvent = android.view.MotionEvent.obtain(
                346709L,
                347227L,
                android.view.MotionEvent.ACTION_UP,
                1,
                arrayOf(
                    PointerProperties(0).apply {
                        toolType = android.view.MotionEvent.TOOL_TYPE_STYLUS
                    }
                ),
                arrayOf(
                    PointerCoords(xUp, yUp).apply {
                        pressure = 0.2947497f
                        size = 0.0f
                    }
                ),
                0,
                0,
                1.000625f,
                1.0003906f,
                7,
                0x0, // Edge Flags value of 0.
                0x5002, // Source of the event value of 20482
                0x2 // Motion Event Flags value of 2
            )

            benchmarkRule.measureRepeated {

                rootView.dispatchTouchEvent(stylusDownMotionEvent)
                rootView.dispatchTouchEvent(stylusMoveMotionEvent)
                rootView.dispatchTouchEvent(stylusUpMotionEvent)

                case.expectedClickCount++
                assertThat(case.actualClickCount).isEqualTo(case.expectedClickCount)
            }
        }
    }

    private class ComposeTapTestCase : ComposeTestCase {
        private var itemHeightDp = 0.dp // Is set to correct value during composition.
        var actualClickCount = 0
        var expectedClickCount = 0
        lateinit var expectedLabel: String

        @Composable
        override fun Content() {
            with(LocalDensity.current) {
                itemHeightDp = ItemHeightPx.toDp()
            }

            EmailList(NumItems)
        }

        @Composable
        fun EmailList(count: Int) {
            Column {
                repeat(count) { i ->
                    Email("$i")
                }
            }
        }

        @Composable
        fun Email(label: String) {
            BasicText(
                text = label,
                modifier = Modifier
                    .pointerInput(label) {
                        coroutineScope {
                            awaitPointerEventScope {
                                while (coroutineContext.isActive) {
                                    val down = awaitFirstDown()
                                    down.consume()

                                    val upOrCancel: PointerInputChange? = waitForUpOrCancellation()
                                    upOrCancel?.consume()
                                    assertThat(label).isEqualTo(expectedLabel)
                                    actualClickCount++
                                }
                            }
                        }
                    }
                    .fillMaxWidth()
                    .requiredHeight(itemHeightDp)
            )
        }
    }
    companion object {
        private const val MOVE_AMOUNT_PX = 30f
    }
}
