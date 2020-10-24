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
import androidx.compose.runtime.Providers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.enforce
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
class LayoutAlignTest : LayoutTest() {
    @Test
    fun test2DWrapContentSize() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Container(Modifier.saveLayoutInfo(alignSize, alignPosition, positionedLatch)) {
                Container(
                    Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.BottomEnd)
                        .preferredSize(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntSize(root.width, root.height), alignSize.value)
        assertEquals(Offset(0f, 0f), alignPosition.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(
            Offset(root.width - size.toFloat(), root.height - size.toFloat()),
            childPosition.value
        )
    }

    @Test
    fun test1DWrapContentSize() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Container(
                Modifier.saveLayoutInfo(
                    size = alignSize,
                    position = alignPosition,
                    positionedLatch = positionedLatch
                )
            ) {
                Container(
                    Modifier.fillMaxSize()
                        .wrapContentWidth(Alignment.End)
                        .preferredWidth(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntSize(root.width, root.height), alignSize.value)
        assertEquals(Offset(0f, 0f), alignPosition.value)
        assertEquals(IntSize(size, root.height), childSize.value)
        assertEquals(Offset(root.width - size.toFloat(), 0f), childPosition.value)
    }

    @Test
    fun testWrapContentSize_rtl() = with(density) {
        val sizeDp = 200.toDp()
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val childSize = Array(3) { Ref<IntSize>() }
        val childPosition = Array(3) { Ref<Offset>() }
        show {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                        Box(
                            Modifier.preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                    }
                    Box(Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)) {
                        Box(
                            Modifier.preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                        ) {
                        }
                    }
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                        Box(
                            Modifier.preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(
            Offset((root.width - size).toFloat(), 0f),
            childPosition[0].value
        )
        assertEquals(
            Offset(
                (root.width - size).toFloat(),
                ((root.height - size) / 2).toFloat()
            ),
            childPosition[1].value
        )
        assertEquals(
            Offset(0f, (root.height - size).toFloat()),
            childPosition[2].value
        )
    }

    @Test
    fun testModifier_wrapsContent() = with(density) {
        val contentSize = 50.dp
        val size = Ref<IntSize>()
        val latch = CountDownLatch(1)
        show {
            Container {
                Container(Modifier.saveLayoutInfo(size, Ref(), latch)) {
                    Container(
                        Modifier.wrapContentSize(Alignment.TopStart)
                            .preferredSize(contentSize)
                    ) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(contentSize.toIntPx(), contentSize.toIntPx()), size.value)
    }

    @Test
    fun testWrapContentSize_wrapsContent_whenMeasuredWithInfiniteConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Layout(
                children = {
                    Container(
                        Modifier.saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.wrapContentSize(Alignment.BottomEnd)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                },
                measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
            )
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntSize(size, size), alignSize.value)
        assertEquals(Offset(0f, 0f), alignPosition.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(Offset(0f, 0f), childPosition.value)
    }

    @Test
    fun testWrapContentSize_respectsMinConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Container(Modifier.wrapContentSize(Alignment.TopStart)) {
                Layout(
                    modifier = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        wrapSize.value = coordinates.size
                        positionedLatch.countDown()
                    },
                    children = {
                        Container(
                            Modifier.wrapContentSize(Alignment.Center)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    },
                    measureBlock = { measurables, incomingConstraints ->
                        val measurable = measurables.first()
                        val constraints = Constraints(
                            minWidth = doubleSizeDp.toIntPx(),
                            minHeight = doubleSizeDp.toIntPx()
                        ).enforce(incomingConstraints)
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(Offset.Zero)
                        }
                    }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(
            Offset(
                ((doubleSize - size) / 2f).roundToInt().toFloat(),
                ((doubleSize - size) / 2f).roundToInt().toFloat()
            ),
            childPosition.value
        )
    }

    @Test
    fun testWrapContentSize_unbounded() = with(density) {
        val outerSize = 10f
        val innerSize = 20f

        val positionedLatch = CountDownLatch(4)
        show {
            Box(
                Modifier.size(outerSize.toDp())
                    .onGloballyPositioned {
                        assertEquals(outerSize, it.size.width.toFloat())
                        positionedLatch.countDown()
                    }
            ) {
                Box(
                    Modifier.wrapContentSize(Alignment.BottomEnd, unbounded = true)
                        .size(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(
                                Offset(outerSize - innerSize, outerSize - innerSize),
                                it.positionInParent
                            )
                            positionedLatch.countDown()
                        }
                )
                Box(
                    Modifier.wrapContentWidth(Alignment.End, unbounded = true)
                        .size(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(outerSize - innerSize, it.positionInParent.x)
                            positionedLatch.countDown()
                        }
                )
                Box(
                    Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true)
                        .size(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(outerSize - innerSize, it.positionInParent.y)
                            positionedLatch.countDown()
                        }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    // TODO(popam): this should be unit test instead
    @Test
    fun testAlignmentCoordinates_evenSize() {
        val size = IntSize(2, 2)
        assertEquals(IntOffset(0, 0), Alignment.TopStart.align(size))
        assertEquals(IntOffset(1, 0), Alignment.TopCenter.align(size))
        assertEquals(IntOffset(2, 0), Alignment.TopEnd.align(size))
        assertEquals(IntOffset(0, 1), Alignment.CenterStart.align(size))
        assertEquals(IntOffset(1, 1), Alignment.Center.align(size))
        assertEquals(IntOffset(2, 1), Alignment.CenterEnd.align(size))
        assertEquals(IntOffset(0, 2), Alignment.BottomStart.align(size))
        assertEquals(IntOffset(1, 2), Alignment.BottomCenter.align(size))
        assertEquals(IntOffset(2, 2), Alignment.BottomEnd.align(size))
    }

    // TODO(popam): this should be unit test instead
    @Test
    fun testAlignmentCoordinates_oddSize() {
        val size = IntSize(3, 3)
        assertEquals(IntOffset(0, 0), Alignment.TopStart.align(size))
        assertEquals(IntOffset(2, 0), Alignment.TopCenter.align(size))
        assertEquals(IntOffset(3, 0), Alignment.TopEnd.align(size))
        assertEquals(IntOffset(0, 2), Alignment.CenterStart.align(size))
        assertEquals(IntOffset(2, 2), Alignment.Center.align(size))
        assertEquals(IntOffset(3, 2), Alignment.CenterEnd.align(size))
        assertEquals(IntOffset(0, 3), Alignment.BottomStart.align(size))
        assertEquals(IntOffset(2, 3), Alignment.BottomCenter.align(size))
        assertEquals(IntOffset(3, 3), Alignment.BottomEnd.align(size))
    }

    @Test
    fun test2DAlignedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.wrapContentSize(Alignment.TopStart).aspectRatio(2f)) { }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(Constraints.Infinity))

            // Min height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals((50.dp.toIntPx() / 2f).roundToInt(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(Constraints.Infinity))

            // Max width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(Constraints.Infinity))

            // Max height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals((50.dp.toIntPx() / 2f).roundToInt(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun test1DAlignedModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics({
            Container(
                Modifier.wrapContentHeight(Alignment.CenterVertically)
                    .aspectRatio(2f)
            ) { }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->

            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.toIntPx() * 2, minIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(Constraints.Infinity))

            // Min height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals((50.dp.toIntPx() / 2f).roundToInt(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(Constraints.Infinity))

            // Max width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.toIntPx() * 2, maxIntrinsicWidth(25.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(Constraints.Infinity))

            // Max height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals((50.dp.toIntPx() / 2f).roundToInt(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testAlignedModifier_alignsCorrectly_whenOddDimensions_endAligned() = with(density) {
        // Given a 100 x 100 pixel container, we want to make sure that when aligning a 1 x 1 pixel
        // child to both ends (bottom, and right) we correctly position children at the last
        // possible pixel, and avoid rounding issues. Previously we first centered the coordinates,
        // and then aligned after, so the maths would actually be (99 / 2) * 2, which incorrectly
        // ends up at 100 (Int rounds up) - so the last pixels in both directions just wouldn't
        // be visible.
        val parentSize = 100.toDp()
        val childSizeDp = 1.toDp()
        val childSizeIpx = childSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Layout(
                children = {
                    Container(
                        Modifier.preferredSize(parentSize)
                            .saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.fillMaxSize()
                                .wrapContentSize(Alignment.BottomEnd)
                                .preferredSize(childSizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                },
                measureBlock = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntSize(childSizeIpx, childSizeIpx), childSize.value)
        assertEquals(
            Offset(
                (alignSize.value!!.width - childSizeIpx).toFloat(),
                (alignSize.value!!.height - childSizeIpx).toFloat()
            ),
            childPosition.value
        )
    }
}
