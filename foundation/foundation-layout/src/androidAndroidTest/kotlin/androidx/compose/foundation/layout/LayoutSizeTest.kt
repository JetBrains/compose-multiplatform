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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class LayoutSizeTest : LayoutTest() {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testPreferredSize_withWidthSizeModifiers() = with(density) {
        val sizeDp = 50.toDp()
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntSize>() }
        val position = MutableList(6) { Ref<Offset>() }
        show {
            Box {
                Column {
                    Container(
                        Modifier.preferredWidthIn(min = sizeDp, max = sizeDp * 2)
                            .preferredHeight(sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredWidthIn(max = sizeDp * 2)
                            .preferredHeight(sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredWidthIn(min = sizeDp)
                            .preferredHeight(sizeDp)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredWidthIn(max = sizeDp)
                            .preferredWidthIn(min = sizeDp * 2)
                            .preferredHeight(sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredWidthIn(min = sizeDp * 2)
                            .preferredWidthIn(max = sizeDp)
                            .preferredHeight(sizeDp)
                            .saveLayoutInfo(size[4], position[4], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredSize(sizeDp)
                            .saveLayoutInfo(size[5], position[5], positionedLatch)
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(Offset.Zero, position[0].value)

        assertEquals(IntSize(0, sizeIpx), size[1].value)
        assertEquals(Offset(0f, sizeIpx.toFloat()), position[1].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(Offset(0f, (sizeIpx * 2).toFloat()), position[2].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(Offset(0f, (sizeIpx * 3).toFloat()), position[3].value)

        assertEquals(IntSize((sizeDp * 2).toIntPx(), sizeIpx), size[4].value)
        assertEquals(Offset(0f, (sizeIpx * 4).toFloat()), position[4].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(Offset(0f, (sizeIpx * 5).toFloat()), position[5].value)
    }

    @Test
    fun testPreferredSize_withHeightSizeModifiers() = with(density) {
        val sizeDp = 10.toDp()
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntSize>() }
        val position = MutableList(6) { Ref<Offset>() }
        show {
            Box {
                Row {
                    Container(
                        Modifier.preferredHeightIn(min = sizeDp, max = sizeDp * 2)
                            .preferredWidth(sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredHeightIn(max = sizeDp * 2)
                            .preferredWidth(sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredHeightIn(min = sizeDp)
                            .preferredWidth(sizeDp)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredHeightIn(max = sizeDp)
                            .preferredHeightIn(min = sizeDp * 2)
                            .preferredWidth(sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredHeightIn(min = sizeDp * 2)
                            .preferredHeightIn(max = sizeDp)
                            .preferredWidth(sizeDp)
                            .saveLayoutInfo(size[4], position[4], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredHeight(sizeDp).then(Modifier.preferredWidth(sizeDp)).then(
                            Modifier.saveLayoutInfo(size[5], position[5], positionedLatch)
                        )
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(Offset.Zero, position[0].value)

        assertEquals(IntSize(sizeIpx, 0), size[1].value)
        assertEquals(Offset(sizeIpx.toFloat(), 0f), position[1].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(Offset((sizeIpx * 2).toFloat(), 0f), position[2].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[3].value)
        assertEquals(Offset((sizeIpx * 3).toFloat(), 0f), position[3].value)

        assertEquals(IntSize(sizeIpx, (sizeDp * 2).toIntPx()), size[4].value)
        assertEquals(Offset((sizeIpx * 4).toFloat(), 0f), position[4].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(Offset((sizeIpx * 5).toFloat(), 0f), position[5].value)
    }

    @Test
    fun testPreferredSize_withSizeModifiers() = with(density) {
        val sizeDp = 50.toDp()
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(5)
        val size = MutableList(5) { Ref<IntSize>() }
        val position = MutableList(5) { Ref<Offset>() }
        show {
            Box {
                Row {
                    val maxSize = sizeDp * 2
                    Container(
                        Modifier.preferredSizeIn(maxWidth = maxSize, maxHeight = maxSize)
                            .preferredSizeIn(minWidth = sizeDp, minHeight = sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredSizeIn(maxWidth = sizeDp, maxHeight = sizeDp)
                            .preferredSizeIn(minWidth = sizeDp * 2, minHeight = sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    val maxSize1 = sizeDp * 2
                    Container(
                        Modifier.preferredSizeIn(minWidth = sizeDp, minHeight = sizeDp)
                            .preferredSizeIn(maxWidth = maxSize1, maxHeight = maxSize1)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    val minSize = sizeDp * 2
                    Container(
                        Modifier.preferredSizeIn(minWidth = minSize, minHeight = minSize)
                            .preferredSizeIn(maxWidth = sizeDp, maxHeight = sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredSize(sizeDp)
                            .saveLayoutInfo(size[4], position[4], positionedLatch)
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(Offset.Zero, position[0].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(Offset(sizeIpx.toFloat(), 0f), position[1].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(Offset((sizeIpx * 2).toFloat(), 0f), position[2].value)

        assertEquals(IntSize(sizeIpx * 2, sizeIpx * 2), size[3].value)
        assertEquals(Offset((sizeIpx * 3).toFloat(), 0f), position[3].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[4].value)
        assertEquals(Offset((sizeIpx * 5).toFloat(), 0f), position[4].value)
    }

    @Test
    fun testPreferredSizeModifiers_respectMaxConstraint() = with(density) {
        val sizeDp = 100.toDp()
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val constrainedBoxSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box {
                Container(width = sizeDp, height = sizeDp) {
                    Container(
                        Modifier.preferredWidth(sizeDp * 2)
                            .preferredHeight(sizeDp * 3)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                constrainedBoxSize.value = coordinates.size
                                positionedLatch.countDown()
                            }
                    ) {
                        Container(
                            expanded = true,
                            modifier = Modifier.saveLayoutInfo(
                                size = childSize,
                                position = childPosition,
                                positionedLatch = positionedLatch
                            )
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), constrainedBoxSize.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(Offset.Zero, childPosition.value)
    }

    @Test
    fun testMaxModifiers_withInfiniteValue() = with(density) {
        val sizeDp = 20.toDp()
        val sizeIpx = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(4)
        val size = MutableList(4) { Ref<IntSize>() }
        val position = MutableList(4) { Ref<Offset>() }
        show {
            Box {
                Row {
                    Container(Modifier.preferredWidthIn(max = Dp.Infinity)) {
                        Container(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.saveLayoutInfo(
                                size[0], position[0],
                                positionedLatch
                            )
                        ) {
                        }
                    }
                    Container(Modifier.preferredHeightIn(max = Dp.Infinity)) {
                        Container(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.saveLayoutInfo(
                                size[1],
                                position[1],
                                positionedLatch
                            )
                        ) {
                        }
                    }
                    Container(
                        Modifier.preferredWidth(sizeDp)
                            .preferredHeight(sizeDp)
                            .preferredWidthIn(max = Dp.Infinity)
                            .preferredHeightIn(max = Dp.Infinity)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.preferredSizeIn(
                            maxWidth = Dp.Infinity,
                            maxHeight = Dp.Infinity
                        )
                    ) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.saveLayoutInfo(
                                size[3],
                                position[3],
                                positionedLatch
                            )
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx, sizeIpx), size[0].value)
        assertEquals(IntSize(sizeIpx, sizeIpx), size[1].value)
        assertEquals(IntSize(sizeIpx, sizeIpx), size[2].value)
        assertEquals(IntSize(sizeIpx, sizeIpx), size[3].value)
    }

    @Test
    fun testSize_smallerInLarger() = with(density) {
        val sizeIpx = 64
        val sizeDp = sizeIpx.toDp()

        val positionedLatch = CountDownLatch(1)
        val boxSize = Ref<IntSize>()
        val boxPosition = Ref<Offset>()
        show {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .size(sizeDp * 2)
                    .size(sizeDp)
                    .saveLayoutInfo(boxSize, boxPosition, positionedLatch)
            )
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx, sizeIpx), boxSize.value)
        assertEquals(
            Offset(
                (sizeIpx / 2).toFloat(),
                (sizeIpx / 2).toFloat()
            ),
            boxPosition.value
        )
    }

    @Test
    fun testSize_largerInSmaller() = with(density) {
        val sizeIpx = 64
        val sizeDp = sizeIpx.toDp()

        val positionedLatch = CountDownLatch(1)
        val boxSize = Ref<IntSize>()
        val boxPosition = Ref<Offset>()
        show {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .size(sizeDp)
                    .size(sizeDp * 2)
                    .saveLayoutInfo(boxSize, boxPosition, positionedLatch)
            )
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(sizeIpx * 2, sizeIpx * 2), boxSize.value)
        assertEquals(
            Offset((-sizeIpx / 2).toFloat(), (-sizeIpx / 2).toFloat()),
            boxPosition.value
        )
    }

    @Test
    fun testMeasurementConstraints_preferredSatisfiable() = with(density) {
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredWidth(20.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredHeight(20.toDp()),
            Constraints(10, 30, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredSize(20.toDp()),
            Constraints(20, 20, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredWidthIn(20.toDp(), 25.toDp()),
            Constraints(20, 25, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredHeightIn(20.toDp(), 25.toDp()),
            Constraints(10, 30, 20, 25)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredSizeIn(20.toDp(), 20.toDp(), 25.toDp(), 25.toDp()),
            Constraints(20, 25, 20, 25)
        )
    }

    @Test
    fun testMeasurementConstraints_preferredUnsatisfiable() = with(density) {
        assertConstraints(
            Constraints(20, 40, 15, 35),
            Modifier.preferredWidth(15.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredHeight(10.toDp()),
            Constraints(10, 30, 15, 15)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredSize(40.toDp()),
            Constraints(30, 30, 35, 35)
        )
        assertConstraints(
            Constraints(20, 30, 15, 35),
            Modifier.preferredWidthIn(10.toDp(), 15.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredHeightIn(5.toDp(), 10.toDp()),
            Constraints(10, 30, 15, 15)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.preferredSizeIn(40.toDp(), 50.toDp(), 45.toDp(), 55.toDp()),
            Constraints(30, 30, 35, 35)
        )
    }

    @Test
    fun testMeasurementConstraints_compulsorySatisfiable() = with(density) {
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.width(20.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.height(20.toDp()),
            Constraints(10, 30, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.size(20.toDp()),
            Constraints(20, 20, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.widthIn(20.toDp(), 25.toDp()),
            Constraints(20, 25, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.heightIn(20.toDp(), 25.toDp()),
            Constraints(10, 30, 20, 25)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.sizeIn(20.toDp(), 20.toDp(), 25.toDp(), 25.toDp()),
            Constraints(20, 25, 20, 25)
        )
    }

    @Test
    fun testMeasurementConstraints_compulsoryUnsatisfiable() = with(density) {
        assertConstraints(
            Constraints(20, 40, 15, 35),
            Modifier.width(15.toDp()),
            Constraints(15, 15, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.height(10.toDp()),
            Constraints(10, 30, 10, 10)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.size(40.toDp()),
            Constraints(40, 40, 40, 40)
        )
        assertConstraints(
            Constraints(20, 30, 15, 35),
            Modifier.widthIn(10.toDp(), 15.toDp()),
            Constraints(10, 15, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.heightIn(5.toDp(), 10.toDp()),
            Constraints(10, 30, 5, 10)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.sizeIn(40.toDp(), 50.toDp(), 45.toDp(), 55.toDp()),
            Constraints(40, 45, 50, 55)
        )
        // When one dimension is unspecified and the other contradicts the incoming constraint.
        assertConstraints(
            Constraints(10, 10, 10, 10),
            Modifier.sizeIn(20.toDp(), 30.toDp(), Dp.Unspecified, Dp.Unspecified),
            Constraints(20, 20, 30, 30)
        )
        assertConstraints(
            Constraints(40, 40, 40, 40),
            Modifier.sizeIn(Dp.Unspecified, Dp.Unspecified, 20.toDp(), 30.toDp()),
            Constraints(20, 20, 30, 30)
        )
    }

    @Test
    fun testDefaultMinSizeConstraints() = with(density) {
        val latch = CountDownLatch(3)
        show {
            // Constraints are applied.
            Layout(
                {},
                Modifier.wrapContentSize()
                    .sizeIn(maxWidth = 30.toDp(), maxHeight = 40.toDp())
                    .defaultMinSizeConstraints(minWidth = 10.toDp(), minHeight = 20.toDp())
            ) { _, constraints ->
                assertEquals(10, constraints.minWidth)
                assertEquals(20, constraints.minHeight)
                assertEquals(30, constraints.maxWidth)
                assertEquals(40, constraints.maxHeight)
                latch.countDown()
                layout(0, 0) {}
            }
            // Constraints are not applied.
            Layout(
                {},
                Modifier.sizeIn(
                    minWidth = 10.toDp(),
                    minHeight = 20.toDp(),
                    maxWidth = 100.toDp(),
                    maxHeight = 110.toDp()
                ).defaultMinSizeConstraints(
                    minWidth = 50.toDp(),
                    minHeight = 50.toDp()
                )
            ) { _, constraints ->
                assertEquals(10, constraints.minWidth)
                assertEquals(20, constraints.minHeight)
                assertEquals(100, constraints.maxWidth)
                assertEquals(110, constraints.maxHeight)
                latch.countDown()
                layout(0, 0) {}
            }
            // Defaults values are not changing.
            Layout(
                {},
                Modifier.sizeIn(
                    minWidth = 10.toDp(),
                    minHeight = 20.toDp(),
                    maxWidth = 100.toDp(),
                    maxHeight = 110.toDp()
                ).defaultMinSizeConstraints()
            ) { _, constraints ->
                assertEquals(10, constraints.minWidth)
                assertEquals(20, constraints.minHeight)
                assertEquals(100, constraints.maxWidth)
                assertEquals(110, constraints.maxHeight)
                latch.countDown()
                layout(0, 0) {}
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testDefaultMinSizeConstraints_withCoercingMaxConstraints() = with(density) {
        val latch = CountDownLatch(1)
        show {
            Layout(
                {},
                Modifier.wrapContentSize()
                    .sizeIn(maxWidth = 30.toDp(), maxHeight = 40.toDp())
                    .defaultMinSizeConstraints(minWidth = 70.toDp(), minHeight = 80.toDp())
            ) { _, constraints ->
                assertEquals(30, constraints.minWidth)
                assertEquals(40, constraints.minHeight)
                assertEquals(30, constraints.maxWidth)
                assertEquals(40, constraints.maxHeight)
                latch.countDown()
                layout(0, 0) {}
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testMinWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredWidthIn(min = 10.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10, minIntrinsicWidth(0))
            assertEquals(10, minIntrinsicWidth(5))
            assertEquals(50, minIntrinsicWidth(50))
            assertEquals(10, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0, minIntrinsicHeight(0))
            assertEquals(35, minIntrinsicHeight(35))
            assertEquals(50, minIntrinsicHeight(50))
            assertEquals(0, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(10, maxIntrinsicWidth(0))
            assertEquals(10, maxIntrinsicWidth(5))
            assertEquals(50, maxIntrinsicWidth(50))
            assertEquals(10, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0, maxIntrinsicHeight(0))
            assertEquals(35, maxIntrinsicHeight(35))
            assertEquals(50, maxIntrinsicHeight(50))
            assertEquals(0, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testMaxWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredWidthIn(max = 20.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(20, minIntrinsicWidth(50))
            assertEquals(0, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0, minIntrinsicHeight(0))
            assertEquals(15, minIntrinsicHeight(15))
            assertEquals(50, minIntrinsicHeight(50))
            assertEquals(0, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(20, maxIntrinsicWidth(50))
            assertEquals(0, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0, maxIntrinsicHeight(0))
            assertEquals(15, maxIntrinsicHeight(15))
            assertEquals(50, maxIntrinsicHeight(50))
            assertEquals(0, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testMinHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredHeightIn(min = 30.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(50, minIntrinsicWidth(50))
            assertEquals(0, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(30, minIntrinsicHeight(0))
            assertEquals(30, minIntrinsicHeight(15))
            assertEquals(50, minIntrinsicHeight(50))
            assertEquals(30, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(50, maxIntrinsicWidth(50))
            assertEquals(0, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(30, maxIntrinsicHeight(0))
            assertEquals(30, maxIntrinsicHeight(15))
            assertEquals(50, maxIntrinsicHeight(50))
            assertEquals(30, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testMaxHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredHeightIn(max = 40.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(50, minIntrinsicWidth(50))
            assertEquals(0, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0, minIntrinsicHeight(0))
            assertEquals(15, minIntrinsicHeight(15))
            assertEquals(40, minIntrinsicHeight(50))
            assertEquals(0, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(50, maxIntrinsicWidth(50))
            assertEquals(0, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0, maxIntrinsicHeight(0))
            assertEquals(15, maxIntrinsicHeight(15))
            assertEquals(40, maxIntrinsicHeight(50))
            assertEquals(0, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testWidthModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredWidth(10.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10, minIntrinsicWidth(0))
            assertEquals(10, minIntrinsicWidth(10))
            assertEquals(10, minIntrinsicWidth(75))
            assertEquals(10, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0, minIntrinsicHeight(0))
            assertEquals(35, minIntrinsicHeight(35))
            assertEquals(70, minIntrinsicHeight(70))
            assertEquals(0, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(10, maxIntrinsicWidth(0))
            assertEquals(10, maxIntrinsicWidth(15))
            assertEquals(10, maxIntrinsicWidth(75))
            assertEquals(10, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0, maxIntrinsicHeight(0))
            assertEquals(35, maxIntrinsicHeight(35))
            assertEquals(70, maxIntrinsicHeight(70))
            assertEquals(0, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testHeightModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredHeight(10.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(75, minIntrinsicWidth(75))
            assertEquals(0, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(10, minIntrinsicHeight(0))
            assertEquals(10, minIntrinsicHeight(35))
            assertEquals(10, minIntrinsicHeight(70))
            assertEquals(10, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(75, maxIntrinsicWidth(75))
            assertEquals(0, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(10, maxIntrinsicHeight(0))
            assertEquals(10, maxIntrinsicHeight(35))
            assertEquals(10, maxIntrinsicHeight(70))
            assertEquals(10, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testWidthHeightModifiers_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(
                    Modifier.preferredSizeIn(
                        minWidth = 10.toDp(),
                        maxWidth = 20.toDp(),
                        minHeight = 30.toDp(),
                        maxHeight = 40.toDp()
                    )
                ) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(10, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(20, minIntrinsicWidth(50))
            assertEquals(10, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(30, minIntrinsicHeight(0))
            assertEquals(35, minIntrinsicHeight(35))
            assertEquals(40, minIntrinsicHeight(50))
            assertEquals(30, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(10, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(20, maxIntrinsicWidth(50))
            assertEquals(10, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(30, maxIntrinsicHeight(0))
            assertEquals(35, maxIntrinsicHeight(35))
            assertEquals(40, maxIntrinsicHeight(50))
            assertEquals(30, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testMinSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredSizeIn(minWidth = 20.toDp(), minHeight = 30.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(20, minIntrinsicWidth(0))
            assertEquals(20, minIntrinsicWidth(10))
            assertEquals(50, minIntrinsicWidth(50))
            assertEquals(20, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(30, minIntrinsicHeight(0))
            assertEquals(30, minIntrinsicHeight(10))
            assertEquals(50, minIntrinsicHeight(50))
            assertEquals(30, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(20, maxIntrinsicWidth(0))
            assertEquals(20, maxIntrinsicWidth(10))
            assertEquals(50, maxIntrinsicWidth(50))
            assertEquals(20, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(30, maxIntrinsicHeight(0))
            assertEquals(30, maxIntrinsicHeight(10))
            assertEquals(50, maxIntrinsicHeight(50))
            assertEquals(30, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testMaxSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredSizeIn(maxWidth = 40.toDp(), maxHeight = 50.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(15, minIntrinsicWidth(15))
            assertEquals(40, minIntrinsicWidth(50))
            assertEquals(0, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(0, minIntrinsicHeight(0))
            assertEquals(15, minIntrinsicHeight(15))
            assertEquals(50, minIntrinsicHeight(75))
            assertEquals(0, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(0, maxIntrinsicWidth(0))
            assertEquals(15, maxIntrinsicWidth(15))
            assertEquals(40, maxIntrinsicWidth(50))
            assertEquals(0, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(0, maxIntrinsicHeight(0))
            assertEquals(15, maxIntrinsicHeight(15))
            assertEquals(50, maxIntrinsicHeight(75))
            assertEquals(0, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testPreferredSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.preferredSize(40.toDp(), 50.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(40, minIntrinsicWidth(0))
            assertEquals(40, minIntrinsicWidth(35))
            assertEquals(40, minIntrinsicWidth(75))
            assertEquals(40, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(50, minIntrinsicHeight(0))
            assertEquals(50, minIntrinsicHeight(35))
            assertEquals(50, minIntrinsicHeight(70))
            assertEquals(50, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(40, maxIntrinsicWidth(0))
            assertEquals(40, maxIntrinsicWidth(35))
            assertEquals(40, maxIntrinsicWidth(75))
            assertEquals(40, maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(50, maxIntrinsicHeight(0))
            assertEquals(50, maxIntrinsicHeight(35))
            assertEquals(50, maxIntrinsicHeight(70))
            assertEquals(50, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testFillModifier_correctSize() = with(density) {
        val parentWidth = 100
        val parentHeight = 80
        val parentModifier = Modifier.size(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 40
        val childHeight = 30
        val childModifier = Modifier.preferredSize(childWidth.toDp(), childHeight.toDp())

        assertEquals(
            IntSize(childWidth, childHeight),
            calculateSizeFor(parentModifier, childModifier)
        )
        assertEquals(
            IntSize(parentWidth, childHeight),
            calculateSizeFor(parentModifier, Modifier.fillMaxWidth().then(childModifier))
        )
        assertEquals(
            IntSize(childWidth, parentHeight),
            calculateSizeFor(parentModifier, Modifier.fillMaxHeight().then(childModifier))
        )
        assertEquals(
            IntSize(parentWidth, parentHeight),
            calculateSizeFor(parentModifier, Modifier.fillMaxSize().then(childModifier))
        )
    }

    @Test
    fun testFractionalFillModifier_correctSize_whenSmallerChild() = with(density) {
        val parentWidth = 100
        val parentHeight = 80
        val parentModifier = Modifier.size(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 40
        val childHeight = 30
        val childModifier = Modifier.preferredSize(childWidth.toDp(), childHeight.toDp())

        assertEquals(
            IntSize(childWidth, childHeight),
            calculateSizeFor(parentModifier, childModifier)
        )
        assertEquals(
            IntSize(parentWidth / 2, childHeight),
            calculateSizeFor(parentModifier, Modifier.fillMaxWidth(0.5f).then(childModifier))
        )
        assertEquals(
            IntSize(childWidth, parentHeight / 2),
            calculateSizeFor(parentModifier, Modifier.fillMaxHeight(0.5f).then(childModifier))
        )
        assertEquals(
            IntSize(parentWidth / 2, parentHeight / 2),
            calculateSizeFor(parentModifier, Modifier.fillMaxSize(0.5f).then(childModifier))
        )
    }

    @Test
    fun testFractionalFillModifier_correctSize_whenLargerChild() = with(density) {
        val parentWidth = 100
        val parentHeight = 80
        val parentModifier = Modifier.size(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 70
        val childHeight = 50
        val childModifier = Modifier.preferredSize(childWidth.toDp(), childHeight.toDp())

        assertEquals(
            IntSize(childWidth, childHeight),
            calculateSizeFor(parentModifier, childModifier)
        )
        assertEquals(
            IntSize(parentWidth / 2, childHeight),
            calculateSizeFor(parentModifier, Modifier.fillMaxWidth(0.5f).then(childModifier))
        )
        assertEquals(
            IntSize(childWidth, parentHeight / 2),
            calculateSizeFor(parentModifier, Modifier.fillMaxHeight(0.5f).then(childModifier))
        )
        assertEquals(
            IntSize(parentWidth / 2, parentHeight / 2),
            calculateSizeFor(parentModifier, Modifier.fillMaxSize(0.5f).then(childModifier))
        )
    }

    @Test
    fun testFractionalFillModifier_coerced() = with(density) {
        val childMinWidth = 40
        val childMinHeight = 30
        val childMaxWidth = 60
        val childMaxHeight = 50
        val childModifier = Modifier.sizeIn(
            childMinWidth.toDp(),
            childMinHeight.toDp(),
            childMaxWidth.toDp(),
            childMaxHeight.toDp()
        )

        assertEquals(
            IntSize(childMinWidth, childMinHeight),
            calculateSizeFor(Modifier, childModifier.then(Modifier.fillMaxSize(-0.1f)))
        )
        assertEquals(
            IntSize(childMinWidth, childMinHeight),
            calculateSizeFor(Modifier, childModifier.then(Modifier.fillMaxSize(0.1f)))
        )
        assertEquals(
            IntSize(childMaxWidth, childMaxHeight),
            calculateSizeFor(Modifier, childModifier.then(Modifier.fillMaxSize(1.2f)))
        )
    }

    @Test
    fun testFillModifier_noChangeIntrinsicMeasurements() = with(density) {
        verifyIntrinsicMeasurements(Modifier.fillMaxWidth())
        verifyIntrinsicMeasurements(Modifier.fillMaxHeight())
        verifyIntrinsicMeasurements(Modifier.fillMaxSize())
    }

    @Test
    fun testDefaultMinSizeConstraintsModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.defaultMinSizeConstraints(40.toDp(), 50.toDp())) {
                    Container(Modifier.aspectRatio(1f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, _, _ ->
            // Min width.
            assertEquals(40, minIntrinsicWidth(0))
            assertEquals(40, minIntrinsicWidth(35))
            assertEquals(55, minIntrinsicWidth(55))
            assertEquals(40, minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(50, minIntrinsicHeight(0))
            assertEquals(50, minIntrinsicHeight(35))
            assertEquals(55, minIntrinsicHeight(55))
            assertEquals(50, minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(40, minIntrinsicWidth(0))
            assertEquals(40, minIntrinsicWidth(35))
            assertEquals(55, minIntrinsicWidth(55))
            assertEquals(40, minIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(50, minIntrinsicHeight(0))
            assertEquals(50, minIntrinsicHeight(35))
            assertEquals(55, minIntrinsicHeight(55))
            assertEquals(50, minIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testInspectableParameter() {
        checkModifier(Modifier.width(200.0.dp), "width", 200.0.dp, listOf())
        checkModifier(Modifier.height(300.0.dp), "height", 300.0.dp, listOf())
        checkModifier(Modifier.size(400.0.dp), "size", 400.0.dp, listOf())
        checkModifier(
            Modifier.size(100.0.dp, 200.0.dp), "size", null,
            listOf(ValueElement("width", 100.0.dp), ValueElement("height", 200.0.dp))
        )
        checkModifier(
            Modifier.widthIn(100.0.dp, 200.0.dp), "widthIn", null,
            listOf(ValueElement("min", 100.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.heightIn(10.0.dp, 200.0.dp), "heightIn", null,
            listOf(ValueElement("min", 10.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.sizeIn(10.dp, 20.dp, 30.dp, 40.dp), "sizeIn", null,
            listOf(
                ValueElement("minWidth", 10.dp), ValueElement("minHeight", 20.dp),
                ValueElement("maxWidth", 30.dp), ValueElement("maxHeight", 40.dp)
            )
        )
        checkModifier(Modifier.preferredWidth(200.0.dp), "preferredWidth", 200.0.dp, listOf())
        checkModifier(Modifier.preferredHeight(300.0.dp), "preferredHeight", 300.0.dp, listOf())
        checkModifier(Modifier.preferredSize(400.0.dp), "preferredSize", 400.0.dp, listOf())
        checkModifier(
            Modifier.preferredSize(100.0.dp, 200.0.dp), "preferredSize", null,
            listOf(ValueElement("width", 100.0.dp), ValueElement("height", 200.0.dp))
        )
        checkModifier(
            Modifier.preferredWidthIn(100.0.dp, 200.0.dp), "preferredWidthIn", null,
            listOf(ValueElement("min", 100.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.preferredHeightIn(10.0.dp, 200.0.dp), "preferredHeightIn", null,
            listOf(ValueElement("min", 10.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.preferredSizeIn(10.dp, 20.dp, 30.dp, 40.dp), "preferredSizeIn", null,
            listOf(
                ValueElement("minWidth", 10.dp), ValueElement("minHeight", 20.dp),
                ValueElement("maxWidth", 30.dp), ValueElement("maxHeight", 40.dp)
            )
        )

        checkModifier(
            Modifier.fillMaxWidth(), "fillMaxWidth", null,
            listOf(ValueElement("fraction", 1.0f))
        )
        checkModifier(
            Modifier.fillMaxWidth(0.7f), "fillMaxWidth", null,
            listOf(ValueElement("fraction", 0.7f))
        )
        checkModifier(
            Modifier.fillMaxHeight(), "fillMaxHeight", null,
            listOf(ValueElement("fraction", 1.0f))
        )
        checkModifier(
            Modifier.fillMaxHeight(0.15f), "fillMaxHeight", null,
            listOf(ValueElement("fraction", 0.15f))
        )
        checkModifier(
            Modifier.fillMaxSize(), "fillMaxSize", null,
            listOf(ValueElement("fraction", 1.0f))
        )
        checkModifier(
            Modifier.fillMaxSize(0.25f), "fillMaxSize", null,
            listOf(ValueElement("fraction", 0.25f))
        )

        checkModifier(
            Modifier.wrapContentWidth(), "wrapContentWidth", null,
            listOf(
                ValueElement("align", Alignment.CenterHorizontally),
                ValueElement("unbounded", false)
            )
        )
        checkModifier(
            Modifier.wrapContentWidth(Alignment.End, true), "wrapContentWidth", null,
            listOf(
                ValueElement("align", Alignment.End),
                ValueElement("unbounded", true)
            )
        )
        checkModifier(
            Modifier.wrapContentHeight(), "wrapContentHeight", null,
            listOf(
                ValueElement("align", Alignment.CenterVertically),
                ValueElement("unbounded", false)
            )
        )
        checkModifier(
            Modifier.wrapContentHeight(Alignment.Bottom, true), "wrapContentHeight", null,
            listOf(
                ValueElement("align", Alignment.Bottom),
                ValueElement("unbounded", true)
            )
        )
        checkModifier(
            Modifier.wrapContentSize(), "wrapContentSize", null,
            listOf(
                ValueElement("align", Alignment.Center),
                ValueElement("unbounded", false)
            )
        )
        checkModifier(
            Modifier.wrapContentSize(Alignment.BottomCenter, true), "wrapContentSize", null,
            listOf(
                ValueElement("align", Alignment.BottomCenter),
                ValueElement("unbounded", true)
            )
        )

        checkModifier(
            Modifier.defaultMinSizeConstraints(10.0.dp, 20.0.dp),
            "defaultMinSizeConstraints", null,
            listOf(ValueElement("minWidth", 10.dp), ValueElement("minHeight", 20.dp))
        )
    }

    private fun checkModifier(
        modifier: Modifier,
        expectedName: String,
        expectedValue: Any?,
        expectedElements: List<ValueElement>
    ) {
        assertThat(modifier).isInstanceOf(InspectableValue::class.java)
        val parameter = modifier as InspectableValue
        assertThat(parameter.nameFallback).isEqualTo(expectedName)
        assertThat(parameter.valueOverride).isEqualTo(expectedValue)
        assertThat(parameter.inspectableElements.toList()).isEqualTo(expectedElements)
    }

    private fun calculateSizeFor(parentModifier: Modifier, modifier: Modifier): IntSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<IntSize>()
        val position = Ref<Offset>()
        show {
            Box(parentModifier) {
                Box(modifier.saveLayoutInfo(size, position, positionedLatch))
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        return size.value!!
    }

    private fun assertConstraints(
        incomingConstraints: Constraints,
        modifier: Modifier,
        expectedConstraints: Constraints
    ) {
        val latch = CountDownLatch(1)
        show {
            Layout({
                WithConstraints(modifier) {
                    assertEquals(expectedConstraints, constraints)
                    latch.countDown()
                }
            }) { measurables, _ ->
                measurables[0].measure(incomingConstraints)
                layout(0, 0) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    private fun verifyIntrinsicMeasurements(expandedModifier: Modifier) = with(density) {
        // intrinsic measurements do not change with the ExpandedModifier
        testIntrinsics(
            @Composable {
                Container(
                    expandedModifier.then(Modifier.aspectRatio(2f)),
                    width = 30.toDp(), height = 40.toDp()
                ) { }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Width
            assertEquals(40, minIntrinsicWidth(20))
            assertEquals(30, minIntrinsicWidth(Constraints.Infinity))

            assertEquals(40, maxIntrinsicWidth(20))
            assertEquals(30, maxIntrinsicWidth(Constraints.Infinity))

            // Height
            assertEquals(20, minIntrinsicHeight(40))
            assertEquals(40, minIntrinsicHeight(Constraints.Infinity))

            assertEquals(20, maxIntrinsicHeight(40))
            assertEquals(40, maxIntrinsicHeight(Constraints.Infinity))
        }
    }
}