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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Copy of [LazyListFocusableInteractionTest], modified for non-lazy scrollables. Any new tests
 * added here should probably be added there too.
 */
@MediumTest
@RunWith(Parameterized::class)
class ScrollableFocusableInteractionTest(
    private val orientation: Orientation,
    private val reverseScrolling: Boolean
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} reverseScrolling={1}")
        fun initParameters() = arrayOf(
            arrayOf(Vertical, true),
            arrayOf(Vertical, false),
            arrayOf(Horizontal, true),
            arrayOf(Horizontal, false),
        )
    }

    @get:Rule
    val rule = createComposeRule()

    private val scrollableAreaTag = "scrollableArea"
    private val focusableTag = "focusable"
    private val scrollState = ScrollState(initial = 0)
    private lateinit var focusManager: FocusManager

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenFullyInViewAndBecomesFullyHidden() {
        var viewportSize by mutableStateOf(100.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp())
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(40.toDp())
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenFullyInViewAndBecomesPartiallyHidden() {
        var viewportSize by mutableStateOf(100.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp())
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 95.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(85.toDp())
            .assertIsDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndBecomesMoreHidden() {
        var viewportSize by mutableStateOf(95.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) (-5).toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 91.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) (-5 - 4).toDp() else 90.toDp()
            )
            .assertIsDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndBecomesFullyHidden() {
        var viewportSize by mutableStateOf(95.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                WithSpacerBefore(90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) (-5).toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 90.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                // When reversing scrolling, shrinking the viewport will move the child as well by
                // the amount it shrunk – 5px.
                if (reverseScrolling) (-5 - 5).toDp() else 90.toDp()
            )
            .assertIsNotDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportAnimatedQuickly() {
        var viewportSize by mutableStateOf(100.toDp())
        var animate by mutableStateOf(false)

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp())
            }

            if (animate) {
                LaunchedEffect(Unit) {
                    // Manually execute an "animation" that shrinks the viewport by twice the
                    // focusable's size on every frame, for a few frames. The underlying bug in
                    // b/230756508 would lose track of the focusable after the second frame.
                    withFrameNanos {
                        viewportSize = 80.toDp()
                    }
                    withFrameNanos {
                        viewportSize = 60.toDp()
                    }
                    withFrameNanos {
                        viewportSize = 40.toDp()
                    }
                }
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        animate = true

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(30.toDp())
            .assertIsDisplayed()
    }

    @Test
    fun scrollFromViewportShrink_isInterrupted_byGesture() {
        var viewportSize by mutableStateOf(100.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp())
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        rule.mainClock.autoAdvance = false
        viewportSize = 80.toDp()
        // Run the first frame of the scroll animation.
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Interrupt the scroll by manually dragging.
        rule.onNodeWithTag(scrollableAreaTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(viewConfiguration.touchSlop + 1, viewConfiguration.touchSlop + 1))
                up()
            }

        // Resume the clock. The animation scroll animation should have been interrupted and not
        // continue.
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.autoAdvance = true

        rule.onNodeWithTag(focusableTag)
            .assertIsNotDisplayed()
    }

    /**
     * This test ensures that scrollable correctly cleans up its state when the scroll animation
     * triggered by shrinking the viewport is interrupted by something other than another shrink
     * and the focusable child does not change. If it's cleaned up correctly, expanding then re-
     * shrinking the viewport should trigger another animation.
     */
    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportExpandedThenReshrunk_afterInterruption() {
        var viewportSize by mutableStateOf(100.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp())
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        rule.mainClock.autoAdvance = false
        viewportSize = 80.toDp()
        // Run the first frame of the scroll animation.
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Interrupt the scroll by manually dragging.
        rule.onNodeWithTag(scrollableAreaTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(viewConfiguration.touchSlop + 1, viewConfiguration.touchSlop + 1))
                up()
            }

        // Resume the clock. The animation scroll animation should have been interrupted and not
        // continue.
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag(focusableTag)
            .assertIsFocused()
            .assertIsNotDisplayed()

        // Expand the viewport back to its original size to bring the focusable back into view.
        viewportSize = 100.toDp()
        rule.waitForIdle()

        // Shrink the viewport again, this should trigger another scroll animation to keep the
        // scrollable in view.
        viewportSize = 50.toDp()
        rule.waitForIdle()

        rule.onNodeWithTag(focusableTag)
            .assertIsDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenNotInViewAndViewportShrunk() {
        val gapSize = 100.toDp()
        var viewportSize by mutableStateOf(gapSize)

        rule.setContent {
            ScrollableRowOrColumn(viewportSize) {
                // Put a focusable just out of view.
                WithSpacerBefore(size = gapSize) {
                    TestFocusable(10.toDp())
                }
            }
        }
        requestFocusAndScrollToTop()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) (-10).toDp() else gapSize
            )
            .assertIsNotDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()

        // Focusable should not have moved since it was never in view.
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                // Focusable size minus the change in viewport size.
                if (reverseScrolling) (-10 - 50).toDp() else gapSize
            )
            .assertIsNotDisplayed()
    }

    @Test
    fun doesNotScrollUnfocusedFocusableIntoView_whenViewportShrunk() {
        var viewportSize by mutableStateOf(100.toDp())

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) 0.toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsNotFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) (-50).toDp() else 90.toDp()
            )
            .assertIsNotDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndViewportGrown() {
        val initialViewPortSize = 50.toDp()
        val itemSize = 30.toDp()
        var viewportSize by mutableStateOf(initialViewPortSize)

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the middle of the viewport, but ensure we're a lot bigger
                // than the viewport so it can grow without requiring a scroll.
                Spacer(Modifier.size(initialViewPortSize * 2))
                TestFocusable(size = itemSize)
                Spacer(Modifier.size(initialViewPortSize * 2))
            }
        }
        scrollToTop()
        requestFocus()

        // Requesting focus will bring the entire focused item into view (at the bottom of the
        // viewport), then scroll up by half the focusable height so it's partially in view.
        rule.waitForIdle()
        runBlocking {
            val halfFocusableSize = with(rule.density) { (itemSize / 2).toPx() }
            scrollState.scrollBy(-halfFocusableSize)
        }

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling) -itemSize / 2 else initialViewPortSize - (itemSize / 2)
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Grow the viewport.
        viewportSize *= 2

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(initialViewPortSize - (itemSize / 2))
            .assertIsDisplayed()
    }

    @Test
    fun scrollsToNewFocusable_whenFocusedChildChangesDuringAnimation() {
        var viewportSize by mutableStateOf(100.toDp())
        val focusable1 = "focusable1"
        val focusable2 = "focusable2"

        @Composable
        fun Focusable1() {
            Box(
                Modifier
                    .size(10.toDp())
                    .background(Color.Blue)
                    .testTag(focusable1)
                    .focusable()
            )
        }

        @Composable
        fun Focusable2() {
            Box(
                Modifier
                    .size(10.toDp())
                    .background(Color.Blue)
                    .testTag(focusable2)
                    .focusable()
            )
        }

        rule.setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                if (reverseScrolling) Focusable2() else Focusable1()
                Spacer(Modifier.size(100.toDp()))
                if (reverseScrolling) Focusable1() else Focusable2()
            }
        }
        // When focusable2 gets focus, focusable1 should be scrolled out of view.
        requestFocus(focusable2)
        rule.onNodeWithTag(focusable1).assertIsNotDisplayed()
        rule.onNodeWithTag(focusable2).assertIsDisplayed()
        // Pause the clock because we need to do some work in the middle of an animation.
        rule.mainClock.autoAdvance = false

        // Shrink the viewport, which should scroll to keep focusable2 in-view.
        rule.runOnIdle {
            viewportSize = 20.toDp()
        }

        // Tick the clock forward to let the animation start and run a bit.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag(focusable1).assertIsNotDisplayed()
        rule.onNodeWithTag(focusable2).assertIsNotDisplayed()

        requestFocus(focusable1)
        // Resume the clock, allow animation to finish.
        rule.mainClock.autoAdvance = true

        rule.onNodeWithTag(focusable1).assertIsDisplayed()
        rule.onNodeWithTag(focusable2).assertIsNotDisplayed()
    }

    @Test
    fun focusingOnVisibleItemDoesNotScroll_whenMultipleFocusables() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 3) {
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = focusableTag)
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = "")
            }
        }

        // Act.
        requestFocus()

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(0) }
    }

    @Test
    fun focusingOutOfBoundsItem_bringsItIntoView_whenMultipleFocusables() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = focusableTag)
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = "")
            }
        }

        // Act.
        requestFocus()

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    @Test
    fun moveOutFromBoundaryItem_bringsNextItemIntoView() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                TestFocusable(itemSize, tag = "")
                TestFocusable(itemSize, tag = focusableTag)
                TestFocusable(itemSize, tag = "")
            }
        }
        requestFocus()

        // Act.
        rule.runOnIdle { focusManager.moveFocus(if (reverseScrolling) Previous else Next) }

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    private fun ComposeContentTestRule.setContentForTest(
        composable: @Composable () -> Unit
    ) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
    }

    @Composable
    private fun ScrollableRowOrColumn(
        size: Dp,
        content: @Composable () -> Unit
    ) {
        val modifier = Modifier
            .testTag(scrollableAreaTag)
            .size(size)
            .border(2.toDp(), Color.Black)

        when (orientation) {
            Vertical -> {
                Column(
                    // Uses scrollable under the hood.
                    modifier.verticalScroll(
                        state = scrollState,
                        reverseScrolling = reverseScrolling
                    )
                ) { content() }
            }

            Horizontal -> {
                Row(
                    // Uses scrollable under the hood.
                    modifier.horizontalScroll(
                        state = scrollState,
                        reverseScrolling = reverseScrolling
                    )
                ) { content() }
            }
        }
    }

    /**
     * Places a spacer before or after [content], depending on [reverseScrolling].
     */
    @Composable
    fun WithSpacerBefore(size: Dp, content: @Composable () -> Unit) {
        if (!reverseScrolling) {
            Spacer(Modifier.size(size))
        }
        content()
        if (reverseScrolling) {
            Spacer(Modifier.size(size))
        }
    }

    @Composable
    private fun TestFocusable(size: Dp, tag: String = focusableTag) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
            Modifier
                .testTag(tag)
                .size(size)
                .border(1.dp, Color.White)
                .background(if (isFocused) Color.Blue else Color.Black)
                .focusable(interactionSource = interactionSource)
        )
    }

    /**
     * Sizes and offsets of the composables in these tests must be specified using this function.
     * If they're specified using `xx.dp` syntax, a rounding error somewhere in the layout system
     * will cause the pixel values to be off-by-one.
     */
    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    private fun requestFocusAndScrollToTop() {
        requestFocus()
        scrollToTop()
    }

    private fun requestFocus(tag: String = focusableTag) {
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    private fun scrollToTop() {
        rule.waitForIdle()
        // Reset scroll to top since requesting focus will scroll it.
        runBlocking {
            scrollState.scrollTo(if (reverseScrolling) -scrollState.maxValue else 0)
        }
    }

    private fun SemanticsNodeInteraction.assertScrollAxisPositionInRootIsEqualTo(expected: Dp) =
        when (orientation) {
            Vertical -> assertTopPositionInRootIsEqualTo(expected)
            Horizontal -> assertLeftPositionInRootIsEqualTo(expected)
        }
}