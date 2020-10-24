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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class BoxTest : LayoutTest() {
    @Test
    fun testBox() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntSize>()
        val alignedChildSize = Ref<IntSize>()
        val alignedChildPosition = Ref<Offset>()
        val positionedChildSize = Ref<IntSize>()
        val positionedChildPosition = Ref<Offset>()
        show {
            Container(alignment = Alignment.TopStart) {
                Box {
                    Container(
                        Modifier.align(Alignment.BottomEnd)
                            .saveLayoutInfo(alignedChildSize, alignedChildPosition, positionedLatch)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                stackSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }

                    Container(
                        Modifier.matchParentSize()
                            .padding(10.toDp())
                            .saveLayoutInfo(
                                positionedChildSize,
                                positionedChildPosition,
                                positionedLatch
                            )
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), stackSize.value)
        assertEquals(IntSize(size, size), alignedChildSize.value)
        assertEquals(Offset(0f, 0f), alignedChildPosition.value)
        assertEquals(IntSize(30, 30), positionedChildSize.value)
        assertEquals(Offset(10f, 10f), positionedChildPosition.value)
    }

    @Test
    fun testBox_withMultipleAlignedChildren() = with(density) {
        val size = 200
        val sizeDp = size.toDp()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = (sizeDp * 2).toIntPx()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntSize>()
        val childSize = arrayOf(Ref<IntSize>(), Ref<IntSize>())
        val childPosition = arrayOf(Ref<Offset>(), Ref<Offset>())
        show {
            Container(alignment = Alignment.TopStart) {
                Box(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        stackSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    Container(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .saveLayoutInfo(
                                childSize[0],
                                childPosition[0],
                                positionedLatch
                            ),
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }
                    Container(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .saveLayoutInfo(
                                size = childSize[1],
                                position = childPosition[1],
                                positionedLatch = positionedLatch
                            ),
                        width = doubleSizeDp,
                        height = doubleSizeDp
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(doubleSize, doubleSize), stackSize.value)
        assertEquals(IntSize(size, size), childSize[0].value)
        assertEquals(
            Offset(size.toFloat(), size.toFloat()),
            childPosition[0].value
        )
        assertEquals(IntSize(doubleSize, doubleSize), childSize[1].value)
        assertEquals(Offset(0f, 0f), childPosition[1].value)
    }

    @Test
    fun testBox_withStretchChildren() = with(density) {
        val size = 250
        val sizeDp = size.toDp()
        val halfSizeDp = sizeDp / 2
        val inset = 50
        val insetDp = inset.toDp()

        val positionedLatch = CountDownLatch(6)
        val stackSize = Ref<IntSize>()
        val childSize = Array(5) { Ref<IntSize>() }
        val childPosition = Array(5) { Ref<Offset>() }
        show {
            Container(alignment = Alignment.TopStart) {
                Box(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        stackSize.value = coordinates.size
                        positionedLatch.countDown()
                    }
                ) {
                    Container(
                        Modifier.align(Alignment.Center)
                            .saveLayoutInfo(
                                childSize[0],
                                childPosition[0],
                                positionedLatch
                            ),
                        width = sizeDp,
                        height = sizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(start = insetDp, top = insetDp)
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(end = insetDp, bottom = insetDp)
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(start = insetDp, end = insetDp)
                            .saveLayoutInfo(childSize[3], childPosition[3], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                    Container(
                        Modifier.matchParentSize()
                            .padding(top = insetDp, bottom = insetDp)
                            .saveLayoutInfo(childSize[4], childPosition[4], positionedLatch),
                        width = halfSizeDp,
                        height = halfSizeDp
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), stackSize.value)
        assertEquals(IntSize(size, size), childSize[0].value)
        assertEquals(Offset(0f, 0f), childPosition[0].value)
        assertEquals(IntSize(size - inset, size - inset), childSize[1].value)
        assertEquals(Offset(inset.toFloat(), inset.toFloat()), childPosition[1].value)
        assertEquals(IntSize(size - inset, size - inset), childSize[2].value)
        assertEquals(Offset(0f, 0f), childPosition[2].value)
        assertEquals(IntSize(size - inset * 2, size), childSize[3].value)
        assertEquals(Offset(inset.toFloat(), 0f), childPosition[3].value)
        assertEquals(IntSize(size, size - inset * 2), childSize[4].value)
        assertEquals(Offset(0f, inset.toFloat()), childPosition[4].value)
    }

    @Test
    fun testBox_Rtl() = with(density) {
        val sizeDp = 48.toDp()
        val size = sizeDp.toIntPx()
        val tripleSizeDp = sizeDp * 3
        val tripleSize = (sizeDp * 3).toIntPx()

        val positionedLatch = CountDownLatch(10)
        val stackSize = Ref<IntSize>()
        val childSize = Array(9) { Ref<IntSize>() }
        val childPosition = Array(9) { Ref<Offset>() }
        show {
            Box(Modifier.wrapContentSize(Alignment.TopStart)) {
                Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                    Box(
                        Modifier
                            .preferredSize(tripleSizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                stackSize.value = coordinates.size
                                positionedLatch.countDown()
                            }
                    ) {
                        Box(
                            Modifier.align(Alignment.TopStart)
                                .preferredSize(sizeDp, sizeDp)
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.TopCenter)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.TopEnd)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.CenterStart)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[3], childPosition[3], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.Center)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[4], childPosition[4], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.CenterEnd)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[5], childPosition[5], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomStart)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[6], childPosition[6], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[7], childPosition[7], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd)
                                .preferredSize(sizeDp)
                                .saveLayoutInfo(childSize[8], childPosition[8], positionedLatch)
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(tripleSize, tripleSize), stackSize.value)
        assertEquals(Offset((size * 2).toFloat(), 0f), childPosition[0].value)
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1].value)
        assertEquals(Offset(0f, 0f), childPosition[2].value)
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                size.toFloat()
            ),
            childPosition[3].value
        )
        assertEquals(Offset(size.toFloat(), size.toFloat()), childPosition[4].value)
        assertEquals(Offset(0f, size.toFloat()), childPosition[5].value)
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                (size * 2).toFloat()
            ),
            childPosition[6].value
        )
        assertEquals(
            Offset(size.toFloat(), (size * 2).toFloat()),
            childPosition[7].value
        )
        assertEquals(Offset(0f, (size * 2).toFloat()), childPosition[8].value)
    }

    @Test
    fun testBox_expanded() = with(density) {
        val size = 250
        val sizeDp = size.toDp()
        val halfSize = 125
        val halfSizeDp = halfSize.toDp()

        val positionedLatch = CountDownLatch(3)
        val stackSize = Ref<IntSize>()
        val childSize = Array(2) { Ref<IntSize>() }
        val childPosition = Array(2) { Ref<Offset>() }
        show {
            Container(alignment = Alignment.TopStart) {
                Container(
                    Modifier.preferredSize(
                        sizeDp,
                        sizeDp
                    ).then(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            stackSize.value = coordinates.size
                            positionedLatch.countDown()
                        }
                    )
                ) {
                    Box {
                        Container(
                            Modifier.fillMaxSize()
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                        Container(
                            Modifier.align(Alignment.BottomEnd)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch),
                            width = halfSizeDp,
                            height = halfSizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), stackSize.value)
        assertEquals(IntSize(size, size), childSize[0].value)
        assertEquals(Offset(0f, 0f), childPosition[0].value)
        assertEquals(IntSize(halfSize, halfSize), childSize[1].value)
        assertEquals(
            Offset(
                (size - halfSize).toFloat(),
                (size - halfSize).toFloat()
            ),
            childPosition[1].value
        )
    }

    @Test
    fun testBox_alignmentParameter() = with(density) {
        val outerSizePx = 50f
        val outerSize = outerSizePx.toDp()
        val innerSizePx = 10f
        val innerSize = innerSizePx.toDp()

        val positionedLatch = CountDownLatch(1)
        show {
            Box(
                alignment = Alignment.BottomEnd,
                modifier = Modifier.size(outerSize)
            ) {
                Box(
                    Modifier.size(innerSize).onGloballyPositioned {
                        assertEquals(outerSizePx - innerSizePx, it.positionInParent.x)
                        assertEquals(outerSizePx - innerSizePx, it.positionInParent.y)
                        positionedLatch.countDown()
                    }
                ) {}
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testBox_outermostGravityWins() = with(density) {
        val positionedLatch = CountDownLatch(1)
        val size = 10f
        val sizeDp = size.toDp()
        show {
            Box(Modifier.size(sizeDp)) {
                Box(
                    Modifier.align(Alignment.BottomEnd).align(Alignment.TopStart)
                        .onGloballyPositioned {
                            assertEquals(size, it.positionInParent.x)
                            assertEquals(size, it.positionInParent.y)
                            positionedLatch.countDown()
                        }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testBox_hasCorrectIntrinsicMeasurements() = with(density) {
        val testWidth = 90.toDp()
        val testHeight = 80.toDp()

        val testDimension = 200
        // When measuring the height with testDimension, width should be double
        val expectedWidth = testDimension * 2
        // When measuring the width with testDimension, height should be half
        val expectedHeight = testDimension / 2

        testIntrinsics(
            @Composable {
                Box {
                    Container(Modifier.align(Alignment.TopStart).aspectRatio(2f)) { }
                    ConstrainedBox(
                        DpConstraints.fixed(testWidth, testHeight),
                        Modifier.align(Alignment.BottomCenter)
                    ) { }
                    ConstrainedBox(
                        DpConstraints.fixed(200.dp, 200.dp),
                        Modifier.matchParentSize().padding(10.dp)
                    ) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(testWidth.toIntPx(), minIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(expectedWidth, minIntrinsicWidth(testDimension))
            assertEquals(testWidth.toIntPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(testHeight.toIntPx(), minIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(expectedHeight, minIntrinsicHeight(testDimension))
            assertEquals(testHeight.toIntPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(testWidth.toIntPx(), maxIntrinsicWidth(0.dp.toIntPx()))
            assertEquals(expectedWidth, maxIntrinsicWidth(testDimension))
            assertEquals(testWidth.toIntPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(testHeight.toIntPx(), maxIntrinsicHeight(0.dp.toIntPx()))
            assertEquals(expectedHeight, maxIntrinsicHeight(testDimension))
            assertEquals(testHeight.toIntPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testBox_hasCorrectIntrinsicMeasurements_withNoAlignedChildren() = with(density) {
        testIntrinsics(
            @Composable {
                Box {
                    ConstrainedBox(
                        modifier = Modifier.matchParentSize().padding(10.dp),
                        constraints = DpConstraints.fixed(200.dp, 200.dp)
                    ) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(50.dp.toIntPx()))
            assertEquals(0.dp.toIntPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }
}
