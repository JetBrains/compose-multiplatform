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

package androidx.compose.ui.test.actions

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.actions.ScrollToTest.ExpectedAlignment.Bottom
import androidx.compose.ui.test.actions.ScrollToTest.ExpectedAlignment.Center
import androidx.compose.ui.test.actions.ScrollToTest.ExpectedAlignment.Left
import androidx.compose.ui.test.actions.ScrollToTest.ExpectedAlignment.Right
import androidx.compose.ui.test.actions.ScrollToTest.ExpectedAlignment.Top
import androidx.compose.ui.test.actions.ScrollToTest.Orientation.HorizontalLtr
import androidx.compose.ui.test.actions.ScrollToTest.Orientation.HorizontalRtl
import androidx.compose.ui.test.actions.ScrollToTest.Orientation.Vertical
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.CenterAlignedIn
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.EndAlignedIn
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.FullyAfter
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.FullyBefore
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.PartiallyAfter
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.PartiallyBefore
import androidx.compose.ui.test.actions.ScrollToTest.StartPosition.StartAlignedIn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ScrollToTest(private val config: TestConfig) {
    data class TestConfig(
        val orientation: Orientation,
        val reverseScrolling: Boolean,
        val viewportSize: ViewportSize,
        val startPosition: StartPosition,
        val expectScrolling: Boolean,
        val expectedAlignment: ExpectedAlignment
    ) {
        val viewportSizePx: Int get() = viewportSize.sizePx

        val initialScrollOffset: Int get() {
            val offset = when (viewportSize) {
                ViewportSize.SmallerThanItem -> startPosition.smallViewportOffset
                ViewportSize.BiggerThenItem -> startPosition.bigViewportOffset
            }
            val scrollRange = itemCount * itemSizePx - viewportSizePx
            // Need to invert the scroll offset for reverseScrolling so the target is
            // on the correct side of the viewport according to the [StartPosition]
            return if (reverseScrolling) scrollRange - offset else offset
        }

        override fun toString(): String = "orientation=$orientation, " +
            "reverseScrolling=$reverseScrolling, " +
            "viewport=$viewportSize, " +
            "targetStarts=${startPosition}Viewport, " +
            "expectScrolling=$expectScrolling, " +
            "expectedAlignment=$expectedAlignment"
    }

    companion object {
        private const val containerTag = "container"
        private const val itemTag = "target"
        private const val itemCount = 5
        private const val itemSizePx = 100
        private const val bigViewport = 150
        private const val smallViewport = 80

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<TestConfig>().apply {
            for (orientation in Orientation.values()) {
                for (reverseScrolling in listOf(false, true)) {
                    for (viewportSize in ViewportSize.values()) {
                        for (startPosition in StartPosition.values()) {
                            addConfig(orientation, reverseScrolling, viewportSize, startPosition)
                        }
                    }
                }
            }
        }

        private fun MutableList<TestConfig>.addConfig(
            orientation: Orientation,
            reverseScrolling: Boolean,
            viewportSize: ViewportSize,
            startPosition: StartPosition
        ) {
            val isVertical = orientation == Vertical
            val expectScrolling = startPosition.expectScrolling

            // Start with simple expectation, factor in other parameters below
            var expectedAlignment = when (startPosition) {
                FullyAfter, PartiallyAfter, EndAlignedIn -> if (isVertical) Bottom else Right
                FullyBefore, PartiallyBefore, StartAlignedIn -> if (isVertical) Top else Left
                CenterAlignedIn -> Center
            }

            // When scrolling into a small viewport, the opposite edge is found first
            if (expectScrolling && viewportSize == ViewportSize.SmallerThanItem) {
                expectedAlignment = expectedAlignment.reverse
            }

            // In horizontal RTL, the expectation is reversed
            if (orientation == HorizontalRtl) {
                expectedAlignment = expectedAlignment.reverse
            }

            TestConfig(
                orientation,
                reverseScrolling,
                viewportSize,
                startPosition,
                expectScrolling,
                expectedAlignment
            ).also { add(it) }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun scrollToTarget() {
        val scrollState = ScrollState(config.initialScrollOffset)
        val isRtl = config.orientation == HorizontalRtl

        // Five boxes in a row/col with a specific initialScrollOffset so that the target we want
        // to bring into view is either before, partially before, in, partially after or after
        // the viewport.
        rule.setContent {
            val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                when (config.orientation) {
                    HorizontalLtr,
                    HorizontalRtl -> Row(rowModifier(scrollState)) { Boxes() }
                    Vertical -> Column(columnModifier(scrollState)) { Boxes() }
                }
            }
        }

        val targetBoundsBefore = rule.onNodeWithTag(itemTag).getUnclippedBoundsInRoot().toPx()

        rule.onNodeWithTag(itemTag).performScrollTo()
        rule.waitForIdle()

        val viewportBounds = rule.onNodeWithTag(containerTag).getBoundsInRoot().toPx()
        val targetBoundsAfter = rule.onNodeWithTag(itemTag).getUnclippedBoundsInRoot().toPx()

        if (config.expectScrolling) {
            assertWithMessage("target must have scrolled, it was not yet in the viewport")
                .that(targetBoundsAfter).isNotEqualTo(targetBoundsBefore)
        } else {
            assertWithMessage("target must not scroll, it was already in the viewport")
                .that(targetBoundsAfter).isEqualTo(targetBoundsBefore)
        }

        when (config.expectedAlignment) {
            Left -> {
                assertWithMessage("target must be left-aligned in the viewport")
                    .that(targetBoundsAfter.left).isEqualTo(viewportBounds.left)
            }
            Right -> {
                assertWithMessage("target must be right-aligned in the viewport")
                    .that(targetBoundsAfter.right).isEqualTo(viewportBounds.right)
            }
            Top -> {
                assertWithMessage("target must be top-aligned in the viewport")
                    .that(targetBoundsAfter.top).isEqualTo(viewportBounds.top)
            }
            Bottom -> {
                assertWithMessage("target must be bottom-aligned in the viewport")
                    .that(targetBoundsAfter.bottom).isEqualTo(viewportBounds.bottom)
            }
            Center -> {
                assertWithMessage("target must be bottom-aligned in the viewport")
                    .that(targetBoundsAfter.center).isEqualTo(viewportBounds.center)
            }
        }
    }

    private fun DpRect.toPx(): Rect = with(rule.density) { toRect() }

    private fun rowModifier(scrollState: ScrollState): Modifier = Modifier.composed {
        with(LocalDensity.current) {
            Modifier
                .testTag(containerTag)
                .requiredSize(config.viewportSizePx.toDp(), itemSizePx.toDp())
                .horizontalScroll(scrollState, reverseScrolling = config.reverseScrolling)
        }
    }

    private fun columnModifier(scrollState: ScrollState): Modifier = Modifier.composed {
        with(LocalDensity.current) {
            Modifier
                .testTag(containerTag)
                .requiredSize(itemSizePx.toDp(), config.viewportSizePx.toDp())
                .verticalScroll(scrollState, reverseScrolling = config.reverseScrolling)
        }
    }

    @Composable
    private fun Boxes() {
        ClickableTestBox(color = Color.Blue)
        ClickableTestBox(color = Color.Red)
        ClickableTestBox(color = Color.Yellow, tag = itemTag)
        ClickableTestBox(color = Color.Green)
        ClickableTestBox(color = Color.Cyan)
        // When changing the number of boxes, don't forget to change itemCount
        assertThat(itemCount).isEqualTo(5)
    }

    enum class Orientation {
        HorizontalLtr,
        HorizontalRtl,
        Vertical
    }

    enum class ViewportSize(val sizePx: Int) {
        SmallerThanItem(smallViewport),
        BiggerThenItem(bigViewport)
    }

    enum class StartPosition(val smallViewportOffset: Int, val bigViewportOffset: Int) {
        FullyAfter(0, 0),
        PartiallyAfter(150, 100),
        EndAlignedIn(220, 150),
        CenterAlignedIn(210, 175),
        StartAlignedIn(200, 200),
        PartiallyBefore(270, 250),
        FullyBefore(420, 350);

        val expectScrolling get() = when (this) {
            EndAlignedIn, CenterAlignedIn, StartAlignedIn -> false
            else -> true
        }
    }

    enum class ExpectedAlignment {
        Left, Right, Top, Bottom, Center;
        val reverse get() = when (this) {
            Left -> Right
            Right -> Left
            Top -> Bottom
            Bottom -> Top
            Center -> Center
        }
    }
}
