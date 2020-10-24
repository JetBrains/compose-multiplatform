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
import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.HorizontalAlignmentLine
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.VerticalAlignmentLine
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min

@SmallTest
@RunWith(AndroidJUnit4::class)
class AlignmentLineTest : LayoutTest() {
    @Test
    fun testRelativePaddingFrom_vertical() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val beforeDp = 20f.toDp()
        val afterDp = 40f.toDp()
        val childDp = 30f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(
                Modifier.onGloballyPositioned {
                    parentSize.value = it.size
                    layoutLatch.countDown()
                }
            ) {
                AlignmentLineLayout(
                    childDp, 0.dp, testLine, lineDp,
                    Modifier.onGloballyPositioned {
                        childSize.value = it.size
                        childPosition.value = it.positionInRoot
                        layoutLatch.countDown()
                    }.relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(
            beforeDp.toIntPx() + afterDp.toIntPx(),
            parentSize.value!!.width
        )
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(
            (beforeDp.toIntPx() - lineDp.toIntPx()).toFloat(),
            childPosition.value!!.x
        )
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val beforeDp = 20f.toDp()
        val afterDp = 40f.toDp()
        val childDp = 30f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(
                modifier = Modifier.onGloballyPositioned {
                    parentSize.value = it.size
                    layoutLatch.countDown()
                }
            ) {
                AlignmentLineLayout(
                    0.dp, childDp, testLine, lineDp,
                    Modifier.onGloballyPositioned {
                        childSize.value = it.size
                        childPosition.value = it.positionInRoot
                        layoutLatch.countDown()
                    }.relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(beforeDp.toIntPx() + afterDp.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(
            (beforeDp.toIntPx() - lineDp.toIntPx()).toFloat(),
            childPosition.value!!.y
        )
    }

    @Test
    fun testRelativePaddingFrom_vertical_withSmallOffsets() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val beforeDp = 5f.toDp()
        val afterDp = 5f.toDp()
        val childDp = 30f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(modifier = Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                AlignmentLineLayout(
                    childDp, 0.dp, testLine, lineDp,
                    Modifier.saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal_withSmallOffsets() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val beforeDp = 5f.toDp()
        val afterDp = 5f.toDp()
        val childDp = 30f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                AlignmentLineLayout(
                    0.dp, childDp, testLine, lineDp,
                    Modifier.saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value, parentSize.value)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_vertical_withInsufficientSpace() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = VerticalAlignmentLine(::min)
        val maxWidth = 30f.toDp()
        val beforeDp = 20f.toDp()
        val afterDp = 20f.toDp()
        val childDp = 25f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                AlignmentLineLayout(
                    childDp, 0.dp, testLine, lineDp,
                    Modifier.preferredSizeIn(maxWidth = maxWidth)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxWidth.toIntPx(), parentSize.value!!.width)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(5f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_horizontal_withInsufficientSpace() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val testLine = HorizontalAlignmentLine(::min)
        val maxHeight = 30f.toDp()
        val beforeDp = 20f.toDp()
        val afterDp = 20f.toDp()
        val childDp = 25f.toDp()
        val lineDp = 10f.toDp()

        val parentSize = Ref<IntSize>()
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()
        show {
            Box(Modifier.saveLayoutInfo(parentSize, Ref(), layoutLatch)) {
                AlignmentLineLayout(
                    0.dp, childDp, testLine, lineDp,
                    Modifier.preferredSizeIn(maxHeight = maxHeight)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                        .relativePaddingFrom(testLine, beforeDp, afterDp)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxHeight.toIntPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(5f, childPosition.value!!.y)
    }

    @Test
    fun testRelativePaddingFrom_vertical_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minHeight = 10.dp
        show {
            Box {
                WithConstraints(
                    Modifier
                        .preferredSizeIn(minHeight = minHeight)
                        .relativePaddingFrom(testLine)
                ) {
                    Assert.assertEquals(minHeight.toIntPx(), constraints.minHeight)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRelativePaddingFrom_horizontal_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minWidth = 10.dp
        show {
            Box {
                WithConstraints(
                    Modifier
                        .preferredSizeIn(minWidth = minWidth)
                        .relativePaddingFrom(testLine)
                ) {
                    Assert.assertEquals(minWidth.toIntPx(), constraints.minWidth)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRelativePaddingFrom_vertical_whenMinConstrained() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(2)
        val childSizePx = 20f
        val childSize = childSizePx.toDp()
        val linePositionPx = 10f
        val linePosition = linePositionPx.toDp()
        val beforePx = 20f
        val before = beforePx.toDp()
        val afterPx = 20f
        val after = afterPx.toDp()
        val incomingSizePx = 50f
        val incomingSize = incomingSizePx.toDp()

        show {
            Box {
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.preferredWidth(incomingSize)
                        .relativePaddingFrom(testLine, before = before)
                        .onGloballyPositioned {
                            Assert.assertEquals(beforePx - linePositionPx, it.positionInParent.x)
                            latch.countDown()
                        }
                )
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.preferredWidth(incomingSize)
                        .relativePaddingFrom(testLine, after = after)
                        .onGloballyPositioned {
                            Assert.assertEquals(
                                incomingSizePx - childSizePx - afterPx + linePositionPx,
                                it.positionInParent.x
                            )
                            latch.countDown()
                        }
                )
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRelativePaddingFrom_horizontal_whenMinConstrained() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(2)
        val childSizePx = 20f
        val childSize = childSizePx.toDp()
        val linePositionPx = 10f
        val linePosition = linePositionPx.toDp()
        val beforePx = 20f
        val before = beforePx.toDp()
        val afterPx = 20f
        val after = afterPx.toDp()
        val incomingSizePx = 50f
        val incomingSize = incomingSizePx.toDp()

        show {
            Box {
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.preferredHeight(incomingSize)
                        .relativePaddingFrom(testLine, before = before)
                        .onGloballyPositioned {
                            Assert.assertEquals(beforePx - linePositionPx, it.positionInParent.y)
                            latch.countDown()
                        }
                )
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.preferredHeight(incomingSize)
                        .relativePaddingFrom(testLine, after = after)
                        .onGloballyPositioned {
                            Assert.assertEquals(
                                incomingSizePx - childSizePx - afterPx + linePositionPx,
                                it.positionInParent.y
                            )
                            latch.countDown()
                        }
                )
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun AlignmentLineLayout(
        width: Dp,
        height: Dp,
        line: AlignmentLine,
        linePosition: Dp,
        modifier: Modifier
    ) {
        Layout({}, modifier) { _, _ ->
            layout(width.toIntPx(), height.toIntPx(), mapOf(line to linePosition.toIntPx())) {}
        }
    }
}
