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

package androidx.compose.ui.test.assertions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max

@MediumTest
@RunWith(AndroidJUnit4::class)
class BoundsAssertionsTest {

    @get:Rule
    val rule = createComposeRule()

    val tag = "box"

    private fun composeBox() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
            ) {
                Box(modifier = Modifier.padding(start = 50.dp, top = 100.dp)) {
                    Box(
                        modifier = Modifier
                            .testTag(tag)
                            .requiredSize(80.dp, 100.dp)
                            .background(color = Color.Black)
                    )
                }
            }
        }
    }

    @Test
    fun assertSizeEquals() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun assertSizeAtLeast() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsAtLeast(80.dp)
            .assertWidthIsAtLeast(79.dp)
            .assertHeightIsAtLeast(100.dp)
            .assertHeightIsAtLeast(99.dp)
    }

    @Test
    fun assertSizeEquals_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertWidthIsEqualTo(70.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertHeightIsEqualTo(90.dp)
        }
    }

    @Test
    fun assertSizeAtLeast_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertWidthIsAtLeast(81.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(101.dp)
        }
    }

    @Test
    fun assertPosition() {
        composeBox()

        rule.onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = 50.dp, expectedTop = 100.dp)
            .assertLeftPositionInRootIsEqualTo(50.dp)
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun assertPosition_fail() {
        composeBox()

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 51.dp, expectedTop = 101.dp)
        }

        expectError<AssertionError> {
            rule.onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 49.dp, expectedTop = 99.dp)
        }
    }

    private fun composeClippedBox() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .wrapContentSize(Alignment.TopStart)
            ) {
                // Box is shifted 30dp to the left and 10dp to the top,
                // so it is clipped to a size of 50 x 90
                Box(
                    modifier = Modifier
                        .offset((-30).dp, (-10).dp)
                        .testTag(tag)
                        .requiredSize(80.dp, 100.dp)
                        .background(color = Color.Black)
                )
            }
        }
    }

    @Test
    fun assertClippedPosition() {
        composeClippedBox()

        rule.onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = (-30).dp, expectedTop = (-10).dp)
            .assertLeftPositionInRootIsEqualTo((-30).dp)
            .assertTopPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun assertClippedSize() {
        composeClippedBox()

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun getPosition_measuredNotPlaced() {
        // When we have a node that is measure but not placed
        getPositionTest {
            DoNotPlace {
                Box(Modifier.testTag(tag).requiredSize(10.dp))
            }
        }
    }

    @Test
    fun getPosition_notMeasuredNotPlaced() {
        // When we have a node that is not measure and not placed
        getPositionTest {
            DoNotMeasure {
                Box(Modifier.testTag(tag).requiredSize(10.dp))
            }
        }
    }

    private fun getPositionTest(content: @Composable () -> Unit) {
        // When we have a node that is [not] measured and not placed
        rule.setContent(content)

        // Then querying for positional information gives unspecified values
        val node = rule.onNodeWithTag(tag)
        node.assertPositionInRootIsEqualTo(Dp.Unspecified, Dp.Unspecified)
        node.assertTopPositionInRootIsEqualTo(Dp.Unspecified)
        node.assertLeftPositionInRootIsEqualTo(Dp.Unspecified)
        node.getUnclippedBoundsInRoot().let {
            assertThat(it.left).isEqualTo(Dp.Unspecified)
            assertThat(it.top).isEqualTo(Dp.Unspecified)
            assertThat(it.right).isEqualTo(Dp.Unspecified)
            assertThat(it.bottom).isEqualTo(Dp.Unspecified)
        }

        fun notEqual(subject: String) = "Actual $subject is Dp.Unspecified, expected 1.0.dp \\(.*"

        expectError<AssertionError>(expectedMessage = notEqual("left")) {
            node.assertPositionInRootIsEqualTo(1.dp, 1.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("top")) {
            node.assertTopPositionInRootIsEqualTo(1.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("left")) {
            node.assertLeftPositionInRootIsEqualTo(1.dp)
        }
    }

    @Test
    fun getSize_measuredNotPlaced() {
        // When we have a node that is measure but not placed
        getSizeTest {
            DoNotPlace {
                Box(Modifier.testTag(tag).requiredSize(10.dp))
            }
        }
    }

    @Test
    fun getSize_notMeasuredNotPlaced() {
        // When we have a node that is not measure and not placed
        getSizeTest {
            DoNotMeasure {
                Box(Modifier.testTag(tag).requiredSize(10.dp))
            }
        }
    }

    private fun getSizeTest(content: @Composable() () -> Unit) {
        // When we have a node that is [not] measured and not placed
        rule.setContent(content)

        // Then querying for size information gives real or unspecified values
        val node = rule.onNodeWithTag(tag)
        node.assertWidthIsEqualTo(Dp.Unspecified)
        node.assertHeightIsEqualTo(Dp.Unspecified)
        node.assertWidthIsAtLeast(Dp.Unspecified)
        node.assertHeightIsAtLeast(Dp.Unspecified)

        fun notEqual(subject: String) =
            "Actual $subject is Dp.Unspecified, expected 10.0.dp \\(.*"
        fun notAtLeast(subject: String) =
            "Actual $subject is Dp.Unspecified, expected at least 10.0.dp \\(.*"

        expectError<AssertionError>(expectedMessage = notEqual("width")) {
            node.assertWidthIsEqualTo(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("height")) {
            node.assertHeightIsEqualTo(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notAtLeast("width")) {
            node.assertWidthIsAtLeast(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notAtLeast("height")) {
            node.assertHeightIsAtLeast(10.dp)
        }
    }

    @Test
    fun getAlignmentLine_measuredNotPlaced() {
        // When we have a node with an alignment line that is measured but not placed
        getAlignmentLineTest(expectedPosition = with(rule.density) { TestLinePosition.toDp() }) {
            DoNotPlace {
                BoxWithAlignmentLine(Modifier.testTag(tag))
            }
        }
    }

    @Test
    fun getAlignmentLine_notMeasuredNotPlaced() {
        // When we have a node with an alignment line that is not measured and not placed
        getAlignmentLineTest(expectedPosition = Dp.Unspecified) {
            DoNotMeasure {
                BoxWithAlignmentLine(Modifier.testTag(tag))
            }
        }
    }

    private fun getAlignmentLineTest(expectedPosition: Dp, content: @Composable () -> Unit) {
        // When we have a node with an alignment line that is [not] measured and not placed
        rule.setContent(content)

        // Then we can still query the alignment line
        assertThat(rule.onNodeWithTag(tag).getAlignmentLinePosition(TestLine))
            .isEqualTo(expectedPosition)
    }

    @Composable
    private fun DoNotMeasure(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { _, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {}
        }
    }

    @Composable
    private fun DoNotPlace(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeable = measurables.map { it.measure(constraints) }
            layout(placeable.maxOf { it.width }, placeable.maxOf { it.height }) {}
        }
    }

    private val TestLinePosition = 30
    private val TestLine = HorizontalAlignmentLine(::max)

    @Composable
    private fun BoxWithAlignmentLine(modifier: Modifier) {
        Layout({}, modifier) { _, constraints ->
            layout(
                constraints.maxWidth,
                constraints.maxHeight,
                mapOf(TestLine to TestLinePosition)
            ) {}
        }
    }
}
