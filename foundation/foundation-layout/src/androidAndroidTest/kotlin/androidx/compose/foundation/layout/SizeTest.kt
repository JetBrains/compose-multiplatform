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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.Ref
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import org.junit.Assert.assertNotEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class SizeTest : LayoutTest() {

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
        val sizeIpx = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntSize>() }
        val position = MutableList(6) { Ref<Offset>() }
        show {
            Box {
                Column {
                    Container(
                        Modifier.widthIn(min = sizeDp, max = sizeDp * 2)
                            .height(sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.widthIn(max = sizeDp * 2)
                            .height(sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.widthIn(min = sizeDp)
                            .height(sizeDp)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.widthIn(max = sizeDp)
                            .widthIn(min = sizeDp * 2)
                            .height(sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.widthIn(min = sizeDp * 2)
                            .widthIn(max = sizeDp)
                            .height(sizeDp)
                            .saveLayoutInfo(size[4], position[4], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.size(sizeDp)
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

        assertEquals(IntSize((sizeDp * 2).roundToPx(), sizeIpx), size[4].value)
        assertEquals(Offset(0f, (sizeIpx * 4).toFloat()), position[4].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(Offset(0f, (sizeIpx * 5).toFloat()), position[5].value)
    }

    @Test
    fun testPreferredSize_withHeightSizeModifiers() = with(density) {
        val sizeDp = 10.toDp()
        val sizeIpx = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(6)
        val size = MutableList(6) { Ref<IntSize>() }
        val position = MutableList(6) { Ref<Offset>() }
        show {
            Box {
                Row {
                    Container(
                        Modifier.heightIn(min = sizeDp, max = sizeDp * 2)
                            .width(sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.heightIn(max = sizeDp * 2)
                            .width(sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.heightIn(min = sizeDp)
                            .width(sizeDp)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.heightIn(max = sizeDp)
                            .heightIn(min = sizeDp * 2)
                            .width(sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.heightIn(min = sizeDp * 2)
                            .heightIn(max = sizeDp)
                            .width(sizeDp)
                            .saveLayoutInfo(size[4], position[4], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.height(sizeDp).then(Modifier.width(sizeDp)).then(
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

        assertEquals(IntSize(sizeIpx, (sizeDp * 2).roundToPx()), size[4].value)
        assertEquals(Offset((sizeIpx * 4).toFloat(), 0f), position[4].value)

        assertEquals(IntSize(sizeIpx, sizeIpx), size[5].value)
        assertEquals(Offset((sizeIpx * 5).toFloat(), 0f), position[5].value)
    }

    @Test
    fun testPreferredSize_withSizeModifiers() = with(density) {
        val sizeDp = 50.toDp()
        val sizeIpx = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(5)
        val size = MutableList(5) { Ref<IntSize>() }
        val position = MutableList(5) { Ref<Offset>() }
        show {
            Box {
                Row {
                    val maxSize = sizeDp * 2
                    Container(
                        Modifier.sizeIn(maxWidth = maxSize, maxHeight = maxSize)
                            .sizeIn(minWidth = sizeDp, minHeight = sizeDp)
                            .saveLayoutInfo(size[0], position[0], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.sizeIn(maxWidth = sizeDp, maxHeight = sizeDp)
                            .sizeIn(minWidth = sizeDp * 2, minHeight = sizeDp)
                            .saveLayoutInfo(size[1], position[1], positionedLatch)
                    ) {
                    }
                    val maxSize1 = sizeDp * 2
                    Container(
                        Modifier.sizeIn(minWidth = sizeDp, minHeight = sizeDp)
                            .sizeIn(maxWidth = maxSize1, maxHeight = maxSize1)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    val minSize = sizeDp * 2
                    Container(
                        Modifier.sizeIn(minWidth = minSize, minHeight = minSize)
                            .sizeIn(maxWidth = sizeDp, maxHeight = sizeDp)
                            .saveLayoutInfo(size[3], position[3], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.size(sizeDp)
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
        val size = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(2)
        val constrainedBoxSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box {
                Container(width = sizeDp, height = sizeDp) {
                    Container(
                        Modifier.width(sizeDp * 2)
                            .height(sizeDp * 3)
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
        val sizeIpx = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(4)
        val size = MutableList(4) { Ref<IntSize>() }
        val position = MutableList(4) { Ref<Offset>() }
        show {
            Box {
                Row {
                    Container(Modifier.widthIn(max = Dp.Infinity)) {
                        Container(
                            width = sizeDp, height = sizeDp,
                            modifier = Modifier.saveLayoutInfo(
                                size[0], position[0],
                                positionedLatch
                            )
                        ) {
                        }
                    }
                    Container(Modifier.heightIn(max = Dp.Infinity)) {
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
                        Modifier.width(sizeDp)
                            .height(sizeDp)
                            .widthIn(max = Dp.Infinity)
                            .heightIn(max = Dp.Infinity)
                            .saveLayoutInfo(size[2], position[2], positionedLatch)
                    ) {
                    }
                    Container(
                        Modifier.sizeIn(
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
                    .requiredSize(sizeDp * 2)
                    .requiredSize(sizeDp)
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
    fun testSize_smallerBoxInLargerBox() = with(density) {
        val sizeIpx = 64
        val sizeDp = sizeIpx.toDp()

        val positionedLatch = CountDownLatch(1)
        val boxSize = Ref<IntSize>()
        val boxPosition = Ref<Offset>()
        show {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart).requiredSize(sizeDp * 2),
                propagateMinConstraints = true
            ) {
                Box(
                    Modifier.requiredSize(sizeDp)
                        .onGloballyPositioned {
                            boxSize.value = it.size
                            boxPosition.value = it.positionInRoot()
                            positionedLatch.countDown()
                        }
                )
            }
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
                    .requiredSize(sizeDp)
                    .requiredSize(sizeDp * 2)
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
    fun testMeasurementConstraints_preferredUnsatisfiable() = with(density) {
        assertConstraints(
            Constraints(20, 40, 15, 35),
            Modifier.width(15.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.height(10.toDp()),
            Constraints(10, 30, 15, 15)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.size(40.toDp()),
            Constraints(30, 30, 35, 35)
        )
        assertConstraints(
            Constraints(20, 30, 15, 35),
            Modifier.widthIn(10.toDp(), 15.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.heightIn(5.toDp(), 10.toDp()),
            Constraints(10, 30, 15, 15)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.sizeIn(40.toDp(), 50.toDp(), 45.toDp(), 55.toDp()),
            Constraints(30, 30, 35, 35)
        )
    }

    @Test
    fun testMeasurementConstraints_compulsorySatisfiable() = with(density) {
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredWidth(20.toDp()),
            Constraints(20, 20, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredHeight(20.toDp()),
            Constraints(10, 30, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredSize(20.toDp()),
            Constraints(20, 20, 20, 20)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredWidthIn(20.toDp(), 25.toDp()),
            Constraints(20, 25, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredHeightIn(20.toDp(), 25.toDp()),
            Constraints(10, 30, 20, 25)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredSizeIn(20.toDp(), 20.toDp(), 25.toDp(), 25.toDp()),
            Constraints(20, 25, 20, 25)
        )
    }

    @Test
    fun testMeasurementConstraints_compulsoryUnsatisfiable() = with(density) {
        assertConstraints(
            Constraints(20, 40, 15, 35),
            Modifier.requiredWidth(15.toDp()),
            Constraints(15, 15, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredHeight(10.toDp()),
            Constraints(10, 30, 10, 10)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredSize(40.toDp()),
            Constraints(40, 40, 40, 40)
        )
        assertConstraints(
            Constraints(20, 30, 15, 35),
            Modifier.requiredWidthIn(10.toDp(), 15.toDp()),
            Constraints(10, 15, 15, 35)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredHeightIn(5.toDp(), 10.toDp()),
            Constraints(10, 30, 5, 10)
        )
        assertConstraints(
            Constraints(10, 30, 15, 35),
            Modifier.requiredSizeIn(40.toDp(), 50.toDp(), 45.toDp(), 55.toDp()),
            Constraints(40, 45, 50, 55)
        )
        // When one dimension is unspecified and the other contradicts the incoming constraint.
        assertConstraints(
            Constraints(10, 10, 10, 10),
            Modifier.requiredSizeIn(20.toDp(), 30.toDp(), Dp.Unspecified, Dp.Unspecified),
            Constraints(20, 20, 30, 30)
        )
        assertConstraints(
            Constraints(40, 40, 40, 40),
            Modifier.requiredSizeIn(Dp.Unspecified, Dp.Unspecified, 20.toDp(), 30.toDp()),
            Constraints(20, 20, 30, 30)
        )
    }

    @Test
    fun testDefaultMinSize() = with(density) {
        val latch = CountDownLatch(3)
        show {
            // Constraints are applied.
            Layout(
                {},
                Modifier.wrapContentSize()
                    .requiredSizeIn(maxWidth = 30.toDp(), maxHeight = 40.toDp())
                    .defaultMinSize(minWidth = 10.toDp(), minHeight = 20.toDp())
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
                Modifier.requiredSizeIn(
                    minWidth = 10.toDp(),
                    minHeight = 20.toDp(),
                    maxWidth = 100.toDp(),
                    maxHeight = 110.toDp()
                ).defaultMinSize(
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
                Modifier.requiredSizeIn(
                    minWidth = 10.toDp(),
                    minHeight = 20.toDp(),
                    maxWidth = 100.toDp(),
                    maxHeight = 110.toDp()
                ).defaultMinSize()
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
    fun testDefaultMinSize_withCoercingMaxConstraints() = with(density) {
        val latch = CountDownLatch(1)
        show {
            Layout(
                {},
                Modifier.wrapContentSize()
                    .requiredSizeIn(maxWidth = 30.toDp(), maxHeight = 40.toDp())
                    .defaultMinSize(minWidth = 70.toDp(), minHeight = 80.toDp())
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
                Container(Modifier.widthIn(min = 10.toDp())) {
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
                Container(Modifier.widthIn(max = 20.toDp())) {
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
                Container(Modifier.heightIn(min = 30.toDp())) {
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
                Container(Modifier.heightIn(max = 40.toDp())) {
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
                Container(Modifier.width(10.toDp())) {
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
                Container(Modifier.height(10.toDp())) {
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
                    Modifier.sizeIn(
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
                Container(Modifier.sizeIn(minWidth = 20.toDp(), minHeight = 30.toDp())) {
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
                Container(Modifier.sizeIn(maxWidth = 40.toDp(), maxHeight = 50.toDp())) {
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
                Container(Modifier.size(40.toDp(), 50.toDp())) {
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
        val parentModifier = Modifier.requiredSize(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 40
        val childHeight = 30
        val childModifier = Modifier.size(childWidth.toDp(), childHeight.toDp())

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
        val parentModifier = Modifier.requiredSize(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 40
        val childHeight = 30
        val childModifier = Modifier.size(childWidth.toDp(), childHeight.toDp())

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
        val parentModifier = Modifier.requiredSize(parentWidth.toDp(), parentHeight.toDp())
        val childWidth = 70
        val childHeight = 50
        val childModifier = Modifier.size(childWidth.toDp(), childHeight.toDp())

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
        val childModifier = Modifier.requiredSizeIn(
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
    fun testDefaultMinSizeModifier_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Container(Modifier.defaultMinSize(40.toDp(), 50.toDp())) {
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
        checkModifier(Modifier.requiredWidth(200.0.dp), "requiredWidth", 200.0.dp, listOf())
        checkModifier(Modifier.requiredHeight(300.0.dp), "requiredHeight", 300.0.dp, listOf())
        checkModifier(Modifier.requiredSize(400.0.dp), "requiredSize", 400.0.dp, listOf())
        checkModifier(
            Modifier.requiredSize(100.0.dp, 200.0.dp), "requiredSize", null,
            listOf(
                ValueElement("width", 100.0.dp),
                ValueElement("height", 200.0.dp)
            )
        )
        checkModifier(
            Modifier.requiredWidthIn(100.0.dp, 200.0.dp), "requiredWidthIn", null,
            listOf(ValueElement("min", 100.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.requiredHeightIn(10.0.dp, 200.0.dp), "requiredHeightIn", null,
            listOf(ValueElement("min", 10.0.dp), ValueElement("max", 200.0.dp))
        )
        checkModifier(
            Modifier.requiredSizeIn(10.dp, 20.dp, 30.dp, 40.dp), "requiredSizeIn", null,
            listOf(
                ValueElement("minWidth", 10.dp), ValueElement("minHeight", 20.dp),
                ValueElement("maxWidth", 30.dp), ValueElement("maxHeight", 40.dp)
            )
        )
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
            Modifier.defaultMinSize(10.0.dp, 20.0.dp),
            "defaultMinSize", null,
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
        // Capture constraints and assert on test thread
        var actualConstraints: Constraints? = null
        // Clear contents before each test so that we don't recompose the BoxWithConstraints call;
        // doing so would recompose the old subcomposition with old constraints in the presence of
        // new content before the measurement performs explicit composition the new constraints.
        show({})
        show {
            Layout({
                BoxWithConstraints(modifier) {
                    actualConstraints = constraints
                    latch.countDown()
                }
            }) { measurables, _ ->
                measurables[0].measure(incomingConstraints)
                layout(0, 0) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(expectedConstraints, actualConstraints)
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
    @Test
    fun test2DWrapContentSize() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()

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
                        .size(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
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
        val size = sizeDp.roundToPx()

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
                        .width(sizeDp)
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(root.width, root.height), alignSize.value)
        assertEquals(Offset(0f, 0f), alignPosition.value)
        assertEquals(IntSize(size, root.height), childSize.value)
        assertEquals(Offset(root.width - size.toFloat(), 0f), childPosition.value)
    }

    @Test
    fun testWrapContentSize_rtl() = with(density) {
        val sizeDp = 200.toDp()
        val size = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(3)
        val childSize = Array(3) { Ref<IntSize>() }
        val childPosition = Array(3) { Ref<Offset>() }
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                        Box(
                            Modifier.size(sizeDp)
                                .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                        ) {
                        }
                    }
                    Box(Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)) {
                        Box(
                            Modifier.size(sizeDp)
                                .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                        ) {
                        }
                    }
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                        Box(
                            Modifier.size(sizeDp)
                                .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
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
                            .size(contentSize)
                    ) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(contentSize.roundToPx(), contentSize.roundToPx()), size.value)
    }

    @Test
    fun testWrapContentSize_wrapsContent_whenMeasuredWithInfiniteConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Layout(
                content = {
                    Container(
                        Modifier.saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.wrapContentSize(Alignment.BottomEnd)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                },
                measurePolicy = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
            )
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(size, size), alignSize.value)
        assertEquals(Offset(0f, 0f), alignPosition.value)
        assertEquals(IntSize(size, size), childSize.value)
        assertEquals(Offset(0f, 0f), childPosition.value)
    }

    @Test
    fun testWrapContentSize_respectsMinConstraints() = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.roundToPx()

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
                    content = {
                        Container(
                            Modifier.wrapContentSize(Alignment.Center)
                                .size(sizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    },
                    measurePolicy = { measurables, incomingConstraints ->
                        val measurable = measurables.first()
                        val constraints = incomingConstraints.constrain(
                            Constraints(
                                minWidth = doubleSizeDp.roundToPx(),
                                minHeight = doubleSizeDp.roundToPx()
                            )
                        )
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(IntOffset.Zero)
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
                Modifier.requiredSize(outerSize.toDp())
                    .onGloballyPositioned {
                        assertEquals(outerSize, it.size.width.toFloat())
                        positionedLatch.countDown()
                    }
            ) {
                Box(
                    Modifier.wrapContentSize(Alignment.BottomEnd, unbounded = true)
                        .requiredSize(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(
                                Offset(outerSize - innerSize, outerSize - innerSize),
                                it.positionInParent()
                            )
                            positionedLatch.countDown()
                        }
                )
                Box(
                    Modifier.wrapContentWidth(Alignment.End, unbounded = true)
                        .requiredSize(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(outerSize - innerSize, it.positionInParent().x)
                            positionedLatch.countDown()
                        }
                )
                Box(
                    Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true)
                        .requiredSize(innerSize.toDp())
                        .onGloballyPositioned {
                            assertEquals(outerSize - innerSize, it.positionInParent().y)
                            positionedLatch.countDown()
                        }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
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
            assertEquals(25.dp.roundToPx() * 2, minIntrinsicWidth(25.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), minIntrinsicWidth(Constraints.Infinity))

            // Min height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(
                (50.dp.roundToPx() / 2f).roundToInt(),
                minIntrinsicHeight(50.dp.roundToPx())
            )
            assertEquals(0.dp.roundToPx(), minIntrinsicHeight(Constraints.Infinity))

            // Max width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.roundToPx() * 2, maxIntrinsicWidth(25.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), maxIntrinsicWidth(Constraints.Infinity))

            // Max height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(
                (50.dp.roundToPx() / 2f).roundToInt(),
                maxIntrinsicHeight(50.dp.roundToPx())
            )
            assertEquals(0.dp.roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
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
            assertEquals(25.dp.roundToPx() * 2, minIntrinsicWidth(25.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), minIntrinsicWidth(Constraints.Infinity))

            // Min height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(
                (50.dp.roundToPx() / 2f).roundToInt(),
                minIntrinsicHeight(50.dp.roundToPx())
            )
            assertEquals(0.dp.roundToPx(), minIntrinsicHeight(Constraints.Infinity))

            // Max width.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(25.dp.roundToPx() * 2, maxIntrinsicWidth(25.dp.roundToPx()))
            assertEquals(0.dp.roundToPx(), maxIntrinsicWidth(Constraints.Infinity))

            // Max height.
            assertEquals(0, minIntrinsicWidth(0))
            assertEquals(
                (50.dp.roundToPx() / 2f).roundToInt(),
                maxIntrinsicHeight(50.dp.roundToPx())
            )
            assertEquals(0.dp.roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
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
        val childSizeIpx = childSizeDp.roundToPx()

        val positionedLatch = CountDownLatch(2)
        val alignSize = Ref<IntSize>()
        val alignPosition = Ref<Offset>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Layout(
                content = {
                    Container(
                        Modifier.size(parentSize)
                            .saveLayoutInfo(alignSize, alignPosition, positionedLatch)
                    ) {
                        Container(
                            Modifier.fillMaxSize()
                                .wrapContentSize(Alignment.BottomEnd)
                                .size(childSizeDp)
                                .saveLayoutInfo(childSize, childPosition, positionedLatch)
                        ) {
                        }
                    }
                },
                measurePolicy = { measurables, constraints ->
                    val placeable = measurables.first().measure(Constraints())
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, 0)
                    }
                }
            )
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        val root = findComposeView()
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

    @Test
    @FlakyTest(bugId = 183713100)
    fun testModifiers_doNotCauseUnnecessaryRemeasure() {
        var first by mutableStateOf(true)
        var totalMeasures = 0
        @Composable fun CountMeasures(modifier: Modifier) {
            Layout(
                content = {},
                modifier = modifier,
                measurePolicy = { _, _ ->
                    ++totalMeasures
                    layout(0, 0) {}
                }
            )
        }
        show {
            Box {
                if (first) Box {} else Row {}
                CountMeasures(Modifier.size(10.dp))
                CountMeasures(Modifier.requiredSize(10.dp))
                CountMeasures(Modifier.wrapContentSize(Alignment.BottomEnd))
                CountMeasures(Modifier.fillMaxSize(0.8f))
                CountMeasures(Modifier.defaultMinSize(10.dp, 20.dp))
            }
        }

        val root = findComposeView()
        waitForDraw(root)

        activityTestRule.runOnUiThread {
            assertEquals(5, totalMeasures)
            first = false
        }

        activityTestRule.runOnUiThread {
            assertEquals(5, totalMeasures)
        }
    }

    @Test
    fun testModifiers_equals() {
        assertEquals(Modifier.size(10.dp, 20.dp), Modifier.size(10.dp, 20.dp))
        assertEquals(Modifier.requiredSize(10.dp, 20.dp), Modifier.requiredSize(10.dp, 20.dp))
        assertEquals(
            Modifier.wrapContentSize(Alignment.BottomEnd),
            Modifier.wrapContentSize(Alignment.BottomEnd)
        )
        assertEquals(Modifier.fillMaxSize(0.8f), Modifier.fillMaxSize(0.8f))
        assertEquals(Modifier.defaultMinSize(10.dp, 20.dp), Modifier.defaultMinSize(10.dp, 20.dp))

        assertNotEquals(Modifier.size(10.dp, 20.dp), Modifier.size(20.dp, 10.dp))
        assertNotEquals(Modifier.requiredSize(10.dp, 20.dp), Modifier.requiredSize(20.dp, 10.dp))
        assertNotEquals(
            Modifier.wrapContentSize(Alignment.BottomEnd),
            Modifier.wrapContentSize(Alignment.BottomCenter)
        )
        assertNotEquals(Modifier.fillMaxSize(0.8f), Modifier.fillMaxSize())
        assertNotEquals(
            Modifier.defaultMinSize(10.dp, 20.dp),
            Modifier.defaultMinSize(20.dp, 10.dp)
        )
    }

    @Test
    fun testIntrinsicMeasurements_notQueriedWhenConstraintsAreFixed() {
        @Composable fun ErrorIntrinsicsLayout(modifier: Modifier) {
            Layout(
                {},
                modifier,
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ): MeasureResult {
                        return layout(0, 0) {}
                    }

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = error("Error intrinsic")

                    override fun IntrinsicMeasureScope.minIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = error("Error intrinsic")

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = error("Error intrinsic")

                    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = error("Error intrinsic")
                }
            )
        }

        show {
            Box(Modifier.width(IntrinsicSize.Min)) { ErrorIntrinsicsLayout(Modifier.width(1.dp)) }
            Box(Modifier.width(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.width(IntrinsicSize.Max)) { ErrorIntrinsicsLayout(Modifier.width(1.dp)) }
            Box(Modifier.width(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.requiredWidth(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.requiredWidth(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.requiredWidth(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.requiredWidth(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.width(1.dp))
            }
            Box(Modifier.height(IntrinsicSize.Min)) { ErrorIntrinsicsLayout(Modifier.height(1.dp)) }
            Box(Modifier.height(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
            Box(Modifier.height(IntrinsicSize.Max)) { ErrorIntrinsicsLayout(Modifier.height(1.dp)) }
            Box(Modifier.height(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
            Box(Modifier.requiredHeight(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
            Box(Modifier.requiredHeight(IntrinsicSize.Min)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
            Box(Modifier.requiredHeight(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
            Box(Modifier.requiredHeight(IntrinsicSize.Max)) {
                ErrorIntrinsicsLayout(Modifier.height(1.dp))
            }
        }
        // The test tests that the measure pass should not crash.
        val root = findComposeView()
        waitForDraw(root)
    }

    @Test
    fun sizeModifiers_doNotCauseCrashesWhenCreatingConstraints() {
        show {
            Box(Modifier.sizeIn(minWidth = -1.dp))
            Box(Modifier.sizeIn(minWidth = 10.dp, maxWidth = 5.dp))
            Box(Modifier.sizeIn(minHeight = -1.dp))
            Box(Modifier.sizeIn(minHeight = 10.dp, maxHeight = 5.dp))
            Box(
                Modifier.sizeIn(
                    minWidth = Dp.Infinity,
                    maxWidth = Dp.Infinity,
                    minHeight = Dp.Infinity,
                    maxHeight = Dp.Infinity
                )
            )
            Box(Modifier.defaultMinSize(minWidth = -1.dp, minHeight = -1.dp))
        }
        val root = findComposeView()
        waitForDraw(root)
    }
}