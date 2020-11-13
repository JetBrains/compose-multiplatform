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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureBlock
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.test.ScrollToTest.Orientation.Horizontal
import androidx.compose.ui.test.ScrollToTest.Orientation.Vertical
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.ClickableTestBox
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ScrollToTest {
    companion object {
        private const val tag = "target"
        private const val crossAxisSize = 100
    }

    enum class Orientation { Horizontal, Vertical }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = mutableListOf<Offset>()

    private fun horizontalLayout(offset: Int, columnWidth: Int): MeasureBlock {
        return { measurables, constraints ->
            val childConstraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
            val placeables = measurables.map { it.measure(childConstraints) }
            layout(columnWidth, crossAxisSize) {
                var placeOffset = -offset
                placeables.forEach {
                    it.place(placeOffset, 0)
                    placeOffset += it.width
                }
            }
        }
    }

    private fun verticalLayout(offset: Int, columnHeight: Int): MeasureBlock {
        return { measurables, constraints ->
            val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
            val placeables = measurables.map { it.measure(childConstraints) }
            layout(crossAxisSize, columnHeight) {
                var placeOffset = -offset
                placeables.forEach {
                    it.place(0, placeOffset)
                    placeOffset += it.height
                }
            }
        }
    }

    @Composable
    private fun SimpleColumn(
        modifier: Modifier,
        offset: Int,
        columnHeight: Int,
        content: @Composable () -> Unit
    ) {
        with(AmbientDensity.current) {
            Layout(
                content,
                modifier.size(crossAxisSize.toDp(), columnHeight.toDp()),
                verticalLayout(offset, columnHeight)
            )
        }
    }

    @Composable
    private fun SimpleRow(
        modifier: Modifier,
        offset: Int,
        rowWidth: Int,
        content: @Composable () -> Unit
    ) {
        with(AmbientDensity.current) {
            Layout(
                content,
                modifier.size(rowWidth.toDp(), crossAxisSize.toDp()),
                horizontalLayout(offset, rowWidth)
            )
        }
    }

    @Composable
    private fun BoxesWithOffset(
        modifier: Modifier,
        orientation: Orientation,
        offset: Int,
        mainAxisSize: Int,
        content: @Composable () -> Unit
    ) {
        when (orientation) {
            Horizontal -> SimpleRow(modifier, offset, mainAxisSize, content)
            Vertical -> SimpleColumn(modifier, offset, mainAxisSize, content)
        }
    }

    /**
     * Creates a row or column (depending on the [orientation]) of 5 boxes in a viewport of size
     * [mainAxisSizePx] and [crossAxisSize], offset by the given [scrollOffsetPx] and tests if
     * [performScrollTo] scrolls by the expected amount of pixels when called on the middle of
     * the 5 boxes. Each box is 100x100 pixels.
     */
    private fun test(
        orientation: Orientation,
        scrollOffsetPx: Int,
        mainAxisSizePx: Int,
        expectedScrollX: Float = 0f,
        expectedScrollY: Float = 0f
    ) {
        rule.setContent {
            BoxesWithOffset(
                modifier = Modifier.semantics {
                    scrollBy(action = { x, y -> recorder.add(Offset(x, y)) })
                },
                orientation = orientation,
                offset = scrollOffsetPx,
                mainAxisSize = mainAxisSizePx
            ) {
                ClickableTestBox(color = Color.Blue)
                ClickableTestBox(color = Color.Red)
                ClickableTestBox(color = Color.Yellow, tag = tag)
                ClickableTestBox(color = Color.Green)
                ClickableTestBox(color = Color.Cyan)
            }
        }

        rule.waitForIdle()
        assertThat(recorder).isEmpty()

        rule.onNodeWithTag(tag).performScrollTo()

        rule.waitForIdle()
        assertThat(recorder).containsExactly(Offset(expectedScrollX, expectedScrollY))
    }

    /* VERTICAL */

    /* Tests with a viewport larger than the target (target fits in the viewport) */

    @Test
    fun vertical_largerViewport_targetCompletelyBelowViewport() {
        test(Vertical, scrollOffsetPx = 0, mainAxisSizePx = 150, expectedScrollY = 150f)
    }

    @Test
    fun vertical_largerViewport_targetPartlyBelowViewport() {
        test(Vertical, scrollOffsetPx = 100, mainAxisSizePx = 150, expectedScrollY = 50f)
    }

    @Test
    fun vertical_largerViewport_targetBottomAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 150, mainAxisSizePx = 150, expectedScrollY = 0f)
    }

    @Test
    fun vertical_largerViewport_targetCenterAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 175, mainAxisSizePx = 150, expectedScrollY = 0f)
    }

    @Test
    fun vertical_largerViewport_targetTopAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 200, mainAxisSizePx = 150, expectedScrollY = 0f)
    }

    @Test
    fun vertical_largerViewport_targetPartlyAboveViewport() {
        test(Vertical, scrollOffsetPx = 250, mainAxisSizePx = 150, expectedScrollY = -50f)
    }

    @Test
    fun vertical_largerViewport_targetCompletelyAboveViewport() {
        test(Vertical, scrollOffsetPx = 350, mainAxisSizePx = 150, expectedScrollY = -150f)
    }

    /* Tests with a viewport smaller than the target (target does not fit in the viewport) */

    @Test
    fun vertical_smallerViewport_targetCompletelyBelowViewport() {
        test(Vertical, scrollOffsetPx = 0, mainAxisSizePx = 80, expectedScrollY = 200f)
    }

    @Test
    fun vertical_smallerViewport_targetPartlyBelowViewport() {
        test(Vertical, scrollOffsetPx = 150, mainAxisSizePx = 80, expectedScrollY = 50f)
    }

    @Test
    fun vertical_smallerViewport_targetTopAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 200, mainAxisSizePx = 80, expectedScrollY = 0f)
    }

    @Test
    fun vertical_smallerViewport_targetCenterAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 210, mainAxisSizePx = 80, expectedScrollY = 0f)
    }

    @Test
    fun vertical_smallerViewport_targetBottomAlignedInViewport() {
        test(Vertical, scrollOffsetPx = 220, mainAxisSizePx = 80, expectedScrollY = 0f)
    }

    @Test
    fun vertical_smallerViewport_targetPartlyAboveViewport() {
        test(Vertical, scrollOffsetPx = 250, mainAxisSizePx = 80, expectedScrollY = -30f)
    }

    @Test
    fun vertical_smallerViewport_targetCompletelyAboveViewport() {
        test(Vertical, scrollOffsetPx = 420, mainAxisSizePx = 80, expectedScrollY = -200f)
    }

    /* HORIZONTAL */

    /* Tests with a viewport larger than the target (target fits in the viewport) */

    @Test
    fun horizontal_largerViewport_targetCompletelyRightOfViewport() {
        test(Horizontal, scrollOffsetPx = 0, mainAxisSizePx = 150, expectedScrollX = 150f)
    }

    @Test
    fun horizontal_largerViewport_targetPartlyRightOfViewport() {
        test(Horizontal, scrollOffsetPx = 100, mainAxisSizePx = 150, expectedScrollX = 50f)
    }

    @Test
    fun horizontal_largerViewport_targetRightAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 150, mainAxisSizePx = 150, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_largerViewport_targetCenterAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 175, mainAxisSizePx = 150, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_largerViewport_targetLeftAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 200, mainAxisSizePx = 150, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_largerViewport_targetPartlyLeftOfViewport() {
        test(Horizontal, scrollOffsetPx = 250, mainAxisSizePx = 150, expectedScrollX = -50f)
    }

    @Test
    fun horizontal_largerViewport_targetCompletelyLeftOfViewport() {
        test(Horizontal, scrollOffsetPx = 350, mainAxisSizePx = 150, expectedScrollX = -150f)
    }

    /* Tests with a viewport smaller than the target (target does not fit in the viewport) */

    @Test
    fun horizontal_smallerViewport_targetCompletelyRightOfViewport() {
        test(Horizontal, scrollOffsetPx = 0, mainAxisSizePx = 80, expectedScrollX = 200f)
    }

    @Test
    fun horizontal_smallerViewport_targetPartlyRightOfViewport() {
        test(Horizontal, scrollOffsetPx = 150, mainAxisSizePx = 80, expectedScrollX = 50f)
    }

    @Test
    fun horizontal_smallerViewport_targetLeftAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 200, mainAxisSizePx = 80, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_smallerViewport_targetCenterAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 210, mainAxisSizePx = 80, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_smallerViewport_targetRightAlignedInViewport() {
        test(Horizontal, scrollOffsetPx = 220, mainAxisSizePx = 80, expectedScrollX = 0f)
    }

    @Test
    fun horizontal_smallerViewport_targetPartlyLeftOfViewport() {
        test(Horizontal, scrollOffsetPx = 250, mainAxisSizePx = 80, expectedScrollX = -30f)
    }

    @Test
    fun horizontal_smallerViewport_targetCompletelyLeftOfViewport() {
        test(Horizontal, scrollOffsetPx = 420, mainAxisSizePx = 80, expectedScrollX = -200f)
    }
}
