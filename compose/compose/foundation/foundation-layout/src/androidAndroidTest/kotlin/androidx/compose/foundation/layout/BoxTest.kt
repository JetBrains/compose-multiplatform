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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class BoxTest : LayoutTest() {
    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testBox() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

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
        val doubleSize = (sizeDp * 2).roundToPx()

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
        val size = sizeDp.roundToPx()
        val tripleSizeDp = sizeDp * 3
        val tripleSize = (sizeDp * 3).roundToPx()

        val positionedLatch = CountDownLatch(10)
        val stackSize = Ref<IntSize>()
        val childSize = Array(9) { Ref<IntSize>() }
        val childPosition = Array(9) { Ref<Offset>() }
        show {
            Box(Modifier.wrapContentSize(Alignment.TopStart)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        Modifier
                            .size(tripleSizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                stackSize.value = coordinates.size
                                positionedLatch.countDown()
                            }
                    ) {
                        Box(
                            Modifier.align(Alignment.TopStart)
                                .size(sizeDp, sizeDp)
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.TopCenter)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.TopEnd)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.CenterStart)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[3], childPosition[3], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.Center)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[4], childPosition[4], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.CenterEnd)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[5], childPosition[5], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomStart)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[6], childPosition[6], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize[7], childPosition[7], positionedLatch)
                        ) {
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd)
                                .size(sizeDp)
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
                    Modifier.size(
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
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.requiredSize(outerSize)
            ) {
                Box(
                    Modifier.requiredSize(innerSize).onGloballyPositioned {
                        assertEquals(outerSizePx - innerSizePx, it.positionInParent().x)
                        assertEquals(outerSizePx - innerSizePx, it.positionInParent().y)
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
            Box(Modifier.requiredSize(sizeDp)) {
                Box(
                    Modifier.align(Alignment.BottomEnd).align(Alignment.TopStart)
                        .onGloballyPositioned {
                            assertEquals(size, it.positionInParent().x)
                            assertEquals(size, it.positionInParent().y)
                            positionedLatch.countDown()
                        }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testBox_childAffectsBoxSize() {
        var layoutLatch = CountDownLatch(2)
        val size = mutableStateOf(10.dp)
        var measure = 0
        var layout = 0
        show {
            Box {
                Layout(
                    content = {
                        Box {
                            Box(
                                Modifier.requiredSize(size.value, 10.dp).onGloballyPositioned {
                                    layoutLatch.countDown()
                                }
                            )
                        }
                    },
                    measurePolicy = remember {
                        MeasurePolicy { measurables, constraints ->
                            val placeable = measurables.first().measure(constraints)
                            ++measure
                            layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                                ++layout
                                layoutLatch.countDown()
                            }
                        }
                    }
                )
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
    fun testBox_canPropagateMinConstraints() = with(density) {
        val measuredLatch = CountDownLatch(1)

        show {
            Box(
                Modifier.requiredWidthIn(20.dp, 40.dp),
                propagateMinConstraints = true
            ) {
                Box(
                    Modifier.width(10.dp).onSizeChanged {
                        assertEquals(20.dp.roundToPx(), it.width)
                        measuredLatch.countDown()
                    }
                )
            }
        }

        assertTrue(measuredLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testBox_tracksPropagateMinConstraintsChanges() = with(density) {
        val measuredLatch = CountDownLatch(2)

        val pmc = mutableStateOf(true)

        show {
            Box(
                Modifier.requiredWidthIn(20.dp, 40.dp),
                propagateMinConstraints = pmc.value,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(10.dp)
                        .onSizeChanged {
                            if (measuredLatch.count == 2L) {
                                assertEquals(20.dp.roundToPx(), it.width)
                                pmc.value = false
                            } else {
                                assertEquals(10.dp.roundToPx(), it.width)
                            }
                            measuredLatch.countDown()
                        }
                )
            }
        }
        assertTrue(measuredLatch.await(1, TimeUnit.SECONDS))
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
            assertEquals(testWidth.roundToPx(), minIntrinsicWidth(0.dp.roundToPx()))
            assertEquals(expectedWidth, minIntrinsicWidth(testDimension))
            assertEquals(testWidth.roundToPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(testHeight.roundToPx(), minIntrinsicHeight(0.dp.roundToPx()))
            assertEquals(expectedHeight, minIntrinsicHeight(testDimension))
            assertEquals(testHeight.roundToPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(testWidth.roundToPx(), maxIntrinsicWidth(0.dp.roundToPx()))
            assertEquals(expectedWidth, maxIntrinsicWidth(testDimension))
            assertEquals(testWidth.roundToPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(testHeight.roundToPx(), maxIntrinsicHeight(0.dp.roundToPx()))
            assertEquals(expectedHeight, maxIntrinsicHeight(testDimension))
            assertEquals(testHeight.roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
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
            assertEquals(0.dp.roundToPx(), minIntrinsicWidth(50.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0.dp.roundToPx(), minIntrinsicHeight(50.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0.dp.roundToPx(), maxIntrinsicWidth(50.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0.dp.roundToPx(), maxIntrinsicHeight(50.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testAlignInspectableValue() {
        val modifier = with(BoxScopeInstance) { Modifier.align(Alignment.BottomCenter) }
            as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("align")
        assertThat(modifier.valueOverride).isEqualTo(Alignment.BottomCenter)
        assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }

    @Test
    fun testMatchParentSizeInspectableValue() {
        val modifier = with(BoxScopeInstance) { Modifier.matchParentSize() }
            as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("matchParentSize")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }
}
