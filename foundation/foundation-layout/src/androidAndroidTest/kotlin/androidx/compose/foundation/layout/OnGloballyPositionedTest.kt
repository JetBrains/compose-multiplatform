/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.foundation.layout

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.VerticalAlignmentLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnGloballyPositionedTest : LayoutTest() {

    @Test
    fun simplePadding() = with(density) {
        val paddingLeftPx = 100.0f
        val paddingTopPx = 120.0f
        var realLeft: Float? = null
        var realTop: Float? = null

        val positionedLatch = CountDownLatch(1)
        show {
            Container(
                Modifier.fillMaxSize()
                    .padding(start = paddingLeftPx.toDp(), top = paddingTopPx.toDp())
                    .onGloballyPositioned {
                        realLeft = it.positionInParent.x
                        realTop = it.positionInParent.y
                        positionedLatch.countDown()
                    }
            ) {
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(paddingLeftPx).isEqualTo(realLeft)
        assertThat(paddingTopPx).isEqualTo(realTop)
    }

    @Test
    fun nestedLayoutCoordinates() = with(density) {
        val firstPaddingPx = 10f
        val secondPaddingPx = 20f
        val thirdPaddingPx = 30f
        var gpCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null

        val positionedLatch = CountDownLatch(2)
        show {
            Container(
                Modifier.padding(start = firstPaddingPx.toDp()).then(
                    Modifier.onGloballyPositioned {
                        gpCoordinates = it
                        positionedLatch.countDown()
                    }
                )
            ) {
                Container(Modifier.padding(start = secondPaddingPx.toDp())) {
                    Container(
                        Modifier.fillMaxSize()
                            .padding(start = thirdPaddingPx.toDp())
                            .onGloballyPositioned {
                                childCoordinates = it
                                positionedLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        // global position
        val gPos = childCoordinates!!.localToGlobal(Offset.Zero).x
        assertThat(gPos).isEqualTo((firstPaddingPx + secondPaddingPx + thirdPaddingPx))
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.childToLocal(childCoordinates!!, Offset.Zero).x
        assertThat(gpPos).isEqualTo((secondPaddingPx + thirdPaddingPx))
        // local position
        assertThat(childCoordinates!!.positionInParent.x).isEqualTo(thirdPaddingPx)
    }

    @Test
    fun globalCoordinatesAreInActivityCoordinates() = with(density) {
        val padding = 30
        val localPosition = Offset.Zero
        val framePadding = Offset(padding.toFloat(), padding.toFloat())
        var realGlobalPosition: Offset? = null
        var realLocalPosition: Offset? = null
        var frameGlobalPosition: Offset? = null

        val positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                val frameLayout = FrameLayout(activity)
                frameLayout.setPadding(padding, padding, padding, padding)
                activity.setContentView(frameLayout)

                val position = IntArray(2)
                frameLayout.getLocationOnScreen(position)
                frameGlobalPosition = Offset(position[0].toFloat(), position[1].toFloat())

                frameLayout.setContent(Recomposer.current()) {
                    Container(
                        Modifier.onGloballyPositioned {
                            realGlobalPosition = it.localToGlobal(localPosition)
                            realLocalPosition = it.globalToLocal(
                                framePadding +
                                    frameGlobalPosition!!
                            )
                            positionedLatch.countDown()
                        },
                        expanded = true,
                        children = emptyContent()
                    )
                }
            }
        })
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(realGlobalPosition).isEqualTo(frameGlobalPosition!! + framePadding)
        assertThat(realLocalPosition).isEqualTo(localPosition)
    }

    @Test
    fun justAddedOnPositionedCallbackFiredWithoutLayoutChanges() = with(density) {
        val needCallback = mutableStateOf(false)

        val positionedLatch = CountDownLatch(1)
        show {
            val modifier = if (needCallback.value) {
                Modifier.onGloballyPositioned { positionedLatch.countDown() }
            } else {
                Modifier
            }
            Container(modifier, expanded = true) { }
        }

        activityTestRule.runOnUiThread { needCallback.value = true }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30.dp)
        var realLeft: Float? = null

        var positionedLatch = CountDownLatch(1)
        show {
            Box {
                Container(
                    Modifier.onGloballyPositioned {
                        realLeft = it.positionInParent.x
                        positionedLatch.countDown()
                    }
                        .fillMaxSize()
                        .padding(start = left.value),
                    children = emptyContent()
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { left.value = 40.dp }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        with(density) {
            assertThat(realLeft).isEqualTo(40.dp.toPx())
        }
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val left = mutableStateOf(20.dp)
        var realLeft: Float? = null
        var positionedLatch = CountDownLatch(1)
        show {
            Box {
                Offset(left) {
                    Container(width = 10.dp, height = 10.dp) {
                        Container(width = 10.dp, height = 10.dp) {
                            Container(
                                Modifier.onGloballyPositioned {
                                    realLeft = it.positionInRoot.x
                                    positionedLatch.countDown()
                                },
                                width = 10.dp,
                                height = 10.dp
                            ) {
                            }
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { left.value = 40.dp }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        with(density) {
            assertThat(realLeft).isEqualTo(40.dp.toPx())
        }
    }

    @Test
    fun testAlignmentLinesArePresent() {
        val latch = CountDownLatch(1)
        val line = VerticalAlignmentLine(::min)
        val lineValue = 10
        show {
            val onPositioned = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                Assert.assertEquals(1, coordinates.providedAlignmentLines.size)
                Assert.assertEquals(lineValue, coordinates[line])
                latch.countDown()
            }
            Layout(modifier = onPositioned, children = { }) { _, _ ->
                layout(0, 0, mapOf(line to lineValue)) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun Offset(sizeModel: State<Dp>, children: @Composable () -> Unit) {
        // simple copy of Padding which doesn't recompose when the size changes
        Layout(children) { measurables, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {
                measurables.first().measure(constraints)
                    .placeRelative(sizeModel.value.toPx().roundToInt(), 0)
            }
        }
    }
}
