/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLayoutApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FlowRowColumnTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testFlowRow_wrapsToTheNextLine() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_wrapsToTheNextLine() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLine_withExactSpaceNeeded() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(10) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_wrapsToTheNextLine_withExactSpaceNeeded() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(10) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLineMultipleTimes() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_wrapsToTheNextLineMultipleTimes() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsWithMaxItems() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_wrapsWithMaxItems() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_wrapsWithWeights() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2) {
                        repeat(6) {
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .weight(1f, true))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_wrapsWithWeights() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2) {
                        repeat(6) {
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .weight(1f, true))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_staysInOneRow() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(2) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_staysInOneRow() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(2) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLine_Rounding() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowRow(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(3) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_wrapsToTheNextLine_Rounding() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowColumn(
                        Modifier
                            .wrapContentHeight()
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(3) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_centerVertically() {

        val totalRowHeight = 20
        val shorterHeight = 10
        val expectedResult = (totalRowHeight - shorterHeight) / 2
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        Modifier.wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(),
                                        if (index == 4) {
                                            shorterHeight.toDp()
                                        } else {
                                            totalRowHeight.toDp()
                                        }
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowColumn_centerHorizontally() {

        val totalColumnWidth = 20
        val shorterWidth = 10
        val expectedResult = (totalColumnWidth - shorterWidth) / 2
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(
                        Modifier.wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            shorterWidth.toDp()
                                        } else {
                                            totalColumnWidth.toDp()
                                        },
                                        20.toDp()
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowRow_horizontalArrangementSpaceAround() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()
        //  ----
        //      * Visually: #1##2##3# for LTR and #3##2##1# for RTL
        // --(front) - (back) --

        val xPositions = FloatArray(noOfItemsPerRow)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(Modifier.wrapContentHeight().fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEach {
            val xPosition = it
            expectedXPosition += gapSize
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementSpaceAround() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()

        val yPositions = FloatArray(noOfItemsPerRow)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(Modifier.wrapContentWidth().fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        yPositions[index] = yPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEach {
            val yPosition = it
            expectedYPosition += gapSize
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
            expectedYPosition += gapSize
        }
    }

    @Test
    fun testFlowRow_horizontalArrangementSpaceAround_withTwoRows() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()
        //  ----
        //      * Visually: #1##2##3# for LTR and #3##2##1# for RTL
        // --(front) - (back) --

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(Modifier.wrapContentHeight().fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachRow = 5
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, xPosition ->
            if (index % 5 == 0) {
                expectedXPosition = 0
            }
            expectedXPosition += gapSize
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementSpaceAround_withTwoColumns() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(Modifier.wrapContentWidth().fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachColumn = 5
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        yPositions[index] = yPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            expectedYPosition += gapSize
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
            expectedYPosition += gapSize
        }
    }

    @Test
    fun testFlowRow_horizontalArrangementEnd() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val gapSize = spaceAvailable.roundToInt()
       //  * Visually: ####123

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(Modifier.wrapContentHeight().fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.End,
                        maxItemsInEachRow = 5
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = gapSize
        xPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedXPosition = gapSize
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementBottom() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val gapSize = spaceAvailable.roundToInt()

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(Modifier.fillMaxHeight(1f).wrapContentWidth(),
                        verticalArrangement = Arrangement.Bottom,
                        maxItemsInEachColumn = 5
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        yPositions[index] = yPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = gapSize
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = gapSize
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
        }
    }
    @Test
    fun testFlowRow_horizontalArrangementStart() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually: 123####

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(Modifier.wrapContentHeight(),
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
            }
        }
    }

    @Test
    fun testFlowRow_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(Modifier.wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            if (index % maxItemsInMainAxis == 0) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
                expectedXPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
        }
    }

    @Test
    fun testFlowColumn_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val position = positionInParent.y
                                        yPositions[index] = position
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % maxItemsInMainAxis == 0) {
                expectedYPosition = 0
            } else {
                expectedYPosition += eachSize
                expectedYPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
        }
    }

    @Test
    fun testFlowRow_SpaceAligned_notExact() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10
        val noOfItemsThatCanFit = 2

        var width = 0
        val expectedWidth = 30
        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.wrapContentHeight().widthIn(30.toDp(), 40.toDp())) {
                    FlowRow(Modifier.wrapContentHeight().onSizeChanged {
                           width = it.width
                    },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(expectedWidth)
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            if (index % noOfItemsThatCanFit == 0) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
                expectedXPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
        }
    }

    @Test
    fun testFlowColumn_SpaceAligned_notExact() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10
        val noOfItemsThatCanFit = 2

        var height = 0
        val expectedHeight = 30
        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.heightIn(30.toDp(), 40.toDp()).wrapContentWidth()) {
                    FlowColumn(Modifier.wrapContentHeight().onSizeChanged {
                        height = it.height
                    },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        yPositions[index] = yPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(expectedHeight)
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % noOfItemsThatCanFit == 0) {
                expectedYPosition = 0
            } else {
                expectedYPosition += eachSize
                expectedYPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementTop() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(size.toDp())) {
                    FlowColumn(Modifier.fillMaxHeight(1f).wrapContentWidth(),
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        yPositions[index] = yPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
        }
    }

    @Test
    fun testFlowRow_horizontalArrangementStart_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            CompositionLocalProvider(values = arrayOf(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            )) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowRow(Modifier.wrapContentHeight().fillMaxWidth(1f),
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = maxItemsInMainAxis
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .size(eachSize.toDp())
                                        .onPlaced {
                                            val positionInParent = it.positionInParent()
                                            val xPosition = positionInParent.x
                                            xPositions[index] = xPosition
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = size.toInt() - eachSize
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = size.toInt() - eachSize
            } else {
                expectedXPosition -= eachSize
            }
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementTop_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val xYPositions = Array<Pair<Float, Float>>(10) { Pair(0f, 0f) }
        rule.setContent {
            CompositionLocalProvider(values = arrayOf(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            )) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowColumn(Modifier.fillMaxHeight(1f).fillMaxWidth(1f),
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                        .onPlaced {
                                            val positionInParent = it.positionInParent()
                                            val yPosition = positionInParent.y
                                            val xPosition = positionInParent.x
                                            xYPositions[index] = Pair(xPosition, yPosition)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        var expectedYPosition = 0
        var expectedXPosition = size.toInt() - eachSize
        for (index in xYPositions.indices) {
            val xPosition = xYPositions[index].first
            val yPosition = xYPositions[index].second
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition -= eachSize
            }
            expectedYPosition += eachSize
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementTop_rtl_wrapContentWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        var itemsThatCanFit = 0
        var width = 0
        val xYPositions = Array<Pair<Float, Float>>(10) { Pair(0f, 0f) }
        rule.setContent {
            CompositionLocalProvider(values = arrayOf(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            )) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowColumn(Modifier.fillMaxHeight(1f).wrapContentWidth()
                            .onSizeChanged {
                                width = it.width
                                itemsThatCanFit = it.height / eachSize
                        },
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                        .onPlaced {
                                            val positionInParent = it.positionInParent()
                                            val xPosition = positionInParent.x
                                            val yPosition = positionInParent.y
                                            xYPositions[index] = Pair(xPosition, yPosition)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        var expectedXPosition = width
        var fittedItems = 0
        for (index in xYPositions.indices) {
            val pair = xYPositions[index]
            val xPosition = pair.first
            val yPosition = pair.second
            if (index % maxItemsInMainAxis == 0 ||
                fittedItems == itemsThatCanFit) {
                expectedYPosition = 0
                expectedXPosition -= eachSize
                fittedItems = 0
            }
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedYPosition += eachSize
            fittedItems++
        }
    }

    @Test
    fun testFlowRow_horizontalArrangementStart_rtl_wrap() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxMainAxisSize = 100
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.wrapContentHeight().wrapContentWidth(),
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                        .onPlaced {
                                            val positionInParent = it.positionInParent()
                                            val xPosition = positionInParent.x
                                            xPositions[index] = xPosition
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        var expectedXPosition = maxMainAxisSize - eachSize
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = maxMainAxisSize - eachSize
            } else {
                expectedXPosition -= eachSize
            }
        }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                   width = it.width
                            },
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachColumn = 6
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_wrap() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(120)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(80.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(180)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(100)
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .height(if (index == 5) 100.toDp() else 20.toDp())
                                        .width(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(100)
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint_withSpacedBy() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(100)
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint_withSpacedBy() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            verticalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(140)
        Truth.assertThat(width).isEqualTo(120)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Max).wrapContentHeight().onSizeChanged {
                                width = it.width
                            },
                            maxItemsInEachColumn = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint_withMaxItems() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(101)
        Truth.assertThat(height).isEqualTo(120)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint_withMaxItems() {
        var width = 0
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.width(IntrinsicSize.Min).wrapContentHeight().onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachColumn = 2
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(180)
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicHeight() {
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier.height(IntrinsicSize.Min).wrapContentWidth().onSizeChanged {
                                height = it.height
                            },
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight() {
        var height = 0
        rule.setContent {
            CompositionLocalProvider(
                values = arrayOf(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                )
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier.height(IntrinsicSize.Min).wrapContentWidth().onSizeChanged {
                                height = it.height
                            },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }
}