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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class RowColumnTest : LayoutTest() {
    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // region Size and position tests for Row and Column
    @Test
    fun testRow() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntSize(-1, -1), IntSize(-1, -1))
        val childPosition = arrayOf(Offset(-1f, -1f), Offset(-1f, -1f))
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(
            IntSize((sizeDp.toPx() * 2).roundToInt(), (sizeDp.toPx() * 2).roundToInt()),
            childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1])
    }

    @Test
    fun testRow_withChildrenWithWeight() = with(density) {
        val width = 50.toDp()
        val height = 80.toDp()
        val childrenHeight = height.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        Modifier.weight(1f)
                            .onGloballyPositioned { coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f)
                            .onGloballyPositioned { coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(
            IntSize((rootWidth / 3f).roundToInt(), childrenHeight),
            childSize[0]
        )
        assertEquals(
            IntSize((rootWidth * 2f / 3f).roundToInt(), childrenHeight),
            childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset((rootWidth / 3f).roundToInt().toFloat(), 0f), childPosition[1])
    }

    @Test
    fun testRow_withChildrenWithWeightNonFilling() = with(density) {
        val width = 50.toDp()
        val childrenWidth = width.roundToPx()
        val height = 80.toDp()
        val childrenHeight = height.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Row {
                    Container(
                        Modifier.weight(1f, fill = false)
                            .onGloballyPositioned { coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f, fill = false)
                            .onGloballyPositioned { coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height * 2
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(IntSize(childrenWidth, childrenHeight * 2), childSize[1])
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(childrenWidth.toFloat(), 0f), childPosition[1])
    }

    @Test
    fun testColumn() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntSize(-1, -1), IntSize(-1, -1))
        val childPosition = arrayOf(Offset(-1f, -1f), Offset(-1f, -1f))
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(
            IntSize((sizeDp.toPx() * 2).roundToInt(), (sizeDp.toPx() * 2).roundToInt()),
            childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(0f, size.toFloat()), childPosition[1])
    }

    @Test
    fun testColumn_withChildrenWithWeight() = with(density) {
        val width = 80.toDp()
        val childrenWidth = width.roundToPx()
        val height = 50.toDp()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        Modifier.weight(1f)
                            .onGloballyPositioned { coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }

                    Container(
                        Modifier.weight(2f)
                            .onGloballyPositioned { coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootHeight = root.height

        assertEquals(
            IntSize(childrenWidth, (rootHeight / 3f).roundToInt()), childSize[0]
        )
        assertEquals(
            IntSize(childrenWidth, (rootHeight * 2f / 3f).roundToInt()), childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(0f, (rootHeight / 3f).roundToInt().toFloat()), childPosition[1])
    }

    @Test
    fun testColumn_withChildrenWithWeightNonFilling() = with(density) {
        val width = 80.toDp()
        val childrenWidth = width.roundToPx()
        val height = 50.toDp()
        val childrenHeight = height.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Container(alignment = Alignment.TopStart) {
                Column {
                    Container(
                        Modifier.weight(1f, fill = false)
                            .onGloballyPositioned { coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                    Container(
                        Modifier.weight(2f, fill = false)
                            .onGloballyPositioned { coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            },
                        width = width,
                        height = height
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(childrenWidth, childrenHeight), childSize[0])
        assertEquals(
            IntSize(childrenWidth, childrenHeight), childSize[1]
        )
        assertEquals(Offset(0.0f, 0.0f), childPosition[0])
        assertEquals(Offset(0.0f, childrenHeight.toFloat()), childPosition[1])
    }

    @Test
    fun testRow_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = with(density) {
        val expectedRowWidth = 11f
        val leftPadding = 1f
        var rowWidth = 0f
        val width = Array(2) { 0f }
        val x = Array(2) { 0f }
        val latch = CountDownLatch(2)
        show {
            Row(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(start = leftPadding.toDp())
                    .widthIn(max = expectedRowWidth.toDp())
                    .onGloballyPositioned { coordinates ->
                        rowWidth = coordinates.size.width.toFloat()
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            width[0] = coordinates.size.width.toFloat()
                            x[0] = coordinates.positionInRoot().x
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            width[1] = coordinates.size.width.toFloat()
                            x[1] = coordinates.positionInRoot().x
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedRowWidth, rowWidth)
        assertEquals(leftPadding, x[0])
        assertEquals(leftPadding + width[0], x[1])
        assertEquals(rowWidth, width[0] + width[1])
    }

    @Test
    fun testRow_isNotLargerThanItsChildren_becauseOfRoundings() = with(density) {
        val expectedRowWidth = 8f
        val leftPadding = 1f
        var rowWidth = 0f
        val width = Array(3) { 0f }
        val x = Array(3) { 0f }
        val latch = CountDownLatch(3)
        show {
            Row(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(start = leftPadding.toDp())
                    .widthIn(max = expectedRowWidth.toDp())
                    .onGloballyPositioned { coordinates ->
                        rowWidth = coordinates.size.width.toFloat()
                    }
            ) {
                Container(
                    Modifier.weight(2f)
                        .onGloballyPositioned { coordinates ->
                            width[0] = coordinates.size.width.toFloat()
                            x[0] = coordinates.positionInRoot().x
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(2f)
                        .onGloballyPositioned { coordinates ->
                            width[1] = coordinates.size.width.toFloat()
                            x[1] = coordinates.positionInRoot().x
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(3f)
                        .onGloballyPositioned { coordinates ->
                            width[2] = coordinates.size.width.toFloat()
                            x[2] = coordinates.positionInRoot().x
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedRowWidth, rowWidth)
        assertEquals(leftPadding, x[0])
        assertEquals(leftPadding + width[0], x[1])
        assertEquals(leftPadding + width[0] + width[1], x[2])
        assertEquals(rowWidth, width[0] + width[1] + width[2])
    }

    @Test
    fun testColumn_isNotLargetThanItsChildren_becauseOfRoundings() = with(density) {
        val expectedColumnHeight = 8f
        val topPadding = 1f
        var columnHeight = 0f
        val height = Array(3) { 0f }
        val y = Array(3) { 0f }
        val latch = CountDownLatch(3)
        show {
            Column(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(top = topPadding.toDp())
                    .heightIn(max = expectedColumnHeight.toDp())
                    .onGloballyPositioned { coordinates ->
                        columnHeight = coordinates.size.height.toFloat()
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            height[0] = coordinates.size.height.toFloat()
                            y[0] = coordinates.positionInRoot().y
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            height[1] = coordinates.size.height.toFloat()
                            y[1] = coordinates.positionInRoot().y
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            height[2] = coordinates.size.height.toFloat()
                            y[2] = coordinates.positionInRoot().y
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedColumnHeight, columnHeight)
        assertEquals(topPadding, y[0])
        assertEquals(topPadding + height[0], y[1])
        assertEquals(topPadding + height[0] + height[1], y[2])
        assertEquals(columnHeight, height[0] + height[1] + height[2])
    }

    @Test
    fun testColumn_doesNotPlaceChildrenOutOfBounds_becauseOfRoundings() = with(density) {
        val expectedColumnHeight = 11f
        val topPadding = 1f
        var columnHeight = 0f
        val height = Array(2) { 0f }
        val y = Array(2) { 0f }
        val latch = CountDownLatch(2)
        show {
            Column(
                Modifier.wrapContentSize(Alignment.TopStart)
                    .padding(top = topPadding.toDp())
                    .heightIn(max = expectedColumnHeight.toDp())
                    .onGloballyPositioned { coordinates ->
                        columnHeight = coordinates.size.height.toFloat()
                    }
            ) {
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            height[0] = coordinates.size.height.toFloat()
                            y[0] = coordinates.positionInRoot().y
                            latch.countDown()
                        }
                ) {
                }
                Container(
                    Modifier.weight(1f)
                        .onGloballyPositioned { coordinates ->
                            height[1] = coordinates.size.height.toFloat()
                            y[1] = coordinates.positionInRoot().y
                            latch.countDown()
                        }
                ) {
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals(expectedColumnHeight, columnHeight)
        assertEquals(topPadding, y[0])
        assertEquals(topPadding + height[0], y[1])
        assertEquals(columnHeight, height[0] + height[1])
    }

    // endregion

    // region Cross axis alignment tests in Row
    @Test
    fun testRow_withStretchCrossAxisAlignment() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntSize(-1, -1), IntSize(-1, -1))
        val childPosition = arrayOf(Offset(-1f, -1f), Offset(-1f, -1f))
        show {
            Row {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(
                    width = (sizeDp * 2),
                    height = (sizeDp * 2),
                    modifier = Modifier.fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(size, root.height), childSize[0])
        assertEquals(
            IntSize((sizeDp.toPx() * 2).roundToInt(), root.height),
            childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1])
    }

    @Test
    fun testRow_withGravityModifier_andGravityParameter() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntSize>(3)
        val childPosition = arrayOfNulls<Offset>(3)
        show {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Top)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        childSize[1] = coordinates.size
                        childPosition[1] = coordinates.positionInRoot()
                        drawLatch.countDown()
                    }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Bottom)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootHeight = root.height

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(
            Offset(
                size.toFloat(),
                ((rootHeight - size.toFloat()) / 2f).roundToInt().toFloat()
            ),
            childPosition[1]
        )

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset(
                (size.toFloat() * 2),
                (rootHeight - size.toFloat())
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withGravityModifier() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntSize>(3)
        val childPosition = arrayOfNulls<Offset>(3)
        show {
            Row(Modifier.fillMaxHeight()) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Top)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Bottom)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootHeight = root.height

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(
            Offset(
                size.toFloat(),
                ((rootHeight - size.toFloat()) / 2f).roundToInt().toFloat()
            ),
            childPosition[1]
        )

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset((size.toFloat() * 2), (rootHeight - size.toFloat())),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withAlignByModifier() = with(density) {
        val baseline1Dp = 30.toDp()
        val baseline1 = baseline1Dp.roundToPx()
        val baseline2Dp = 25.toDp()
        val baseline2 = baseline2Dp.roundToPx()
        val baseline3Dp = 20.toDp()
        val baseline3 = baseline3Dp.roundToPx()
        val sizeDp = 40.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(5)
        val childSize = arrayOfNulls<IntSize>(5)
        val childPosition = arrayOfNulls<Offset>(5)
        show {
            Row(Modifier.fillMaxHeight()) {
                BaselineTestLayout(
                    baseline = baseline1Dp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy(TestHorizontalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy { it.measuredHeight / 2 }
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                BaselineTestLayout(
                    baseline = baseline2Dp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy(TestHorizontalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy { it.measuredHeight * 3 / 4 }
                        .onGloballyPositioned { coordinates ->
                            childSize[3] = coordinates.size
                            childPosition[3] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                BaselineTestLayout(
                    baseline = baseline3Dp,
                    width = sizeDp,
                    height = sizeDp,
                    horizontalLine = FirstBaseline,
                    modifier = Modifier.alignByBaseline()
                        .onGloballyPositioned { coordinates ->
                            childSize[4] = coordinates.size
                            childPosition[4] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(
            Offset(
                size.toFloat(),
                (baseline1.toFloat() - (size.toFloat() / 2).roundToInt())
            ),
            childPosition[1]
        )

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset((size.toFloat() * 2), (baseline1 - baseline2).toFloat()),
            childPosition[2]
        )

        assertEquals(IntSize(size, size), childSize[3])
        assertEquals(
            Offset((size.toFloat() * 3), 0f),
            childPosition[3]
        )

        assertEquals(IntSize(size, size), childSize[4])
        assertEquals(
            Offset((size.toFloat() * 4), (baseline1 - baseline3).toFloat()),
            childPosition[4]
        )
    }

    @Test
    fun testRow_withAlignByModifier_andWeight() = with(density) {
        val baselineDp = 30.toDp()
        val baseline = baselineDp.roundToPx()
        val sizeDp = 40.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Row(Modifier.fillMaxHeight()) {
                BaselineTestLayout(
                    baseline = baselineDp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy(TestHorizontalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    height = sizeDp,
                    modifier = Modifier.alignBy { it.measuredHeight / 2 }
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(size, childSize[1]!!.height)
        assertEquals(
            Offset(size.toFloat(), (baseline - size / 2).toFloat()),
            childPosition[1]
        )
    }
    // endregion

    // region Cross axis alignment tests in Column
    @Test
    fun testColumn_withStretchCrossAxisAlignment() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(IntSize(-1, -1), IntSize(-1, -1))
        val childPosition = arrayOf(Offset(-1f, -1f), Offset(-1f, -1f))
        show {
            Column {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }

                Container(
                    width = (sizeDp * 2),
                    height = (sizeDp * 2),
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(root.width, size), childSize[0])
        assertEquals(
            IntSize(root.width, (sizeDp * 2).roundToPx()),
            childSize[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(0f, size.toFloat()), childPosition[1])
    }

    @Test
    fun testColumn_withGravityModifier() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntSize>(3)
        val childPosition = arrayOfNulls<Offset>(3)
        show {
            Column(Modifier.fillMaxWidth()) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Start)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.End)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(
            Offset(
                ((rootWidth - size.toFloat()) / 2).roundToInt().toFloat(),
                size.toFloat()
            ),
            childPosition[1]
        )

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset((rootWidth - size.toFloat()), size.toFloat() * 2),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withGravityModifier_andGravityParameter() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(3)
        val childSize = arrayOfNulls<IntSize>(3)
        val childPosition = arrayOfNulls<Offset>(3)
        show {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.Start)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        childSize[1] = coordinates.size
                        childPosition[1] = coordinates.positionInRoot()
                        drawLatch.countDown()
                    }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.align(Alignment.End)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(
            Offset(
                ((rootWidth - size.toFloat()) / 2).roundToInt().toFloat(),
                size.toFloat()
            ),
            childPosition[1]
        )

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset((rootWidth - size.toFloat()), size.toFloat() * 2),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withAlignByModifier() = with(density) {
        val sizeDp = 40.toDp()
        val size = sizeDp.roundToPx()
        val firstBaseline1Dp = 20.toDp()
        val firstBaseline2Dp = 30.toDp()

        val drawLatch = CountDownLatch(4)
        val childSize = arrayOfNulls<IntSize>(4)
        val childPosition = arrayOfNulls<Offset>(4)
        show {
            Column(Modifier.fillMaxWidth()) {
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy { it.measuredWidth }
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy { 0 }
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                BaselineTestLayout(
                    width = sizeDp,
                    height = sizeDp,
                    baseline = firstBaseline1Dp,
                    modifier = Modifier.alignBy(TestVerticalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                BaselineTestLayout(
                    width = sizeDp,
                    height = sizeDp,
                    baseline = firstBaseline2Dp,
                    modifier = Modifier.alignBy(TestVerticalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[3] = coordinates.size
                            childPosition[3] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(IntSize(size, size), childSize[1])
        assertEquals(Offset(size.toFloat(), size.toFloat()), childPosition[1])

        assertEquals(IntSize(size, size), childSize[2])
        assertEquals(
            Offset(
                (size - firstBaseline1Dp.roundToPx()).toFloat(),
                size.toFloat() * 2
            ),
            childPosition[2]
        )

        assertEquals(IntSize(size, size), childSize[3])
        assertEquals(
            Offset(
                (size - firstBaseline2Dp.roundToPx()).toFloat(),
                size.toFloat() * 3
            ),
            childPosition[3]
        )
    }

    @Test
    fun testColumn_withAlignByModifier_andWeight() = with(density) {
        val baselineDp = 30.toDp()
        val baseline = baselineDp.roundToPx()
        val sizeDp = 40.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOfNulls<IntSize>(2)
        val childPosition = arrayOfNulls<Offset>(2)
        show {
            Column(Modifier.fillMaxWidth()) {
                BaselineTestLayout(
                    baseline = baselineDp,
                    width = sizeDp,
                    height = sizeDp,
                    modifier = Modifier.alignBy(TestVerticalLine)
                        .onGloballyPositioned { coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
                Container(
                    width = sizeDp,
                    modifier = Modifier.alignBy { it.measuredWidth / 2 }
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(size, size), childSize[0])
        assertEquals(Offset(0f, 0f), childPosition[0])

        assertEquals(size, childSize[1]!!.width)
        assertEquals(
            Offset((baseline - (size / 2)).toFloat(), size.toFloat()),
            childPosition[1]
        )
    }
    // endregion

    // region Size tests in Row
    @Test
    fun testRow_expandedWidth_withExpandedModifier() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.width,
            rowSize.width
        )
    }

    @Test
    fun testRow_wrappedWidth_withNoWeightChildren() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                Row(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 3).roundToPx(),
            rowSize.width
        )
    }

    @Test
    fun testRow_expandedWidth_withWeightChildren() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                Row(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Container(
                        Modifier.weight(1f),
                        width = sizeDp,
                        height = sizeDp,
                        content = {}
                    )
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.width,
            rowSize.width
        )
    }

    @Test
    fun testRow_withMaxCrossAxisSize() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                Row(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.height,
            rowSize.height
        )
    }

    @Test
    fun testRow_withMinCrossAxisSize() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                Row(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        rowSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 2).roundToPx(),
            rowSize.height
        )
    }

    @Test
    fun testRow_withExpandedModifier_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val rowWidthDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row(
                        Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.width, rowWidthDp.roundToPx()),
            rowSize.width
        )
    }

    @Test
    fun testRow_withChildrenWithWeight_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val rowWidthDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = rowWidthDp)) {
                    Row(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Container(
                            Modifier.weight(1f),
                            width = sizeDp,
                            height = sizeDp,
                            content = {}
                        )
                        Container(
                            width = sizeDp * 2,
                            height = sizeDp * 2,
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.width, rowWidthDp.roundToPx()),
            rowSize.width
        )
    }

    @Test
    fun testRow_withNoWeightChildren_respectsMinWidthConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val rowWidthDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = rowWidthDp)) {
                    Row(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            rowWidthDp.roundToPx(),
            rowSize.width
        )
    }

    @Test
    fun testRow_withMaxCrossAxisSize_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val rowHeightDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = rowHeightDp)) {
                    Row(
                        Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.height, rowHeightDp.roundToPx()),
            rowSize.height
        )
    }

    @Test
    fun testRow_withMinCrossAxisSize_respectsMinHeightConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val rowHeightDp = 150.toDp()

        val drawLatch = CountDownLatch(1)
        var rowSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = rowHeightDp)) {
                    Row(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            rowHeightDp.roundToPx(),
            rowSize.height
        )
    }

    @Test
    @Ignore(
        "Wrap is not supported when there are children with weight. " +
            "Should use maxWidth(.Infinity) modifier when it is available"
    )
    fun testRow_withMinMainAxisSize() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()
        val rowWidthDp = 250.toDp()
        val rowWidth = rowWidthDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        var rowSize: IntSize = IntSize.Zero
        var expandedChildSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = rowWidthDp)) {
                    // TODO: add maxWidth(Constraints.Infinity) modifier
                    Row(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            rowSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Container(
                            modifier = Modifier.weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    expandedChildSize = coordinates.size
                                    drawLatch.countDown()
                                },
                            width = sizeDp,
                            height = sizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            IntSize(rowWidth, size),
            rowSize
        )
        assertEquals(
            IntSize(rowWidth, size),
            expandedChildSize
        )
    }

    @Test
    fun testRow_measuresChildrenCorrectly_whenMeasuredWithInfiniteWidth() = with(density) {
        val rowMinWidth = 100.toDp()
        val noWeightChildWidth = 30.toDp()
        val latch = CountDownLatch(1)
        show {
            WithInfiniteConstraints {
                ConstrainedBox(DpConstraints(minWidth = rowMinWidth)) {
                    Row {
                        BoxWithConstraints {
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.roundToPx(), 0, mapOf())
                        }
                        BoxWithConstraints {
                            assertEquals(Constraints(), constraints)
                            FixedSizeLayout(noWeightChildWidth.roundToPx(), 0, mapOf())
                        }
                        Layout({}, Modifier.weight(1f)) { _, constraints ->
                            assertEquals(
                                rowMinWidth.roundToPx() - noWeightChildWidth.roundToPx() * 2,
                                constraints.minWidth
                            )
                            assertEquals(
                                rowMinWidth.roundToPx() - noWeightChildWidth.roundToPx() * 2,
                                constraints.maxWidth
                            )
                            latch.countDown()
                            layout(0, 0) { }
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_measuresNoWeightChildrenCorrectly() = with(density) {
        val availableWidth = 100.toDp()
        val childWidth = 50.toDp()
        val availableHeight = 200.toDp()
        val childHeight = 100.toDp()
        val latch = CountDownLatch(1)
        show {
            Box {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Row {
                        BoxWithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.roundToPx(),
                                    maxHeight = availableHeight.roundToPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(
                                childWidth.roundToPx(),
                                childHeight.roundToPx(),
                                mapOf()
                            )
                        }
                        BoxWithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.roundToPx() - childWidth.roundToPx(),
                                    maxHeight = availableHeight.roundToPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(
                                childWidth.roundToPx(),
                                childHeight.roundToPx(),
                                mapOf()
                            )
                            latch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_doesNotExpand_whenWeightChildrenDoNotFill() = with(density) {
        val size = 10
        var rowWidth = 0
        val latch = CountDownLatch(1)
        show {
            Row(
                Modifier.onGloballyPositioned {
                    rowWidth = it.size.width
                    latch.countDown()
                }
            ) {
                Box(Modifier.weight(1f, false).size(size.toDp()))
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(size, rowWidth)
    }

    @Test
    fun testRow_includesSpacing_withWeightChildren() = with(density) {
        val rowWidth = 40
        val space = 8
        val latch = CountDownLatch(2)
        show {
            Row(
                modifier = Modifier.widthIn(max = rowWidth.toDp()),
                horizontalArrangement = Arrangement.spacedBy(space.toDp())
            ) {
                Box(
                    Modifier.weight(1f).onGloballyPositioned {
                        assertEquals((rowWidth - space) / 2, it.size.width)
                        assertEquals(0, it.positionInRoot().x.toInt())
                        latch.countDown()
                    }
                )
                Box(
                    Modifier.weight(1f).onGloballyPositioned {
                        assertEquals((rowWidth - space) / 2, it.size.width)
                        assertEquals((rowWidth - space) / 2 + space, it.positionInRoot().x.toInt())
                        latch.countDown()
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region Size tests in Column
    @Test
    fun testColumn_expandedHeight_withExpandedModifier() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.height,
            columnSize.height
        )
    }

    @Test
    fun testColumn_wrappedHeight_withNoChildrenWithWeight() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                Column(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 3).roundToPx(),
            columnSize.height
        )
    }

    @Test
    fun testColumn_expandedHeight_withWeightChildren() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                Column(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Container(
                        Modifier.weight(1f),
                        width = sizeDp,
                        height = sizeDp,
                        content = {}
                    )
                    Container(
                        width = (sizeDp * 2),
                        height = (sizeDp * 2),
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.height,
            columnSize.height
        )
    }

    @Test
    fun testColumn_withMaxCrossAxisSize() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                Column(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            root.width,
            columnSize.width
        )
    }

    @Test
    fun testColumn_withMinCrossAxisSize() = with(density) {
        val sizeDp = 50.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                Column(
                    Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                        columnSize = coordinates.size
                        drawLatch.countDown()
                    }
                ) {
                    Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                    Spacer(Modifier.size(width = (sizeDp * 2), height = (sizeDp * 2)))
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            (sizeDp * 2).roundToPx(),
            columnSize.width
        )
    }

    @Test
    fun testColumn_withExpandedModifier_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val columnHeightDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column(
                        Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.height, columnHeightDp.roundToPx()),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withWeightChildren_respectsMaxHeightConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val columnHeightDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxHeight = columnHeightDp)) {
                    Column(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Container(
                            Modifier.weight(1f),
                            width = sizeDp,
                            height = sizeDp,
                            content = {}
                        )
                        Container(
                            width = sizeDp * 2,
                            height = sizeDp * 2,
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.height, columnHeightDp.roundToPx()),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withChildren_respectsMinHeightConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val columnHeightDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = columnHeightDp)) {
                    Column(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            columnHeightDp.roundToPx(),
            columnSize.height
        )
    }

    @Test
    fun testColumn_withMaxCrossAxisSize_respectsMaxWidthConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val columnWidthDp = 250.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(maxWidth = columnWidthDp)) {
                    Column(
                        Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            min(root.width, columnWidthDp.roundToPx()),
            columnSize.width
        )
    }

    @Test
    fun testColumn_withMinCrossAxisSize_respectsMinWidthConstraint() = with(density) {
        val sizeDp = 50.toDp()
        val columnWidthDp = 150.toDp()

        val drawLatch = CountDownLatch(1)
        var columnSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minWidth = columnWidthDp)) {
                    Column(
                        Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                            columnSize = coordinates.size
                            drawLatch.countDown()
                        }
                    ) {
                        Spacer(Modifier.size(width = sizeDp, height = sizeDp))
                        Spacer(Modifier.size(width = sizeDp * 2, height = sizeDp * 2))
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            columnWidthDp.roundToPx(),
            columnSize.width
        )
    }

    @Test
    @Ignore(
        "Wrap is not supported when there are weight children. " +
            "Should use maxHeight(Constraints.Infinity) modifier when it is available"
    )
    fun testColumn_withMinMainAxisSize() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()
        val columnHeightDp = 250.toDp()
        val columnHeight = columnHeightDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        var columnSize: IntSize = IntSize.Zero
        var expandedChildSize: IntSize = IntSize.Zero
        show {
            Center {
                ConstrainedBox(constraints = DpConstraints(minHeight = columnHeightDp)) {
                    // TODO: add maxHeight(Constraints.Infinity) modifier
                    Column(
                        Modifier.heightIn(max = Dp.Infinity)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                columnSize = coordinates.size
                                drawLatch.countDown()
                            }
                    ) {
                        Container(
                            Modifier.weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    expandedChildSize = coordinates.size
                                    drawLatch.countDown()
                                },
                            width = sizeDp,
                            height = sizeDp
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            IntSize(size, columnHeight),
            columnSize
        )
        assertEquals(
            IntSize(size, columnHeight),
            expandedChildSize
        )
    }

    @Test
    fun testColumn_measuresChildrenCorrectly_whenMeasuredWithInfiniteHeight() =
        with(density) {
            val columnMinHeight = 100.toDp()
            val noWeightChildHeight = 30.toDp()
            val latch = CountDownLatch(1)
            show {
                WithInfiniteConstraints {
                    ConstrainedBox(DpConstraints(minHeight = columnMinHeight)) {
                        Column {
                            BoxWithConstraints {
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(
                                    0,
                                    noWeightChildHeight.roundToPx(),
                                    mapOf()
                                )
                            }
                            BoxWithConstraints {
                                assertEquals(Constraints(), constraints)
                                FixedSizeLayout(
                                    0,
                                    noWeightChildHeight.roundToPx(),
                                    mapOf()
                                )
                            }
                            Layout({}, Modifier.weight(1f)) { _, constraints ->
                                assertEquals(
                                    columnMinHeight.roundToPx() -
                                        noWeightChildHeight.roundToPx() * 2,
                                    constraints.minHeight
                                )
                                assertEquals(
                                    columnMinHeight.roundToPx() -
                                        noWeightChildHeight.roundToPx() * 2,
                                    constraints.maxHeight
                                )
                                latch.countDown()
                                layout(0, 0) { }
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun testColumn_measuresNoWeightChildrenCorrectly() = with(density) {
        val availableWidth = 100.toDp()
        val childWidth = 50.toDp()
        val availableHeight = 200.toDp()
        val childHeight = 100.toDp()
        val latch = CountDownLatch(1)
        show {
            Box {
                ConstrainedBox(
                    DpConstraints(
                        minWidth = availableWidth,
                        maxWidth = availableWidth,
                        minHeight = availableHeight,
                        maxHeight = availableHeight
                    )
                ) {
                    Column {
                        BoxWithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.roundToPx(),
                                    maxHeight = availableHeight.roundToPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(
                                childWidth.roundToPx(),
                                childHeight.roundToPx(),
                                mapOf()
                            )
                        }
                        BoxWithConstraints {
                            assertEquals(
                                Constraints(
                                    maxWidth = availableWidth.roundToPx(),
                                    maxHeight = availableHeight.roundToPx() -
                                        childHeight.roundToPx()
                                ),
                                constraints
                            )
                            FixedSizeLayout(
                                childWidth.roundToPx(),
                                childHeight.roundToPx(),
                                mapOf()
                            )
                            latch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_doesNotExpand_whenWeightChildrenDoNotFill() = with(density) {
        val size = 10
        var columnHeight = 0
        val latch = CountDownLatch(1)
        show {
            Column(
                Modifier.onGloballyPositioned {
                    columnHeight = it.size.height
                    latch.countDown()
                }
            ) {
                Box(Modifier.weight(1f, false).size(size.toDp()))
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(size, columnHeight)
    }

    @Test
    fun testColumn_includesSpacing_withWeightChildren() = with(density) {
        val columnHeight = 40
        val space = 8
        val latch = CountDownLatch(2)
        show {
            Column(
                modifier = Modifier.height(columnHeight.toDp()),
                verticalArrangement = Arrangement.spacedBy(space.toDp())
            ) {
                Box(
                    Modifier.weight(1f).onGloballyPositioned {
                        assertEquals((columnHeight - space) / 2, it.size.height)
                        assertEquals(0, it.positionInRoot().y.toInt())
                        latch.countDown()
                    }
                )
                Box(
                    Modifier.weight(1f).onGloballyPositioned {
                        assertEquals((columnHeight - space) / 2, it.size.height)
                        assertEquals(
                            (columnHeight - space) / 2 + space,
                            it.positionInRoot().y.toInt()
                        )
                        latch.countDown()
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region Main axis alignment tests in Row
    @Test
    fun testRow_withStartArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        }
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1])
        assertEquals(Offset(size.toFloat() * 2, 0f), childPosition[2])
    }

    @Test
    fun testRow_withEndArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.End
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset((root.width - size.toFloat() * 3), 0f), childPosition[0])
        assertEquals(Offset((root.width - size.toFloat() * 2), 0f), childPosition[1])
        assertEquals(Offset((root.width - size.toFloat()), 0f), childPosition[2])
    }

    @Test
    fun testRow_withCenterArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val extraSpace = root.width - size * 3
        assertEquals(Offset((extraSpace / 2f).roundToInt().toFloat(), 0f), childPosition[0])
        assertEquals(
            Offset(((extraSpace / 2f) + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3f) / 4f
        assertEquals(
            Offset(gap.roundToInt().toFloat(), 0f), childPosition[0]
        )
        assertEquals(
            Offset((size.toFloat() + gap * 2f).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset((size.toFloat() * 2f + gap * 3f).roundToInt().toFloat(), 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3) / 2
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(
            Offset((gap + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset((gap * 2 + size.toFloat() * 2).roundToInt().toFloat(), 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width.toFloat() - size * 3) / 3
        assertEquals(Offset((gap / 2f).roundToInt().toFloat(), 0f), childPosition[0])
        assertEquals(
            Offset(((gap * 3 / 2) + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset(((gap * 5 / 2) + size.toFloat() * 2).roundToInt().toFloat(), 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_withSpacedByArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space),
                    modifier = Modifier.onGloballyPositioned {
                        assertEquals((sizePx * 2 + spacePx).roundToInt(), it.size.width)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(0f, it.positionInParent().x)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(sizePx + spacePx, it.positionInParent().x)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_withSpacedByAlignedArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val rowSizePx = 50
        val rowSize = rowSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space, Alignment.End),
                    modifier = Modifier.requiredSize(rowSize).onGloballyPositioned {
                        assertEquals(rowSizePx, it.size.width)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(rowSizePx - spacePx - sizePx * 2, it.positionInParent().x)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(rowSizePx - sizePx, it.positionInParent().x)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_withSpacedByArrangement_insufficientSpace() = with(density) {
        val spacePx = 15f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val rowSizePx = 50f
        val rowSize = rowSizePx.toDp()
        val latch = CountDownLatch(4)
        show {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space),
                    modifier = Modifier.requiredSize(rowSize).onGloballyPositioned {
                        assertEquals(rowSizePx.roundToInt(), it.size.width)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(0f, it.positionInParent().x)
                            assertEquals(sizePx.roundToInt(), it.size.width)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(sizePx + spacePx, it.positionInParent().x)
                            assertEquals((rowSizePx - spacePx - sizePx).roundToInt(), it.size.width)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(rowSizePx, it.positionInParent().x)
                            assertEquals(0, it.size.width)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_withAlignedArrangement() = with(density) {
        val sizePx = 20f
        val size = sizePx.toDp()
        val rowSizePx = 50f
        val rowSize = rowSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Column {
                Row(
                    horizontalArrangement = Arrangement.aligned(Alignment.End),
                    modifier = Modifier.requiredSize(rowSize).onGloballyPositioned {
                        assertEquals(rowSizePx.roundToInt(), it.size.width)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(rowSizePx - sizePx * 2, it.positionInParent().x)
                            assertEquals(sizePx.roundToInt(), it.size.width)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(rowSizePx - sizePx, it.positionInParent().x)
                            assertEquals(sizePx.roundToInt(), it.size.width)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region Main axis alignment tests in Column
    @Test
    fun testColumn_withTopArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    }
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(0f, size.toFloat()), childPosition[1])
        assertEquals(Offset(0f, size.toFloat() * 2), childPosition[2])
    }

    @Test
    fun testColumn_withBottomArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset(0f, (root.height - size.toFloat() * 3)), childPosition[0])
        assertEquals(Offset(0f, (root.height - size.toFloat() * 2)), childPosition[1])
        assertEquals(Offset(0f, (root.height - size.toFloat())), childPosition[2])
    }

    @Test
    fun testColumn_withCenterArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    verticalArrangement = Arrangement.Center
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val extraSpace = root.height - size * 3f
        assertEquals(
            Offset(0f, (extraSpace / 2).roundToInt().toFloat()),
            childPosition[0]
        )
        assertEquals(
            Offset(0f, ((extraSpace / 2) + size.toFloat()).roundToInt().toFloat()),
            childPosition[1]
        )
        assertEquals(
            Offset(
                0f,
                ((extraSpace / 2) + size.toFloat() * 2f).roundToInt().toFloat()
            ),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withSpaceEvenlyArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.height - size.toFloat() * 3) / 4
        assertEquals(Offset(0f, gap.roundToInt().toFloat()), childPosition[0])
        assertEquals(
            Offset(0f, (size.toFloat() + gap * 2).roundToInt().toFloat()),
            childPosition[1]
        )
        assertEquals(
            Offset(0f, (size.toFloat() * 2 + gap * 3f).roundToInt().toFloat()),
            childPosition[2]
        )
    }

    private fun calculateChildPositions(
        childPosition: Array<Offset>,
        parentLayoutCoordinates: LayoutCoordinates?,
        childLayoutCoordinates: Array<LayoutCoordinates?>
    ) {
        for (i in childPosition.indices) {
            childPosition[i] = parentLayoutCoordinates!!
                .localPositionOf(childLayoutCoordinates[i]!!, Offset(0f, 0f))
        }
    }

    @Test
    fun testColumn_withSpaceBetweenArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.height - size.toFloat() * 3f) / 2f
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(
            Offset(0f, (gap + size.toFloat()).roundToInt().toFloat()),
            childPosition[1]
        )
        assertEquals(
            Offset(0f, (gap * 2 + size.toFloat() * 2).roundToInt().toFloat()),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withSpaceAroundArrangement() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(4)
        val childPosition = arrayOf(
            Offset(-1f, -1f), Offset(-1f, -1f), Offset(-1f, -1f)
        )
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Center {
                Column(
                    Modifier.fillMaxHeight().onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.height - size.toFloat() * 3f) / 3f
        assertEquals(Offset(0f, (gap / 2f).roundToInt().toFloat()), childPosition[0])
        assertEquals(
            Offset(0f, ((gap * 3f / 2f) + size.toFloat()).roundToInt().toFloat()),
            childPosition[1]
        )
        assertEquals(
            Offset(0f, ((gap * 5f / 2f) + size.toFloat() * 2f).roundToInt().toFloat()),
            childPosition[2]
        )
    }

    @Test
    fun testColumn_withSpacedByArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Row {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space),
                    modifier = Modifier.onGloballyPositioned {
                        assertEquals((sizePx * 2 + spacePx).roundToInt(), it.size.height)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(0f, it.positionInParent().x)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(sizePx + spacePx, it.positionInParent().y)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_withSpacedByAlignedArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val columnSizePx = 50
        val columnSize = columnSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Row {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space, Alignment.Bottom),
                    modifier = Modifier.requiredSize(columnSize).onGloballyPositioned {
                        assertEquals(columnSizePx, it.size.height)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(
                                columnSizePx - spacePx - sizePx * 2, it.positionInParent().y
                            )
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(columnSizePx - sizePx, it.positionInParent().y)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_withSpacedByArrangement_insufficientSpace() = with(density) {
        val spacePx = 15f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val columnSizePx = 50f
        val columnSize = columnSizePx.toDp()
        val latch = CountDownLatch(4)
        show {
            Row {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space),
                    modifier = Modifier.requiredSize(columnSize).onGloballyPositioned {
                        assertEquals(columnSizePx.roundToInt(), it.size.height)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(0f, it.positionInParent().y)
                            assertEquals(sizePx.roundToInt(), it.size.height)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(sizePx + spacePx, it.positionInParent().y)
                            assertEquals(
                                (columnSizePx - spacePx - sizePx).roundToInt(), it.size.height
                            )
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.size(size).onGloballyPositioned {
                            assertEquals(columnSizePx, it.positionInParent().y)
                            assertEquals(0, it.size.height)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_withAlignedArrangement() = with(density) {
        val sizePx = 20f
        val size = sizePx.toDp()
        val columnSizePx = 50
        val columnSize = columnSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            Row {
                Column(
                    verticalArrangement = Arrangement.aligned(Alignment.Bottom),
                    modifier = Modifier.requiredSize(columnSize).onGloballyPositioned {
                        assertEquals(columnSizePx, it.size.height)
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(columnSizePx - sizePx * 2, it.positionInParent().y)
                            latch.countDown()
                        }
                    )
                    Box(
                        Modifier.requiredSize(size).onGloballyPositioned {
                            assertEquals(columnSizePx - sizePx, it.positionInParent().y)
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRow_doesNotUseMinConstraintsOnChildren() = with(density) {
        val sizeDp = 50.toDp()
        val childSizeDp = 30.toDp()
        val childSize = childSizeDp.roundToPx()

        val layoutLatch = CountDownLatch(1)
        val containerSize = Ref<IntSize>()
        show {
            Center {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp)
                ) {
                    Row {
                        Spacer(
                            Modifier.size(width = childSizeDp, height = childSizeDp)
                                .onGloballyPositioned { coordinates ->
                                    containerSize.value = coordinates.size
                                    layoutLatch.countDown()
                                }
                        )
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(childSize, childSize), containerSize.value)
    }

    @Test
    fun testColumn_doesNotUseMinConstraintsOnChildren() = with(density) {
        val sizeDp = 50.toDp()
        val childSizeDp = 30.toDp()
        val childSize = childSizeDp.roundToPx()

        val layoutLatch = CountDownLatch(1)
        val containerSize = Ref<IntSize>()
        show {
            Center {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp)
                ) {
                    Column {
                        Spacer(
                            Modifier.size(width = childSizeDp, height = childSizeDp).then(
                                Modifier.onGloballyPositioned { coordinates ->
                                    containerSize.value = coordinates.size
                                    layoutLatch.countDown()
                                }
                            )
                        )
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(childSize, childSize), containerSize.value)
    }
    // endregion

    // region Intrinsic measurement tests
    @Test
    fun testRow_withNoWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Row {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth()) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row {
                    Container(
                        Modifier.aspectRatio(2f)
                            .align(Alignment.Top),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.align(Alignment.CenterVertically),
                        content = {}
                    )
                }
            },
            @Composable {
                Row {
                    Container(
                        Modifier.aspectRatio(2f).alignBy(FirstBaseline),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.alignBy { it.measuredWidth },
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Container(
                        Modifier.align(Alignment.CenterVertically).aspectRatio(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.align(Alignment.CenterVertically),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Container(
                        Modifier.align(Alignment.Bottom).aspectRatio(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.align(Alignment.Bottom),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Container(Modifier.fillMaxHeight().aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.fillMaxHeight(),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(
                25.toDp().roundToPx() * 2 + 50.toDp().roundToPx(),
                minIntrinsicWidth(25.toDp().roundToPx())
            )
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(70.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(
                25.toDp().roundToPx() * 2 + 50.toDp().roundToPx(),
                maxIntrinsicWidth(25.toDp().roundToPx())
            )
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(70.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testRow_withWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Row {
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.weight(3f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 40.toDp()),
                        Modifier.weight(2f),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(2f).weight(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row {
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.weight(3f).align(Alignment.Top),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 40.toDp()),
                        Modifier.weight(2f).align(Alignment.CenterVertically),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(2f).weight(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.align(Alignment.Bottom),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.Start) {
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.weight(3f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 40.toDp()),
                        Modifier.weight(2f),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(2f).weight(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.Center) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.weight(3f).align(Alignment.CenterVertically),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 40.toDp()),
                        modifier = Modifier.weight(2f).align(Alignment.CenterVertically),
                        content = {}
                    )
                    Container(
                        Modifier.aspectRatio(2f).weight(2f).align(Alignment.CenterVertically),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.align(Alignment.CenterVertically),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.End) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.weight(3f).align(Alignment.Bottom),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 40.toDp()),
                        modifier = Modifier.weight(2f).align(Alignment.Bottom),
                        content = {}
                    )
                    Container(
                        Modifier.aspectRatio(2f).weight(2f).align(Alignment.Bottom),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.align(Alignment.Bottom),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.SpaceAround) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.weight(3f).fillMaxHeight(),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 40.toDp()),
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        content = {}
                    )
                    Container(
                        Modifier.aspectRatio(2f).weight(2f).fillMaxHeight(),
                        content = {}
                    )
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(20.toDp(), 30.toDp()),
                        modifier = Modifier.fillMaxHeight(),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.weight(3f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 40.toDp()),
                        Modifier.weight(2f),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(2f).weight(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        Modifier.weight(3f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 40.toDp()),
                        Modifier.weight(2f),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(2f).weight(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(20.toDp(), 30.toDp()),
                        content = {}
                    )
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicWidth(0)
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicWidth(10.toDp().roundToPx())
            )
            assertEquals(
                25.toDp().roundToPx() * 2 / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicWidth(25.toDp().roundToPx())
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicWidth(Constraints.Infinity)
            )
            // Min height.
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(125.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), minIntrinsicHeight(370.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicWidth(0)
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicWidth(10.toDp().roundToPx())
            )
            assertEquals(
                25.toDp().roundToPx() * 2 / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicWidth(25.toDp().roundToPx())
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicWidth(Constraints.Infinity)
            )
            // Max height.
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(125.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), maxIntrinsicHeight(370.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testRow_withArrangementSpacing() = with(density) {
        val spacing = 5
        val childSize = 10
        testIntrinsics(
            @Composable {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.toDp())) {
                    Box(Modifier.size(childSize.toDp()))
                    Box(Modifier.size(childSize.toDp()))
                    Box(Modifier.size(childSize.toDp()))
                }
            }
        ) { minIntrinsicWidth, _, maxIntrinsicWidth, _ ->
            assertEquals(childSize * 3 + 2 * spacing, minIntrinsicWidth(Constraints.Infinity))
            assertEquals(childSize * 3 + 2 * spacing, maxIntrinsicWidth(Constraints.Infinity))
        }
    }

    @Test
    fun testColumn_withNoWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Column {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column {
                    Container(
                        Modifier.aspectRatio(2f).align(Alignment.Start),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.align(Alignment.End),
                        content = {}
                    )
                }
            },
            @Composable {
                Column {
                    Container(
                        Modifier.aspectRatio(2f).alignBy { 0 },
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.alignBy(TestVerticalLine),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight()) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Container(
                        Modifier.align(Alignment.CenterHorizontally).aspectRatio(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                    Container(
                        Modifier.align(Alignment.End).aspectRatio(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.align(Alignment.End),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceAround) {
                    Container(Modifier.fillMaxWidth().aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        Modifier.fillMaxWidth(),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Container(Modifier.aspectRatio(2f), content = {})
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                    Container(
                        Modifier.aspectRatio(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(50.toDp(), 40.toDp()),
                        content = {}
                    )
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(25.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(
                50.toDp().roundToPx() / 2 + 40.toDp().roundToPx(),
                minIntrinsicHeight(50.toDp().roundToPx())
            )
            assertEquals(40.toDp().roundToPx(), minIntrinsicHeight(Constraints.Infinity))
            // Max width.
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(25.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(0.toDp().roundToPx()))
            assertEquals(
                50.toDp().roundToPx() / 2 + 40.toDp().roundToPx(),
                maxIntrinsicHeight(50.toDp().roundToPx())
            )
            assertEquals(40.toDp().roundToPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testColumn_withWeightChildren_hasCorrectIntrinsicMeasurements() = with(density) {
        testIntrinsics(
            @Composable {
                Column {
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.weight(3f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(40.toDp(), 30.toDp()),
                        Modifier.weight(2f),
                        content = {}
                    )
                    Container(
                        Modifier.aspectRatio(0.5f).weight(2f),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        content = {}
                    )
                }
            },
            @Composable {
                Column {
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.weight(3f).align(Alignment.Start),
                        content = {}
                    )
                    ConstrainedBox(
                        DpConstraints.fixed(40.toDp(), 30.toDp()),
                        Modifier.weight(2f).align(Alignment.CenterHorizontally),
                        content = {}
                    )
                    Container(Modifier.aspectRatio(0.5f).weight(2f)) { }
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.align(Alignment.End)
                    ) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.Top) {
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.weight(3f)
                    ) { }
                    ConstrainedBox(
                        DpConstraints.fixed(40.toDp(), 30.toDp()),
                        Modifier.weight(2f)
                    ) { }
                    Container(Modifier.aspectRatio(0.5f).weight(2f)) { }
                    ConstrainedBox(DpConstraints.fixed(30.toDp(), 20.toDp())) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.Center) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.weight(3f).align(Alignment.CenterHorizontally)
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(40.toDp(), 30.toDp()),
                        modifier = Modifier.weight(2f).align(Alignment.CenterHorizontally)
                    ) { }
                    Container(
                        Modifier.aspectRatio(0.5f).weight(2f).align(Alignment.CenterHorizontally)
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.Bottom) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.weight(3f).align(Alignment.End)
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(40.toDp(), 30.toDp()),
                        modifier = Modifier.weight(2f).align(Alignment.End)
                    ) { }
                    Container(
                        Modifier.aspectRatio(0.5f).weight(2f).align(Alignment.End)
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.align(Alignment.End)
                    ) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.SpaceAround) {
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.weight(3f).fillMaxWidth()
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(40.toDp(), 30.toDp()),
                        modifier = Modifier.weight(2f).fillMaxWidth()
                    ) { }
                    Container(
                        Modifier.aspectRatio(0.5f).weight(2f).fillMaxWidth()
                    ) { }
                    ConstrainedBox(
                        constraints = DpConstraints.fixed(30.toDp(), 20.toDp()),
                        modifier = Modifier.fillMaxWidth()
                    ) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.weight(3f)
                    ) { }
                    ConstrainedBox(
                        DpConstraints.fixed(40.toDp(), 30.toDp()),
                        Modifier.weight(2f)
                    ) { }
                    Container(Modifier.aspectRatio(0.5f).then(Modifier.weight(2f))) { }
                    ConstrainedBox(DpConstraints.fixed(30.toDp(), 20.toDp())) { }
                }
            },
            @Composable {
                Column(verticalArrangement = Arrangement.SpaceEvenly) {
                    ConstrainedBox(
                        DpConstraints.fixed(30.toDp(), 20.toDp()),
                        Modifier.weight(3f)
                    ) { }
                    ConstrainedBox(
                        DpConstraints.fixed(40.toDp(), 30.toDp()),
                        Modifier.weight(2f)
                    ) { }
                    Container(Modifier.aspectRatio(0.5f).then(Modifier.weight(2f))) { }
                    ConstrainedBox(DpConstraints.fixed(30.toDp(), 20.toDp())) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Min width.
            assertEquals(40.toDp().roundToPx(), minIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicWidth(125.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), minIntrinsicWidth(370.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), minIntrinsicWidth(Constraints.Infinity))
            // Min height.
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicHeight(0)
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicHeight(10.toDp().roundToPx())
            )
            assertEquals(
                25.toDp().roundToPx() * 2 / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicHeight(25.toDp().roundToPx())
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                minIntrinsicHeight(Constraints.Infinity)
            )
            // Max width.
            assertEquals(40.toDp().roundToPx(), maxIntrinsicWidth(0.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicWidth(125.toDp().roundToPx()))
            assertEquals(50.toDp().roundToPx(), maxIntrinsicWidth(370.toDp().roundToPx()))
            assertEquals(40.toDp().roundToPx(), maxIntrinsicWidth(Constraints.Infinity))
            // Max height.
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicHeight(0)
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicHeight(10.toDp().roundToPx())
            )
            assertEquals(
                25.toDp().roundToPx() * 2 / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicHeight(25.toDp().roundToPx())
            )
            assertEquals(
                30.toDp().roundToPx() / 2 * 7 + 20.toDp().roundToPx(),
                maxIntrinsicHeight(Constraints.Infinity)
            )
        }
    }

    @Test
    fun testColumn_withArrangementSpacing() = with(density) {
        val spacing = 5
        val childSize = 10
        testIntrinsics(
            @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.toDp())) {
                    Box(Modifier.size(childSize.toDp()))
                    Box(Modifier.size(childSize.toDp()))
                    Box(Modifier.size(childSize.toDp()))
                }
            }
        ) { _, minIntrinsicHeight, _, maxIntrinsicHeight ->
            assertEquals(childSize * 3 + 2 * spacing, minIntrinsicHeight(Constraints.Infinity))
            assertEquals(childSize * 3 + 2 * spacing, maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test
    fun testRow_withWIHOChild_hasCorrectIntrinsicMeasurements() = with(density) {
        val dividerWidth = 10.dp
        val rowWidth = 40.dp

        val positionedLatch = CountDownLatch(1)
        show {
            Row(Modifier.requiredWidth(rowWidth).height(IntrinsicSize.Min)) {
                Container(
                    Modifier.requiredWidth(dividerWidth).fillMaxHeight().onGloballyPositioned {
                        assertEquals(
                            it.size.height,
                            (rowWidth.roundToPx() - dividerWidth.roundToPx()) / 2
                        )
                        positionedLatch.countDown()
                    }
                ) {}
                val measurePolicy = object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ) = layout(constraints.maxWidth, constraints.maxWidth / 2) {}

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = rowWidth.roundToPx() / 10

                    override fun IntrinsicMeasureScope.minIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = width / 2

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = rowWidth.roundToPx() * 2

                    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = width / 2
                }
                Layout(
                    content = {},
                    measurePolicy = measurePolicy
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_withHIWOChild_hasCorrectIntrinsicMeasurements() = with(density) {
        val dividerHeight = 10.dp
        val columnHeight = 40.dp

        val positionedLatch = CountDownLatch(1)
        show {
            Column(Modifier.requiredHeight(columnHeight).width(IntrinsicSize.Min)) {
                Container(
                    Modifier.requiredHeight(dividerHeight).fillMaxWidth().onGloballyPositioned {
                        assertEquals(
                            it.size.width,
                            (columnHeight.roundToPx() - dividerHeight.roundToPx()) / 2
                        )
                        positionedLatch.countDown()
                    }
                ) {}
                val measurePolicy = object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ) = layout(constraints.maxHeight / 2, constraints.maxHeight) {}

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = height / 2

                    override fun IntrinsicMeasureScope.minIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = columnHeight.roundToPx() / 10

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = height / 2

                    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = columnHeight.roundToPx() * 2
                }
                Layout(
                    content = {},
                    measurePolicy = measurePolicy
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    // endregion

    // region Modifiers specific tests
    @Test
    fun testRowColumnModifiersChain_leftMostWins() = with(density) {
        val positionedLatch = CountDownLatch(1)
        val containerHeight = Ref<Int>()
        val columnHeight = 24

        show {
            Box {
                Column(Modifier.height(columnHeight.toDp())) {
                    Container(
                        Modifier.weight(2f)
                            .weight(1f)
                            .onGloballyPositioned { coordinates ->
                                containerHeight.value = coordinates.size.height
                                positionedLatch.countDown()
                            },
                        content = {}
                    )
                    Container(Modifier.weight(1f), content = {})
                }
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertNotNull(containerHeight.value)
        assertEquals(columnHeight * 2 / 3, containerHeight.value)
    }

    @Test
    fun testAlignByModifiersChain_leftMostWins() = with(density) {
        val positionedLatch = CountDownLatch(1)
        val containerSize = Ref<IntSize>()
        val containerPosition = Ref<Offset>()
        val size = 40.dp

        show {
            Row {
                Container(
                    modifier = Modifier.alignBy { it.measuredHeight },
                    width = size,
                    height = size,
                    content = {}
                )
                Container(
                    modifier = Modifier.alignBy { 0 }
                        .alignBy { it.measuredHeight / 2 }
                        .onGloballyPositioned { coordinates ->
                            containerSize.value = coordinates.size
                            containerPosition.value = coordinates.positionInRoot()
                            positionedLatch.countDown()
                        },
                    width = size,
                    height = size,
                    content = {}
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertNotNull(containerSize)
        assertEquals(Offset(size.toPx(), size.toPx()), containerPosition.value)
    }
    // endregion

    // region Rtl tests
    @Test
    fun testRow_Rtl_arrangementStart() = with(density) {
        val sizeDp = 35.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(Offset.Zero, Offset.Zero)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(Modifier.fillMaxWidth()) {
                    Container(
                        Modifier.size(sizeDp).onGloballyPositioned { coordinates ->
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        Modifier.size(sizeDp * 2)
                            .onGloballyPositioned { coordinates ->
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(Offset((rootWidth - size.toFloat()), 0f), childPosition[0])
        assertEquals(
            Offset((rootWidth - (sizeDp.toPx() * 3f).roundToInt()).toFloat(), 0f),
            childPosition[1]
        )
    }

    @Test
    fun testRow_Rtl_arrangementCenter() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val extraSpace = root.width - size * 3
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[0]
        )
        assertEquals(
            Offset(((extraSpace / 2f) + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(Offset((extraSpace / 2f).roundToInt().toFloat(), 0f), childPosition[2])
    }

    @Test
    fun testRow_Rtl_arrangementSpaceEvenly() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3f) / 4f
        assertEquals(
            Offset((size.toFloat() * 2f + gap * 3f).roundToInt().toFloat(), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset((size.toFloat() + gap * 2f).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset(gap.roundToInt().toFloat(), 0f), childPosition[2]
        )
    }

    @Test
    fun testRow_Rtl_arrangementSpaceBetween() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3) / 2
        assertEquals(
            Offset((gap * 2 + size.toFloat() * 2).roundToInt().toFloat(), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset((gap + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(Offset(0f, 0f), childPosition[2])
    }

    @Test
    fun testRow_Rtl_arrangementSpaceAround() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates = arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] = coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(childPosition, parentLayoutCoordinates, childLayoutCoordinates)

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width.toFloat() - size * 3) / 3
        assertEquals(
            Offset(((gap * 5 / 2) + size.toFloat() * 2).roundToInt().toFloat(), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset(((gap * 3 / 2) + size.toFloat()).roundToInt().toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(Offset((gap / 2f).roundToInt().toFloat(), 0f), childPosition[2])
    }

    @Test
    fun testRow_Rtl_arrangementEnd() = with(density) {
        val sizeDp = 35.toDp()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(Offset.Zero, Offset.Zero)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Container(
                        Modifier.size(sizeDp).onGloballyPositioned { coordinates ->
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        Modifier.size(sizeDp * 2)
                            .onGloballyPositioned { coordinates ->
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(
            Offset(
                (sizeDp.toPx() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[0]
        )
        assertEquals(Offset(0f, 0f), childPosition[1])
    }

    @Test
    fun testRow_Rtl_withSpacedByAlignedArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val rowSizePx = 50
        val rowSize = rowSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space, Alignment.End),
                        modifier = Modifier.requiredSize(rowSize).onGloballyPositioned {
                            assertEquals(rowSizePx, it.size.width)
                            latch.countDown()
                        }
                    ) {
                        Box(
                            Modifier.requiredSize(size).onGloballyPositioned {
                                assertEquals(sizePx + spacePx, it.positionInParent().x)
                                latch.countDown()
                            }
                        )
                        Box(
                            Modifier.requiredSize(size).onGloballyPositioned {
                                assertEquals(0f, it.positionInParent().x)
                                latch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testColumn_Rtl_gravityStart() = with(density) {
        val sizeDp = 35.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(Offset.Zero, Offset.Zero)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth()) {
                    Container(
                        Modifier.size(sizeDp).onGloballyPositioned { coordinates ->
                            childPosition[0] = coordinates.positionInRoot()
                            drawLatch.countDown()
                        }
                    ) {
                    }

                    Container(
                        Modifier.size(sizeDp * 2)
                            .onGloballyPositioned { coordinates ->
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(Offset((rootWidth - size.toFloat()), 0f), childPosition[0])
        assertEquals(
            Offset(
                (rootWidth - (sizeDp * 2f).toPx()).roundToInt().toFloat(),
                size.toFloat()
            ),
            childPosition[1]
        )
    }

    @Test
    fun testColumn_Rtl_gravityEnd() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(Offset.Zero, Offset.Zero)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth()) {
                    Container(
                        Modifier.size(sizeDp)
                            .align(Alignment.End)
                            .onGloballyPositioned { coordinates ->
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Container(
                        Modifier.size(sizeDp * 2)
                            .align(Alignment.End)
                            .onGloballyPositioned { coordinates ->
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(0f, size.toFloat()), childPosition[1])
    }

    @Test
    fun testColumn_Rtl_gravityAlignBy() = with(density) {
        val sizeDp = 50.toDp()
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = arrayOf(Offset.Zero, Offset.Zero)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth()) {
                    Container(
                        Modifier.size(sizeDp)
                            .alignBy { it.measuredWidth }
                            .onGloballyPositioned { coordinates ->
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Container(
                        Modifier.size(sizeDp)
                            .alignBy { it.measuredHeight / 2 }
                            .onGloballyPositioned { coordinates ->
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findComposeView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(
            Offset((rootWidth - size.toFloat()), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset(
                (rootWidth - size.toFloat() * 1.5f).roundToInt().toFloat(),
                size.toFloat()
            ),
            childPosition[1]
        )
    }
    //endregion

    // region AbsoluteArrangement tests
    @Test
    fun testRow_absoluteArrangementLeft() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.Left
            ) {
                for (i in childPosition.indices) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1])
        assertEquals(
            Offset(size.toFloat() * 2, 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Rtl_absoluteArrangementLeft() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.Absolute.Left
                ) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] =
                                    coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(Offset(size.toFloat(), 0f), childPosition[1])
        assertEquals(
            Offset(size.toFloat() * 2, 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_absoluteArrangementRight() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.Right
            ) {
                for (i in childPosition.indices) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            Offset((root.width - size.toFloat() * 3), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset((root.width - size.toFloat() * 2), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset((root.width - size.toFloat()), 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Rtl_absoluteArrangementRight() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.Absolute.Right
                ) {
                    for (i in childPosition.indices) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] =
                                    coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(
            Offset((root.width - size.toFloat() * 3), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset((root.width - size.toFloat() * 2), 0f),
            childPosition[1]
        )
        assertEquals(
            Offset((root.width - size.toFloat()), 0f),
            childPosition[2]
        )
    }

    @Test
    fun testRow_absoluteArrangementCenter() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.Center
            ) {
                for (i in 0 until childPosition.size) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        val extraSpace = root.width - size * 3
        assertEquals(
            Offset(
                (extraSpace / 2f).roundToInt().toFloat(),
                0f
            ),
            childPosition[0]
        )
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat()).roundToInt().toFloat(),
                0f
            ),
            childPosition[1]
        )
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Rtl_absoluteArrangementCenter() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            parentLayoutCoordinates = coordinates
                            drawLatch.countDown()
                        },
                    horizontalArrangement = Arrangement.Absolute.Center
                ) {
                    for (i in 0 until childPosition.size) {
                        Container(
                            width = sizeDp,
                            height = sizeDp,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                childLayoutCoordinates[i] =
                                    coordinates
                                drawLatch.countDown()
                            },
                            content = {}
                        )
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        val extraSpace = root.width - size * 3
        assertEquals(
            Offset(
                (extraSpace / 2f).roundToInt().toFloat(),
                0f
            ),
            childPosition[0]
        )
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat()).roundToInt().toFloat(),
                0f
            ),
            childPosition[1]
        )
        assertEquals(
            Offset(
                ((extraSpace / 2f) + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_absoluteArrangementSpaceEvenly() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.SpaceEvenly
            ) {
                for (i in childPosition.indices) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3f) / 4f
        assertEquals(
            Offset(gap.roundToInt().toFloat(), 0f), childPosition[0]
        )
        assertEquals(
            Offset(
                (size.toFloat() + gap * 2f).roundToInt().toFloat(),
                0f
            ),
            childPosition[1]
        )
        assertEquals(
            Offset(
                (size.toFloat() * 2f + gap * 3f).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Row_absoluteArrangementSpaceEvenly() =
        with(density) {
            val size = 100
            val sizeDp = size.toDp()

            val drawLatch = CountDownLatch(4)
            val childPosition = Array(3) { Offset.Zero }
            val childLayoutCoordinates =
                arrayOfNulls<LayoutCoordinates?>(childPosition.size)
            var parentLayoutCoordinates: LayoutCoordinates? = null
            show {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                parentLayoutCoordinates =
                                    coordinates
                                drawLatch.countDown()
                            },
                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly
                    ) {
                        for (i in childPosition.indices) {
                            Container(
                                width = sizeDp,
                                height = sizeDp,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    childLayoutCoordinates[i] =
                                        coordinates
                                    drawLatch.countDown()
                                },
                                content = {}
                            )
                        }
                    }
                }
            }
            assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

            calculateChildPositions(
                childPosition,
                parentLayoutCoordinates,
                childLayoutCoordinates
            )

            val root = findComposeView()
            waitForDraw(root)

            val gap = (root.width - size.toFloat() * 3f) / 4f
            assertEquals(
                Offset(gap.roundToInt().toFloat(), 0f),
                childPosition[0]
            )
            assertEquals(
                Offset(
                    (size.toFloat() + gap * 2f).roundToInt().toFloat(),
                    0f
                ),
                childPosition[1]
            )
            assertEquals(
                Offset(
                    (size.toFloat() * 2f + gap * 3f).roundToInt().toFloat(),
                    0f
                ),
                childPosition[2]
            )
        }

    @Test
    fun testRow_absoluteArrangementSpaceBetween() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.SpaceBetween
            ) {
                for (i in childPosition.indices) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width - size.toFloat() * 3) / 2
        assertEquals(Offset(0f, 0f), childPosition[0])
        assertEquals(
            Offset(
                (gap + size.toFloat()).roundToInt().toFloat(),
                0f
            ),
            childPosition[1]
        )
        assertEquals(
            Offset(
                (gap * 2 + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Row_absoluteArrangementSpaceBetween() =
        with(density) {
            val size = 100
            val sizeDp = size.toDp()

            val drawLatch = CountDownLatch(4)
            val childPosition = Array(3) { Offset.Zero }
            val childLayoutCoordinates =
                arrayOfNulls<LayoutCoordinates?>(childPosition.size)
            var parentLayoutCoordinates: LayoutCoordinates? = null
            show {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                parentLayoutCoordinates =
                                    coordinates
                                drawLatch.countDown()
                            },
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween
                    ) {
                        for (i in childPosition.indices) {
                            Container(
                                width = sizeDp,
                                height = sizeDp,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    childLayoutCoordinates[i] =
                                        coordinates
                                    drawLatch.countDown()
                                },
                                content = {}
                            )
                        }
                    }
                }
            }
            assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

            calculateChildPositions(
                childPosition,
                parentLayoutCoordinates,
                childLayoutCoordinates
            )

            val root = findComposeView()
            waitForDraw(root)

            val gap = (root.width - size.toFloat() * 3) / 2
            assertEquals(Offset(0f, 0f), childPosition[0])
            assertEquals(
                Offset(
                    (gap + size.toFloat()).roundToInt().toFloat(),
                    0f
                ),
                childPosition[1]
            )
            assertEquals(
                Offset(
                    (gap * 2 + size.toFloat() * 2).roundToInt().toFloat(),
                    0f
                ),
                childPosition[2]
            )
        }

    @Test
    fun testRow_absoluteArrangementSpaceAround() = with(density) {
        val size = 100
        val sizeDp = size.toDp()

        val drawLatch = CountDownLatch(4)
        val childPosition = Array(3) { Offset.Zero }
        val childLayoutCoordinates =
            arrayOfNulls<LayoutCoordinates?>(childPosition.size)
        var parentLayoutCoordinates: LayoutCoordinates? = null
        show {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        parentLayoutCoordinates = coordinates
                        drawLatch.countDown()
                    },
                horizontalArrangement = Arrangement.Absolute.SpaceAround
            ) {
                for (i in 0 until childPosition.size) {
                    Container(
                        width = sizeDp,
                        height = sizeDp,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            childLayoutCoordinates[i] = coordinates
                            drawLatch.countDown()
                        },
                        content = {}
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        calculateChildPositions(
            childPosition,
            parentLayoutCoordinates,
            childLayoutCoordinates
        )

        val root = findComposeView()
        waitForDraw(root)

        val gap = (root.width.toFloat() - size * 3) / 3
        assertEquals(
            Offset((gap / 2f).roundToInt().toFloat(), 0f),
            childPosition[0]
        )
        assertEquals(
            Offset(
                ((gap * 3 / 2) + size.toFloat()).roundToInt().toFloat(),
                0f
            ),
            childPosition[1]
        )
        assertEquals(
            Offset(
                ((gap * 5 / 2) + size.toFloat() * 2).roundToInt().toFloat(),
                0f
            ),
            childPosition[2]
        )
    }

    @Test
    fun testRow_Rtl_absoluteArrangementSpaceAround() =
        with(density) {
            val size = 100
            val sizeDp = size.toDp()

            val drawLatch = CountDownLatch(4)
            val childPosition = Array(3) { Offset.Zero }
            val childLayoutCoordinates =
                arrayOfNulls<LayoutCoordinates?>(childPosition.size)
            var parentLayoutCoordinates: LayoutCoordinates? = null
            show {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                parentLayoutCoordinates =
                                    coordinates
                                drawLatch.countDown()
                            },
                        horizontalArrangement = Arrangement.Absolute.SpaceAround
                    ) {
                        for (i in 0 until childPosition.size) {
                            Container(
                                width = sizeDp,
                                height = sizeDp,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    childLayoutCoordinates[i] =
                                        coordinates
                                    drawLatch.countDown()
                                },
                                content = {}
                            )
                        }
                    }
                }
            }
            assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

            calculateChildPositions(
                childPosition,
                parentLayoutCoordinates,
                childLayoutCoordinates
            )

            val root = findComposeView()
            waitForDraw(root)

            val gap = (root.width.toFloat() - size * 3) / 3
            assertEquals(
                Offset(
                    (gap / 2f).roundToInt().toFloat(),
                    0f
                ),
                childPosition[0]
            )
            assertEquals(
                Offset(
                    ((gap * 3 / 2) + size.toFloat()).roundToInt().toFloat(),
                    0f
                ),
                childPosition[1]
            )
            assertEquals(
                Offset(
                    ((gap * 5 / 2) + size.toFloat() * 2).roundToInt().toFloat(),
                    0f
                ),
                childPosition[2]
            )
        }

    @Test
    fun testRow_Rtl_withSpacedByAlignedAbsoluteArrangement() = with(density) {
        val spacePx = 10f
        val space = spacePx.toDp()
        val sizePx = 20f
        val size = sizePx.toDp()
        val rowSizePx = 50
        val rowSize = rowSizePx.toDp()
        val latch = CountDownLatch(3)
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Absolute.spacedBy(space, Alignment.End),
                        modifier = Modifier.requiredSize(rowSize).onGloballyPositioned {
                            assertEquals(rowSizePx, it.size.width)
                            latch.countDown()
                        }
                    ) {
                        Box(
                            Modifier.requiredSize(size).onGloballyPositioned {
                                assertEquals(0f, it.positionInParent().x)
                                latch.countDown()
                            }
                        )
                        Box(
                            Modifier.requiredSize(size).onGloballyPositioned {
                                assertEquals(sizePx + spacePx, it.positionInParent().x)
                                latch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
    // endregion

    // region InspectableValue tests for Row and Column
    @Test
    fun testRow_AlignInspectableValue() {
        val modifier = with(RowScopeInstance) { Modifier.align(Alignment.Bottom) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("align")
        Truth.assertThat(modifier.valueOverride).isEqualTo(Alignment.Bottom)
        Truth.assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }

    @Test
    fun testRow_AlignByInspectableValue() {
        val modifier = with(RowScopeInstance) { Modifier.alignBy(FirstBaseline) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("alignBy")
        Truth.assertThat(modifier.valueOverride).isEqualTo(FirstBaseline)
        Truth.assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }

    @Test
    fun testRow_WeightInspectableValue() {
        val modifier = with(RowScopeInstance) { Modifier.weight(2.0f, false) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("weight")
        Truth.assertThat(modifier.valueOverride).isEqualTo(2.0f)
        Truth.assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("weight", 2.0f),
            ValueElement("fill", false)
        )
    }
    @Test
    fun testColumn_AlignInspectableValue() {
        val modifier = with(ColumnScopeInstance) { Modifier.align(Alignment.Start) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("align")
        Truth.assertThat(modifier.valueOverride).isEqualTo(Alignment.Start)
        Truth.assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }

    @Test
    fun testColumn_AlignByInspectableValue() {
        val modifier = with(ColumnScopeInstance) { Modifier.alignBy(TestVerticalLine) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("alignBy")
        Truth.assertThat(modifier.valueOverride).isEqualTo(TestVerticalLine)
        Truth.assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }

    @Test
    fun testColumn_WeightInspectableValue() {
        val modifier = with(ColumnScopeInstance) { Modifier.weight(2.0f, false) }
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("weight")
        Truth.assertThat(modifier.valueOverride).isEqualTo(2.0f)
        Truth.assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("weight", 2.0f),
            ValueElement("fill", false)
        )
    }
    // endregion
}

private val TestHorizontalLine = HorizontalAlignmentLine(::min)
private val TestVerticalLine = VerticalAlignmentLine(::min)

@Composable
private fun BaselineTestLayout(
    width: Dp,
    height: Dp,
    baseline: Dp,
    modifier: Modifier,
    horizontalLine: HorizontalAlignmentLine = TestHorizontalLine,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { _, constraints ->
            val widthPx = max(width.roundToPx(), constraints.minWidth)
            val heightPx = max(height.roundToPx(), constraints.minHeight)
            layout(
                widthPx, heightPx,
                mapOf(
                    horizontalLine to baseline.roundToPx(),
                    TestVerticalLine to baseline.roundToPx()
                )
            ) {}
        }
    )
}

// Center composable function is deprected whereas FlexTest tests heavily depend on it.
@Composable
private fun Center(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0
            )
        )

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = Alignment.Center.align(
                    IntSize(placeable.width, placeable.height),
                    IntSize(layoutWidth, layoutHeight),
                    layoutDirection
                )
                placeable.placeRelative(position.x, position.y)
            }
        }
    }
}
