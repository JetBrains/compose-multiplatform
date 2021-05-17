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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min

@SmallTest
@RunWith(AndroidJUnit4::class)
class AlignmentLineTest : LayoutTest() {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testPaddingFrom_vertical() = with(density) {
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
                    Modifier.paddingFrom(testLine, beforeDp, afterDp).onGloballyPositioned {
                        childSize.value = it.size
                        childPosition.value = it.positionInRoot()
                        layoutLatch.countDown()
                    }
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(
            beforeDp.roundToPx() + afterDp.roundToPx(),
            parentSize.value!!.width
        )
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(
            (beforeDp.roundToPx() - lineDp.roundToPx()).toFloat(),
            childPosition.value!!.x
        )
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testPaddingFrom_horizontal() = with(density) {
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
                    Modifier.paddingFrom(testLine, beforeDp, afterDp).onGloballyPositioned {
                        childSize.value = it.size
                        childPosition.value = it.positionInRoot()
                        layoutLatch.countDown()
                    }
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(beforeDp.roundToPx() + afterDp.roundToPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(
            (beforeDp.roundToPx() - lineDp.roundToPx()).toFloat(),
            childPosition.value!!.y
        )
    }

    @Test
    fun testPaddingFrom_vertical_withSmallOffsets() = with(density) {
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
                        .paddingFrom(testLine, beforeDp, afterDp)
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
    fun testPaddingFrom_horizontal_withSmallOffsets() = with(density) {
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
                    Modifier
                        .paddingFrom(testLine, beforeDp, afterDp)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
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
    fun testPaddingFrom_vertical_withInsufficientSpace() = with(density) {
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
                    Modifier.sizeIn(maxWidth = maxWidth)
                        .paddingFrom(testLine, beforeDp, afterDp)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxWidth.roundToPx(), parentSize.value!!.width)
        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.height, parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(5f, childPosition.value!!.x)
        Assert.assertEquals(0f, childPosition.value!!.y)
    }

    @Test
    fun testPaddingFrom_horizontal_withInsufficientSpace() = with(density) {
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
                    Modifier.sizeIn(maxHeight = maxHeight)
                        .paddingFrom(testLine, beforeDp, afterDp)
                        .saveLayoutInfo(childSize, childPosition, layoutLatch)
                )
            }
        }
        Assert.assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        Assert.assertNotNull(childSize.value)
        Assert.assertEquals(childSize.value!!.width, parentSize.value!!.width)
        Assert.assertNotNull(parentSize.value)
        Assert.assertEquals(maxHeight.roundToPx(), parentSize.value!!.height)
        Assert.assertNotNull(childPosition.value)
        Assert.assertEquals(0f, childPosition.value!!.x)
        Assert.assertEquals(5f, childPosition.value!!.y)
    }

    @Test
    fun testPaddingFrom_vertical_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = VerticalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minHeight = 10.dp
        show {
            Box {
                BoxWithConstraints(
                    Modifier
                        .sizeIn(minHeight = minHeight)
                        .paddingFrom(testLine, 0.dp)
                ) {
                    Assert.assertEquals(minHeight.roundToPx(), constraints.minHeight)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFrom_horizontal_keepsCrossAxisMinConstraints() = with(density) {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val minWidth = 10.dp
        show {
            Box {
                BoxWithConstraints(
                    Modifier
                        .sizeIn(minWidth = minWidth)
                        .paddingFrom(testLine, 0.dp)
                ) {
                    Assert.assertEquals(minWidth.roundToPx(), constraints.minWidth)
                    latch.countDown()
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFrom_vertical_whenMinConstrained() = with(density) {
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
                    Modifier.width(incomingSize)
                        .paddingFrom(testLine, before = before)
                        .onGloballyPositioned {
                            Assert.assertEquals(beforePx - linePositionPx, it.positionInParent().x)
                            latch.countDown()
                        }
                )
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.width(incomingSize)
                        .paddingFrom(testLine, after = after)
                        .onGloballyPositioned {
                            Assert.assertEquals(
                                incomingSizePx - childSizePx - afterPx + linePositionPx,
                                it.positionInParent().x
                            )
                            latch.countDown()
                        }
                )
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFrom_horizontal_whenMinConstrained() = with(density) {
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
                    Modifier.height(incomingSize)
                        .paddingFrom(testLine, before = before)
                        .onGloballyPositioned {
                            Assert.assertEquals(beforePx - linePositionPx, it.positionInParent().y)
                            latch.countDown()
                        }
                )
                AlignmentLineLayout(
                    childSize, childSize, testLine, linePosition,
                    Modifier.height(incomingSize)
                        .paddingFrom(testLine, after = after)
                        .onGloballyPositioned {
                            Assert.assertEquals(
                                incomingSizePx - childSizePx - afterPx + linePositionPx,
                                it.positionInParent().y
                            )
                            latch.countDown()
                        }
                )
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFromBaseline() = with(density) {
        val sizePx = 30
        val sizeDp = sizePx.toDp()
        val baselineOffsetPx = 10
        val baselineOffsetDp = baselineOffsetPx.toDp()
        val paddingPx = 20
        val paddingDp = paddingPx.toDp()
        val latch = CountDownLatch(1)

        show {
            Box(
                Modifier.onSizeChanged { boxSize ->
                    Assert.assertEquals(
                        sizeDp.roundToPx() + (paddingPx - baselineOffsetPx) * 2,
                        boxSize.height
                    )
                    latch.countDown()
                }
            ) {
                Box(Modifier.paddingFromBaseline(paddingDp, paddingDp)) {
                    AlignmentLineLayout(sizeDp, sizeDp, FirstBaseline, baselineOffsetDp, Modifier)
                    AlignmentLineLayout(
                        sizeDp,
                        sizeDp,
                        LastBaseline,
                        sizeDp - baselineOffsetDp,
                        Modifier
                    )
                }
            }
        }

        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFromBaseline_textUnit() = with(density) {
        val sizePx = 30
        val sizeDp = sizePx.toDp()
        val baselineOffsetPx = 10
        val baselineOffsetDp = baselineOffsetPx.toDp()
        val paddingPx = 20
        val paddingSp = paddingPx.toSp()
        val latch = CountDownLatch(1)

        show {
            Box(
                Modifier.onSizeChanged { boxSize ->
                    Assert.assertEquals(
                        sizeDp.roundToPx() + paddingPx - baselineOffsetPx,
                        boxSize.height
                    )
                    latch.countDown()
                }
            ) {
                Box(Modifier.paddingFromBaseline(paddingSp)) {
                    AlignmentLineLayout(sizeDp, sizeDp, FirstBaseline, baselineOffsetDp, Modifier)
                }
            }
        }

        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testPaddingFromBaseline_whenMinConstrained() = with(density) {
        val latch = CountDownLatch(1)
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
                Box(
                    Modifier.height(incomingSize)
                        .paddingFromBaseline(top = before, bottom = after)
                        .onGloballyPositioned {
                            Assert.assertEquals(beforePx - linePositionPx, it.positionInParent().y)
                            latch.countDown()
                        }
                ) {
                    AlignmentLineLayout(
                        childSize, childSize, FirstBaseline, linePosition, Modifier
                    )
                    AlignmentLineLayout(
                        childSize, childSize, LastBaseline, linePosition, Modifier
                    )
                }
            }
        }
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testInspectableParameter() {
        val alignment = VerticalAlignmentLine(::min)
        val modifier = Modifier.paddingFrom(alignment, before = 2.0.dp)
            as InspectableValue
        Truth.assertThat(modifier.nameFallback).isEqualTo("paddingFrom")
        Truth.assertThat(modifier.valueOverride).isNull()
        Truth.assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("alignmentLine", alignment),
            ValueElement("before", 2.0.dp),
            ValueElement("after", Dp.Unspecified)
        )
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
            layout(
                width.roundToPx(),
                height.roundToPx(),
                mapOf(line to linePosition.roundToPx())
            ) {}
        }
    }
}
