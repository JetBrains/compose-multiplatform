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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class ContainerTest : LayoutTest() {
    @Test
    fun testContainer_wrapsChild() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<IntSize>()
        show {
            Box {
                Container(
                    Modifier.onGloballyPositioned { coordinates ->
                        containerSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    EmptyBox(width = sizeDp, height = sizeDp)
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_appliesPaddingToChild() = with(density) {
        val paddingDp = 20.dp
        val padding = paddingDp.toIntPx()
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box {
                Container(
                    padding = PaddingValues(paddingDp),
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        containerSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    EmptyBox(
                        width = sizeDp, height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childPosition.value = coordinates.positionInRoot
                            positionedLatch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val totalPadding = paddingDp.toIntPx() * 2
        assertEquals(
            IntSize(size + totalPadding, size + totalPadding),
            containerSize.value
        )
        assertEquals(Offset(padding.toFloat(), padding.toFloat()), childPosition.value)
    }

    @Test
    fun testContainer_passesConstraintsToChild() = with(density) {
        val sizeDp = 100.dp
        val childWidthDp = 20.dp
        val childWidth = childWidthDp.toIntPx()
        val childHeightDp = 30.dp
        val childHeight = childHeightDp.toIntPx()
        val childConstraints = DpConstraints.fixed(childWidthDp, childHeightDp)

        val positionedLatch = CountDownLatch(4)
        val containerSize = Ref<IntSize>()
        val childSize = Array(3) { IntSize(0, 0) }
        show {
            Box {
                Row(
                    Modifier.onGloballyPositioned { coordinates ->
                        containerSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    Container(width = childWidthDp, height = childHeightDp) {
                        EmptyBox(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childSize[0] = coordinates.size
                                positionedLatch.countDown()
                            }
                        )
                    }
                    Container(constraints = childConstraints) {
                        EmptyBox(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childSize[1] = coordinates.size
                                positionedLatch.countDown()
                            }
                        )
                    }
                    Container(
                        constraints = (childConstraints),
                        // These should have priority.
                        width = (childWidthDp * 2),
                        height = (childHeightDp * 2)
                    ) {
                        EmptyBox(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childSize[2] = coordinates.size
                                positionedLatch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(childWidth, childHeight), childSize[0])
        assertEquals(IntSize(childWidth, childHeight), childSize[1])
        assertEquals(
            IntSize((childWidthDp * 2).toIntPx(), (childHeightDp * 2).toIntPx()),
            childSize[2]
        )
    }

    @Test
    fun testContainer_fillsAvailableSpace_whenSizeIsMax() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val alignSize = Ref<IntSize>()
        val containerSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Container(
                alignment = Alignment.TopStart,
                modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                    alignSize.value = coordinates.size
                    positionedLatch.countDown()
                }
            ) {
                Container(
                    expanded = true,
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        containerSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    EmptyBox(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childSize.value = coordinates.size
                            childPosition.value = coordinates.positionInRoot
                            positionedLatch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(alignSize.value, containerSize.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(
            Offset(
                (containerSize.value!!.width.toFloat() / 2 - size.toFloat() / 2)
                    .roundToInt().toFloat(),
                (containerSize.value!!.height.toFloat() / 2 - size.toFloat() / 2)
                    .roundToInt().toFloat()
            ),
            childPosition.value
        )
    }

    @Test
    fun testContainer_respectsIncomingMinConstraints() = with(density) {
        // Start with an even number of Int to avoid rounding issues due to different DPI
        // I.e, if we fix Dp instead, it's possible that when we convert to Px, sizeDp can round
        // down but sizeDp * 2 can round up, causing a 1 pixel test error.
        val size = 200
        val sizeDp = size.toDp()

        val positionedLatch = CountDownLatch(2)
        val containerSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box {
                val constraints = DpConstraints(minWidth = sizeDp * 2, minHeight = sizeDp * 2)
                ConstrainedBox(
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        containerSize.value = coordinates.size
                        positionedLatch.countDown()
                    },
                    constraints = constraints
                ) {
                    Container(alignment = Alignment.BottomEnd) {
                        EmptyBox(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childSize.value = coordinates.size
                                childPosition.value =
                                    coordinates.positionInRoot
                                positionedLatch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            IntSize((sizeDp * 2).toIntPx(), (sizeDp * 2).toIntPx()),
            containerSize.value
        )
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(Offset(size.toFloat(), size.toFloat()), childPosition.value)
    }

    @Test
    fun testContainer_hasTheRightSize_withPaddingAndNoChildren() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val containerSize = Ref<IntSize>()
        val latch = CountDownLatch(1)
        show {
            Box {
                Container(
                    width = sizeDp, height = sizeDp, padding = PaddingValues(10.dp),
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        containerSize.value = coordinates.size
                        latch.countDown()
                    }
                ) {
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), containerSize.value)
    }

    @Test
    fun testContainer_correctlyAppliesNonSymmetricPadding() = with(density) {
        val childSizeDp = 50.toDp()
        val paddingLeft = 8.toDp()
        val paddingTop = 7.toDp()
        val paddingRight = 5.toDp()
        val paddingBottom = 10.toDp()
        val innerPadding = PaddingValues(
            start = paddingLeft,
            top = paddingTop,
            end = paddingRight,
            bottom = paddingBottom
        )
        val expectedSize = IntSize(
            childSizeDp.toIntPx() + paddingLeft.toIntPx() + paddingRight.toIntPx(),
            childSizeDp.toIntPx() + paddingTop.toIntPx() + paddingBottom.toIntPx()
        )

        var containerSize: IntSize? = null
        val latch = CountDownLatch(1)
        show {
            Box {
                Container(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        containerSize = coordinates.size
                        latch.countDown()
                    },
                    padding = innerPadding
                ) {
                    Spacer(Modifier.preferredSize(width = childSizeDp, height = childSizeDp))
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedSize, containerSize)
    }

    @Test
    fun testContainer_contentSmallerThanPaddingIsCentered() = with(density) {
        val containerSize = 50.toDp()
        val padding = 10.toDp()
        val childSize = 5.toDp()
        val innerPadding = PaddingValues(padding)

        var childCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        show {
            Box {
                Container(width = containerSize, height = containerSize, padding = innerPadding) {
                    Spacer(
                        Modifier
                            .preferredSize(width = childSize, height = childSize)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childCoordinates = coordinates
                                latch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val centeringOffset = padding.toIntPx() +
            (
                (
                    containerSize.toIntPx() - padding.toIntPx() * 2 -
                        childSize.toIntPx()
                    ) / 2f
                ).roundToInt()
        val childPosition = childCoordinates!!.parentCoordinates!!.childToLocal(
            childCoordinates!!,
            Offset.Zero
        )
        assertEquals(
            Offset(centeringOffset.toFloat(), centeringOffset.toFloat()),
            childPosition
        )
        assertEquals(IntSize(childSize.toIntPx(), childSize.toIntPx()), childCoordinates!!.size)
    }

    @Test
    fun testContainer_childAffectsContainerSize() {
        var layoutLatch = CountDownLatch(2)
        val size = mutableStateOf(10.dp)
        var measure = 0
        var layout = 0
        show {
            Box {
                Layout(
                    children = {
                        Container {
                            EmptyBox(
                                width = size.value,
                                height = 10.dp,
                                modifier = Modifier.onGloballyPositioned {
                                    layoutLatch.countDown()
                                }
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(2)
        activityTestRule.runOnUiThread { size.value = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, measure)
        assertEquals(2, layout)
    }

    @Test
    fun testContainer_childDoesNotAffectContainerSize_whenSizeIsMax() {
        var layoutLatch = CountDownLatch(2)
        val size = mutableStateOf(10.dp)
        var measure = 0
        var layout = 0
        show {
            Box {
                Layout(
                    children = {
                        Container(expanded = true) {
                            EmptyBox(
                                width = size.value,
                                height = 10.dp,
                                modifier = Modifier.onGloballyPositioned {
                                    layoutLatch.countDown()
                                }
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { size.value = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
    }

    @Test
    fun testContainer_childDoesNotAffectContainerSize_whenFixedWidthAndHeight() {
        var layoutLatch = CountDownLatch(2)
        val size = mutableStateOf(10.dp)
        var measure = 0
        var layout = 0
        show {
            Box {
                Layout(
                    children = {
                        Container(width = 20.dp, height = 20.dp) {
                            EmptyBox(
                                width = size.value,
                                height = 10.dp,
                                modifier = Modifier.onGloballyPositioned {
                                    layoutLatch.countDown()
                                }
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { size.value = 20.dp }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
    }

    @Composable
    fun EmptyBox(width: Dp, height: Dp, modifier: Modifier = Modifier) {
        Layout(modifier = modifier, children = { }) { _, constraints ->
            layout(
                constraints.constrainWidth(width.toIntPx()),
                constraints.constrainHeight(height.toIntPx())
            ) {}
        }
    }
}
