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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class PaddingTest : LayoutTest() {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    /**
     * Tests that negative start padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeStartPadding_throws() {
        Modifier.padding(start = -1f.dp)
    }

    /**
     * Tests that negative top padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeTopPadding_throws() {
        Modifier.padding(top = -1f.dp)
    }

    /**
     * Tests that negative end padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeEndPadding_throws() {
        Modifier.padding(end = -1f.dp)
    }

    /**
     * Tests that negative bottom padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeBottomPadding_throws() {
        Modifier.padding(bottom = -1f.dp)
    }

    /**
     * Tests that the [padding]-all and [padding] factories return equivalent modifiers.
     */
    @Test
    fun allEqualToAbsoluteWithExplicitSides() {
        Assert.assertEquals(
            Modifier.padding(10.dp, 10.dp, 10.dp, 10.dp),
            Modifier.padding(10.dp)
        )
    }

    /**
     * Tests that the symmetrical-[padding] and [padding] factories return equivalent modifiers.
     */
    @Test
    fun symmetricEqualToAbsoluteWithExplicitSides() {
        Assert.assertEquals(
            Modifier.padding(10.dp, 20.dp, 10.dp, 20.dp),
            Modifier.padding(10.dp, 20.dp)
        )
    }

    /**
     * Tests the top-level [padding] modifier factory with a single "all sides" argument,
     * checking that a uniform padding of all sides is applied to a child when plenty of space is
     * available for both content and padding.
     */
    @Test
    fun paddingAllAppliedToChild() = with(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), content = child)
        }
    }

    /**
     * Tests the top-level [padding] modifier factory with a single [PaddingValues]
     * argument, checking that padding is applied to a child when plenty of space
     * is available for both content and padding.
     */
    @Test
    fun paddingPaddingValuesAppliedToChild() = with(density) {
        val padding = PaddingValues(start = 1.dp, top = 3.dp, end = 6.dp, bottom = 10.dp)
        testPaddingWithDifferentInsetsImplementation(
            1.dp, 3.dp, 6.dp, 10.dp
        ) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), content = child)
        }
    }

    /**
     * Tests the top-level [absolutePadding] modifier factory with different values for left, top,
     * right and bottom paddings, checking that this padding is applied as expected when plenty of
     * space is available for both the content and padding.
     */
    @Test
    fun absolutePaddingAppliedToChild() {
        val paddingLeft = 10.dp
        val paddingTop = 15.dp
        val paddingRight = 20.dp
        val paddingBottom = 30.dp
        val padding = Modifier.absolutePadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        testPaddingWithDifferentInsetsImplementation(
            paddingLeft,
            paddingTop,
            paddingRight,
            paddingBottom
        ) { child: @Composable () -> Unit ->
            TestBox(modifier = padding, content = child)
        }
    }

    /**
     * Tests the top-level [absolutePadding] modifier factory with a single [PaddingValues.Absolute]
     * argument, checking that padding is applied to a child when plenty of space
     * is available for both content and padding.
     */
    @Test
    fun paddingAbsolutePaddingValuesAppliedToChild() = with(density) {
        val padding = PaddingValues.Absolute(left = 1.dp, top = 3.dp, right = 6.dp, bottom = 10.dp)
        testPaddingWithDifferentInsetsImplementation(
            1.dp, 3.dp, 6.dp, 10.dp
        ) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), content = child)
        }
    }

    /**
     * Tests the result of the [padding] modifier factory when not enough space is
     * available to accommodate both the padding and the content. In this case, the padding
     * should still be applied, modifying the final position of the content by its left and top
     * paddings even if it would result in constraints that the child content is unable or
     * unwilling to satisfy.
     */
    @Test
    fun insufficientSpaceAvailable() = with(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), content = child)
        }
    }

    @Test
    fun intrinsicMeasurements() = with(density) {
        val padding = 100.toDp()

        val latch = CountDownLatch(1)
        var error: Throwable? = null
        testIntrinsics(
            @Composable {
                TestBox(modifier = Modifier.padding(padding)) {
                    Container(Modifier.aspectRatio(2f)) { }
                }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Spacing is applied on both sides of an axis
            val totalAxisSpacing = (padding * 2).roundToPx()

            // When the width/height is measured as 3 x the padding
            val testDimension = (padding * 3).roundToPx()
            // The actual dimension for the AspectRatio will be: test dimension - total padding
            val actualAspectRatioDimension = testDimension - totalAxisSpacing

            // When we measure the width first, the height will be half
            val expectedAspectRatioHeight = (actualAspectRatioDimension / 2f).roundToInt()
            // When we measure the height first, the width will be double
            val expectedAspectRatioWidth = actualAspectRatioDimension * 2

            // Add back the padding on both sides to get the total expected height
            val expectedTotalHeight = expectedAspectRatioHeight + totalAxisSpacing
            // Add back the padding on both sides to get the total expected height
            val expectedTotalWidth = expectedAspectRatioWidth + totalAxisSpacing

            try {
                // Min width.
                assertEquals(totalAxisSpacing, minIntrinsicWidth(0.dp.roundToPx()))
                assertEquals(expectedTotalWidth, minIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicWidth(Constraints.Infinity))
                // Min height.
                assertEquals(totalAxisSpacing, minIntrinsicHeight(0.dp.roundToPx()))
                assertEquals(expectedTotalHeight, minIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicHeight(Constraints.Infinity))
                // Max width.
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(0.dp.roundToPx()))
                assertEquals(expectedTotalWidth, maxIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(Constraints.Infinity))
                // Max height.
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(0.dp.roundToPx()))
                assertEquals(expectedTotalHeight, maxIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(Constraints.Infinity))
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        error?.let { throw it }

        Unit
    }

    @Test
    fun testPadding_rtl() = with(density) {
        val sizeDp = 100.toDp()
        val size = sizeDp.roundToPx()
        val padding1Dp = 5.dp
        val padding2Dp = 10.dp
        val padding3Dp = 15.dp
        val padding1 = padding1Dp.roundToPx()
        val padding2 = padding2Dp.roundToPx()
        val padding3 = padding3Dp.roundToPx()

        val drawLatch = CountDownLatch(3)
        val childSize = Array(3) { IntSize(0, 0) }
        val childPosition = Array(3) { Offset(0f, 0f) }

        // ltr: P1 S P2 | S P3 | P1 S
        // rtl:    S P1 | P3 S | P2 S P1
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.padding(start = padding1Dp, end = padding2Dp)
                            .size(sizeDp, sizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Box(
                        Modifier.padding(end = padding3Dp)
                            .size(sizeDp, sizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }

                    Box(
                        Modifier.padding(start = padding1Dp)
                            .size(sizeDp, sizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize[2] = coordinates.size
                                childPosition[2] = coordinates.positionInRoot()
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
//        S P1 | P3 S | P2 S P1
        assertEquals(Offset((rootWidth - padding1 - size).toFloat(), 0f), childPosition[0])
        assertEquals(IntSize(size, size), childSize[0])

        assertEquals(
            Offset((rootWidth - padding1 - padding2 - size * 2).toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(IntSize(size, size), childSize[1])

        assertEquals(
            Offset((rootWidth - size * 3 - padding1 * 2 - padding2 - padding3).toFloat(), 0f),
            childPosition[2]
        )
        assertEquals(IntSize(size, size), childSize[2])
    }

    @Test
    fun testAbsolutePadding_rtl() = with(density) {
        val sizeDp = 100.toDp()
        val size = sizeDp.roundToPx()
        val padding1Dp = 5.dp
        val padding2Dp = 10.dp
        val padding3Dp = 15.dp
        val padding1 = padding1Dp.roundToPx()
        val padding2 = padding2Dp.roundToPx()
        val padding3 = padding3Dp.roundToPx()

        val drawLatch = CountDownLatch(2)
        val childPosition = Array(2) { Offset(0f, 0f) }

        // ltr: P1 S P2 | S P3
        // rtl:    S P3 | P1 S P2
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.absolutePadding(left = padding1Dp, right = padding2Dp)
                            .size(sizeDp, sizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childPosition[0] = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                    ) {
                    }
                    Box(
                        Modifier.absolutePadding(right = padding3Dp)
                            .size(sizeDp, sizeDp)
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
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

        assertEquals(Offset((rootWidth - padding2 - size).toFloat(), 0f), childPosition[0])

        assertEquals(
            Offset((rootWidth - size * 2 - padding1 - padding2 - padding3).toFloat(), 0f),
            childPosition[1]
        )
    }

    @Test
    fun testPaddingValuesRtl() = with(density) {
        val latch = CountDownLatch(1)
        val boxSize = 10
        val startPadding = 1
        val endPadding = 2
        show {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(
                    Modifier
                        .size(boxSize.toDp())
                        .padding(
                            PaddingValues(
                                start = startPadding.toDp(),
                                end = endPadding.toDp()
                            )
                        ).onGloballyPositioned {
                            assertEquals(boxSize - startPadding - endPadding, it.size.width)
                            latch.countDown()
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testInspectableParameter() {
        val modifier = Modifier.padding(10.dp, 20.dp, 30.dp, 40.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("padding")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.toList()).containsExactly(
            ValueElement("start", 10.dp),
            ValueElement("top", 20.dp),
            ValueElement("end", 30.dp),
            ValueElement("bottom", 40.dp)
        )
    }

    @Test
    fun testInspectableParameterWith2Parameters() {
        val modifier = Modifier.padding(10.dp, 20.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("padding")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.toList()).containsExactly(
            ValueElement("horizontal", 10.dp),
            ValueElement("vertical", 20.dp)
        )
    }

    @Test
    fun testInspectableParameterForAbsolute() {
        val modifier = Modifier.absolutePadding(10.dp, 20.dp, 30.dp, 40.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("absolutePadding")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.toList()).containsExactly(
            ValueElement("left", 10.dp),
            ValueElement("top", 20.dp),
            ValueElement("right", 30.dp),
            ValueElement("bottom", 40.dp)
        )
    }

    @Test
    fun testInspectableParameterWithSameOverallValue() {
        val modifier = Modifier.padding(40.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("padding")
        assertThat(modifier.valueOverride).isEqualTo(40.dp)
        assertThat(modifier.inspectableElements.toList()).isEmpty()
    }

    private fun testPaddingIsAppliedImplementation(
        padding: Dp,
        paddingContainer: @Composable (@Composable () -> Unit) -> Unit
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()
        val paddingPx = padding.roundToPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Box(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    val content = @Composable {
                        Container(
                            Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                    paddingContainer(content)
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        val innerSize = (size - paddingPx * 2)
        assertEquals(IntSize(innerSize, innerSize), childSize)
        val left = ((root.width - size) / 2f).roundToInt() + paddingPx
        val top = ((root.height - size) / 2f).roundToInt() + paddingPx
        assertEquals(
            Offset(left.toFloat(), top.toFloat()),
            childPosition
        )
    }

    private fun testPaddingWithDifferentInsetsImplementation(
        left: Dp,
        top: Dp,
        right: Dp,
        bottom: Dp,
        paddingContainer: @Composable ((@Composable () -> Unit) -> Unit)
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Box(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    val content = @Composable {
                        Container(
                            Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                    paddingContainer(content)
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        val paddingLeft = left.roundToPx()
        val paddingRight = right.roundToPx()
        val paddingTop = top.roundToPx()
        val paddingBottom = bottom.roundToPx()
        assertEquals(
            IntSize(
                size - paddingLeft - paddingRight,
                size - paddingTop - paddingBottom
            ),
            childSize
        )
        val viewLeft = ((root.width - size) / 2f).roundToInt() + paddingLeft
        val viewTop = ((root.height - size) / 2f).roundToInt() + paddingTop
        assertEquals(
            Offset(viewLeft.toFloat(), viewTop.toFloat()),
            childPosition
        )
    }

    private fun testPaddingWithInsufficientSpaceImplementation(
        padding: Dp,
        paddingContainer: @Composable (@Composable () -> Unit) -> Unit
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.roundToPx()
        val paddingPx = padding.roundToPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Box(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    paddingContainer {
                        Container(
                            Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.positionInRoot()
                                drawLatch.countDown()
                            }
                        ) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findComposeView()
        waitForDraw(root)

        assertEquals(IntSize(0, 0), childSize)
        val left = ((root.width - size) / 2f).roundToInt() + paddingPx
        val top = ((root.height - size) / 2f).roundToInt() + paddingPx
        assertEquals(Offset(left.toFloat(), top.toFloat()), childPosition)
    }

    /**
     * A trivial layout that applies a [Modifier] and measures/lays out a single child
     * with the same constraints it received.
     */
    @Composable
    private fun TestBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content = content, modifier = modifier) { measurables, constraints ->
            require(measurables.size == 1) {
                "TestBox received ${measurables.size} children; must have exactly 1"
            }
            val placeable = measurables.first().measure(constraints)
            layout(
                placeable.width.coerceAtMost(constraints.maxWidth),
                placeable.height.coerceAtMost(constraints.maxHeight)
            ) {
                placeable.placeRelative(0, 0)
            }
        }
    }
}
