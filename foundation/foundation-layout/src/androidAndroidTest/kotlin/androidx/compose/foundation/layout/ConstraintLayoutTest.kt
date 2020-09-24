/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import androidx.ui.test.createComposeRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalLayout::class)
class ConstraintLayoutTest : LayoutTest() {
    @get:Rule
    val rule = createComposeRule()

    // region sizing tests

    @Test
    fun dividerMatchTextHeight_spread() = with(density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .preferredWidth((rule.displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }.onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (rule.displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (rule.displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has spread height so it should spread to fill the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_spread_withPreferredWrapHeightText() = with(density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.preferredWrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .preferredWidth((rule.displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }.onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (rule.displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (rule.displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has spread height so it should spread to fill the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_percent() = with(density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .preferredWidth((rule.displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.percent(0.8f)
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (rule.displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (rule.displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(
                (aspectRatioBoxSize.value!!.height * 0.8f).roundToInt(),
                dividerSize.value!!.height
            )
        }
    }

    @Test
    fun dividerMatchTextHeight_inWrapConstraintLayout_longText() = with(density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            // CL is wrap content.
            ConstraintLayout {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .preferredWidth((rule.displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.percent(0.8f)
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (rule.displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (rule.displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            // TODO(popam; b/150277566): uncomment
            assertEquals(
                (aspectRatioBoxSize.value!!.height * 0.8f).roundToInt(),
                dividerSize.value!!.height
            )
        }
    }

    @Test
    fun dividerMatchTextHeight_inWrapConstraintLayout_shortText() = with(density) {
        val constraintLayoutSize = Ref<IntSize>()
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        val size = 40.toDp()
        rule.setContent {
            ConstraintLayout(
                // CL is wrapping width and height.
                modifier = Modifier.onGloballyPositioned {
                    constraintLayoutSize.value = it.size
                }
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Small width for the CL to wrap it.
                        .preferredWidth(size)
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The width of the ConstraintLayout should be twice the width of the aspect ratio box.
            assertEquals(size.toIntPx() * 2, constraintLayoutSize.value!!.width)
            // The height of the ConstraintLayout should be the height of the aspect ratio box.
            assertEquals(size.toIntPx() / 2, constraintLayoutSize.value!!.height)
            // The aspect ratio gets the requested size.
            assertEquals(size.toIntPx(), aspectRatioBoxSize.value!!.width)
            // Aspect ratio is preserved.
            assertEquals(size.toIntPx() / 2, aspectRatioBoxSize.value!!.height)
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider should have the height of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    // endregion

    // region positioning tests

    @Test
    fun testConstraintLayout_withInlineDSL() = with(density) {
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            ConstraintLayout(Modifier.fillMaxSize()) {
                val (box0, box1, box2) = createRefs()
                Box(
                    Modifier
                        .constrainAs(box0) {
                            centerTo(parent)
                        }
                        .preferredSize(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[0].value = it.positionInRoot
                        }
                )
                val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                Box(
                    Modifier
                        .constrainAs(box1) {
                            start.linkTo(half, margin = offset.toDp())
                            bottom.linkTo(box0.top)
                        }
                        .preferredSize(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[1].value = it.positionInRoot
                        }
                )
                Box(
                    Modifier
                        .constrainAs(box2) {
                            start.linkTo(parent.start, margin = offset.toDp())
                            bottom.linkTo(parent.bottom, margin = offset.toDp())
                        }
                        .preferredSize(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[2].value = it.positionInRoot
                        }
                )
            }
        }

        val displayWidth = rule.displaySize.width
        val displayHeight = rule.displaySize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    ((displayWidth - boxSize) / 2).toFloat(),
                    ((displayHeight - boxSize) / 2).toFloat()
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2 + offset).toFloat(),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    offset.toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_withConstraintSet() = with(density) {
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            ConstraintLayout(
                ConstraintSet {
                    val box0 = createRefFor("box0")
                    val box1 = createRefFor("box1")
                    val box2 = createRefFor("box2")

                    constrain(box0) {
                        centerTo(parent)
                    }

                    val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                    constrain(box1) {
                        start.linkTo(half, margin = offset.toDp())
                        bottom.linkTo(box0.top)
                    }

                    constrain(box2) {
                        start.linkTo(parent.start, margin = offset.toDp())
                        bottom.linkTo(parent.bottom, margin = offset.toDp())
                    }
                },
                Modifier.fillMaxSize()
            ) {
                for (i in 0..2) {
                    Box(
                        Modifier.layoutId("box$i").preferredSize(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[i].value = it.positionInRoot
                            }
                    )
                }
            }
        }

        val displayWidth = rule.displaySize.width
        val displayHeight = rule.displaySize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    (displayWidth - boxSize) / 2f,
                    (displayHeight - boxSize) / 2f
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2f + offset).toFloat(),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    offset.toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_rtl() = with(density) {
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.fillMaxSize()) {
                    val (box0, box1, box2) = createRefs()
                    Box(
                        Modifier
                            .constrainAs(box0) {
                                centerTo(parent)
                            }
                            .preferredSize(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[0].value = it.positionInRoot
                            }
                    )
                    val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                    Box(
                        Modifier
                            .constrainAs(box1) {
                                start.linkTo(half, margin = offset.toDp())
                                bottom.linkTo(box0.top)
                            }
                            .preferredSize(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[1].value = it.positionInRoot
                            }
                    )
                    Box(
                        Modifier
                            .constrainAs(box2) {
                                start.linkTo(parent.start, margin = offset.toDp())
                                bottom.linkTo(parent.bottom, margin = offset.toDp())
                            }
                            .preferredSize(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[2].value = it.positionInRoot
                            }
                    )
                }
            }
        }

        val displayWidth = rule.displaySize.width
        val displayHeight = rule.displaySize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    (displayWidth - boxSize) / 2f,
                    (displayHeight - boxSize) / 2f
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2 - offset - boxSize).toFloat(),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    (displayWidth - offset - boxSize).toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_helpers_ltr() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(8) { 0f }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val guidelines = arrayOf(
                    createGuidelineFromStart(offset),
                    createGuidelineFromAbsoluteLeft(offset),
                    createGuidelineFromEnd(offset),
                    createGuidelineFromAbsoluteRight(offset),
                    createGuidelineFromStart(0.25f),
                    createGuidelineFromAbsoluteLeft(0.25f),
                    createGuidelineFromEnd(0.25f),
                    createGuidelineFromAbsoluteRight(0.25f)
                )

                guidelines.forEachIndexed { index, guideline ->
                    val ref = createRef()
                    Box(
                        Modifier.size(1.dp)
                            .constrainAs(ref) {
                                absoluteLeft.linkTo(guideline)
                            }.onGloballyPositioned {
                                position[index] = it.positionInParent.x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(150f, position[2])
            Assert.assertEquals(150f, position[3])
            Assert.assertEquals(50f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(150f, position[6])
            Assert.assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_helpers_rtl() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(8) { 0f }
        rule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.size(size)) {
                    val guidelines = arrayOf(
                        createGuidelineFromStart(offset),
                        createGuidelineFromAbsoluteLeft(offset),
                        createGuidelineFromEnd(offset),
                        createGuidelineFromAbsoluteRight(offset),
                        createGuidelineFromStart(0.25f),
                        createGuidelineFromAbsoluteLeft(0.25f),
                        createGuidelineFromEnd(0.25f),
                        createGuidelineFromAbsoluteRight(0.25f)
                    )

                    guidelines.forEachIndexed { index, guideline ->
                        val ref = createRef()
                        Box(
                            Modifier.size(1.dp)
                                .constrainAs(ref) {
                                    absoluteLeft.linkTo(guideline)
                                }.onGloballyPositioned {
                                    position[index] = it.positionInParent.x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(150f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(150f, position[3])
            Assert.assertEquals(150f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(50f, position[6])
            Assert.assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_barriers_ltr() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(4) { 0f }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val (box1, box2) = createRefs()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromAbsoluteRight(offset)
                Box(
                    Modifier.size(1.toDp())
                        .constrainAs(box1) {
                            absoluteLeft.linkTo(guideline1)
                        }
                )
                Box(
                    Modifier.size(1.toDp())
                        .constrainAs(box2) {
                            absoluteLeft.linkTo(guideline2)
                        }
                )

                val barriers = arrayOf(
                    createStartBarrier(box1, box2),
                    createAbsoluteLeftBarrier(box1, box2),
                    createEndBarrier(box1, box2),
                    createAbsoluteRightBarrier(box1, box2)
                )

                barriers.forEachIndexed { index, barrier ->
                    val ref = createRef()
                    Box(
                        Modifier.size(1.dp)
                            .constrainAs(ref) {
                                absoluteLeft.linkTo(barrier)
                            }.onGloballyPositioned {
                                position[index] = it.positionInParent.x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(151f, position[2])
            Assert.assertEquals(151f, position[3])
        }
    }

    @Test
    fun testConstraintLayout_barriers_rtl() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(4) { 0f }
        rule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.size(size)) {
                    val (box1, box2) = createRefs()
                    val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                    val guideline2 = createGuidelineFromAbsoluteRight(offset)
                    Box(
                        Modifier.size(1.toDp())
                            .constrainAs(box1) {
                                absoluteLeft.linkTo(guideline1)
                            }
                    )
                    Box(
                        Modifier.size(1.toDp())
                            .constrainAs(box2) {
                                absoluteLeft.linkTo(guideline2)
                            }
                    )

                    val barriers = arrayOf(
                        createStartBarrier(box1, box2),
                        createAbsoluteLeftBarrier(box1, box2),
                        createEndBarrier(box1, box2),
                        createAbsoluteRightBarrier(box1, box2)
                    )

                    barriers.forEachIndexed { index, barrier ->
                        val ref = createRef()
                        Box(
                            Modifier.size(1.dp)
                                .constrainAs(ref) {
                                    absoluteLeft.linkTo(barrier)
                                }.onGloballyPositioned {
                                    position[index] = it.positionInParent.x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(151f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(151f, position[3])
        }
    }

    @Test
    fun testConstraintLayout_anchors_ltr() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(16) { 0f }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline = createGuidelineFromAbsoluteLeft(offset)
                Box(
                    Modifier.size(1.toDp())
                        .constrainAs(box) {
                            absoluteLeft.linkTo(guideline)
                        }
                )

                val anchors = listOf<ConstrainScope.() -> Unit>(
                    { start.linkTo(box.start) },
                    { absoluteLeft.linkTo(box.start) },
                    { start.linkTo(box.absoluteLeft) },
                    { absoluteLeft.linkTo(box.absoluteLeft) },
                    { end.linkTo(box.start) },
                    { absoluteRight.linkTo(box.start) },
                    { end.linkTo(box.absoluteLeft) },
                    { absoluteRight.linkTo(box.absoluteLeft) },
                    { start.linkTo(box.end) },
                    { absoluteLeft.linkTo(box.end) },
                    { start.linkTo(box.absoluteRight) },
                    { absoluteLeft.linkTo(box.absoluteRight) },
                    { end.linkTo(box.end) },
                    { absoluteRight.linkTo(box.end) },
                    { end.linkTo(box.absoluteRight) },
                    { absoluteRight.linkTo(box.absoluteRight) }
                )

                anchors.forEachIndexed { index, anchor ->
                    val ref = createRef()
                    Box(
                        Modifier.size(1.toDp())
                            .constrainAs(ref) {
                                anchor()
                            }.onGloballyPositioned {
                                position[index] = it.positionInParent.x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(50f, position[3])
            Assert.assertEquals(49f, position[4])
            Assert.assertEquals(49f, position[5])
            Assert.assertEquals(49f, position[6])
            Assert.assertEquals(49f, position[7])
            Assert.assertEquals(51f, position[8])
            Assert.assertEquals(51f, position[9])
            Assert.assertEquals(51f, position[10])
            Assert.assertEquals(51f, position[11])
            Assert.assertEquals(50f, position[12])
            Assert.assertEquals(50f, position[13])
            Assert.assertEquals(50f, position[14])
            Assert.assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_anchors_rtl() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(16) { 0f }
        rule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.size(size)) {
                    val box = createRef()
                    val guideline = createGuidelineFromAbsoluteLeft(offset)
                    Box(
                        Modifier.size(1.toDp())
                            .constrainAs(box) {
                                absoluteLeft.linkTo(guideline)
                            }
                    )

                    val anchors = listOf<ConstrainScope.() -> Unit>(
                        { start.linkTo(box.start) },
                        { absoluteLeft.linkTo(box.start) },
                        { start.linkTo(box.absoluteLeft) },
                        { absoluteLeft.linkTo(box.absoluteLeft) },
                        { end.linkTo(box.start) },
                        { absoluteRight.linkTo(box.start) },
                        { end.linkTo(box.absoluteLeft) },
                        { absoluteRight.linkTo(box.absoluteLeft) },
                        { start.linkTo(box.end) },
                        { absoluteLeft.linkTo(box.end) },
                        { start.linkTo(box.absoluteRight) },
                        { absoluteLeft.linkTo(box.absoluteRight) },
                        { end.linkTo(box.end) },
                        { absoluteRight.linkTo(box.end) },
                        { end.linkTo(box.absoluteRight) },
                        { absoluteRight.linkTo(box.absoluteRight) }
                    )

                    anchors.forEachIndexed { index, anchor ->
                        val ref = createRef()
                        Box(
                            Modifier.size(1.toDp())
                                .constrainAs(ref) {
                                    anchor()
                                }.onGloballyPositioned {
                                    position[index] = it.positionInParent.x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(51f, position[1])
            Assert.assertEquals(49f, position[2])
            Assert.assertEquals(50f, position[3])
            Assert.assertEquals(51f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(50f, position[6])
            Assert.assertEquals(49f, position[7])
            Assert.assertEquals(49f, position[8])
            Assert.assertEquals(50f, position[9])
            Assert.assertEquals(50f, position[10])
            Assert.assertEquals(51f, position[11])
            Assert.assertEquals(50f, position[12])
            Assert.assertEquals(49f, position[13])
            Assert.assertEquals(51f, position[14])
            Assert.assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_barriers_margins() = with(density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(2) { Offset(0f, 0f) }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromTop(offset)
                Box(
                    Modifier.size(1.toDp())
                        .constrainAs(box) {
                            absoluteLeft.linkTo(guideline1)
                            top.linkTo(guideline2)
                        }
                )

                val leftBarrier = createAbsoluteLeftBarrier(box, margin = 10.toDp())
                val topBarrier = createTopBarrier(box, margin = 10.toDp())
                val rightBarrier = createAbsoluteRightBarrier(box, margin = 10.toDp())
                val bottomBarrier = createBottomBarrier(box, margin = 10.toDp())

                Box(
                    Modifier.size(1.dp)
                        .constrainAs(createRef()) {
                            absoluteLeft.linkTo(leftBarrier)
                            top.linkTo(topBarrier)
                        }.onGloballyPositioned {
                            position[0] = it.positionInParent
                        }
                )

                Box(
                    Modifier.size(1.dp)
                        .constrainAs(createRef()) {
                            absoluteLeft.linkTo(rightBarrier)
                            top.linkTo(bottomBarrier)
                        }.onGloballyPositioned {
                            position[1] = it.positionInParent
                        }
                )
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(Offset(60f, 60f), position[0])
            Assert.assertEquals(Offset(61f, 61f), position[1])
        }
    }

    @Test
    fun links_canBeOverridden() = with(density) {
        rule.setContent {
            ConstraintLayout(Modifier.width(10.dp)) {
                val box = createRef()
                Box(
                    Modifier.constrainAs(box) {
                        start.linkTo(parent.end)
                        start.linkTo(parent.start)
                    }.onGloballyPositioned {
                        Assert.assertEquals(0f, it.positionInParent.x)
                    }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun chains_defaultOutsideConstraintsCanBeOverridden() = with(density) {
        val size = 100.toDp()
        val boxSize = 10.toDp()
        val guidelinesOffset = 20.toDp()
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val (box1, box2) = createRefs()
                val startGuideline = createGuidelineFromStart(guidelinesOffset)
                val topGuideline = createGuidelineFromTop(guidelinesOffset)
                val endGuideline = createGuidelineFromEnd(guidelinesOffset)
                val bottomGuideline = createGuidelineFromBottom(guidelinesOffset)
                createHorizontalChain(box1, box2, chainStyle = ChainStyle.SpreadInside)
                createVerticalChain(box1, box2, chainStyle = ChainStyle.SpreadInside)
                Box(
                    Modifier.size(boxSize).constrainAs(box1) {
                        start.linkTo(startGuideline)
                        top.linkTo(topGuideline)
                    }.onGloballyPositioned {
                        Assert.assertEquals(20f, it.boundsInParent.left)
                        Assert.assertEquals(20f, it.boundsInParent.top)
                    }
                )
                Box(
                    Modifier.size(boxSize).constrainAs(box2) {
                        end.linkTo(endGuideline)
                        bottom.linkTo(bottomGuideline)
                    }.onGloballyPositioned {
                        Assert.assertEquals(80f, it.boundsInParent.right)
                        Assert.assertEquals(80f, it.boundsInParent.bottom)
                    }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_inlineDSL_recompositionDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout {
                val box = createRef()
                if (first.value) {
                    Box(Modifier.constrainAs(box) { })
                } else {
                    Box(Modifier.constrainAs(box) { })
                }
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_ConstraintSetDSL_recompositionDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(
                ConstraintSet {
                    val box = createRefFor("box")
                    constrain(box) { }
                }
            ) {
                if (first.value) {
                    Box(Modifier.layoutId("box"))
                } else {
                    Box(Modifier.layoutId("box"))
                }
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_inlineDSL_remeasureDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(if (first.value) Modifier else Modifier.padding(10.dp)) {
                Box(if (first.value) Modifier else Modifier.size(20.dp))
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_ConstraintSetDSL_remeasureDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(
                modifier = if (first.value) Modifier else Modifier.padding(10.dp),
                constraintSet = ConstraintSet { }
            ) {
                Box(if (first.value) Modifier else Modifier.size(20.dp))
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }
}
