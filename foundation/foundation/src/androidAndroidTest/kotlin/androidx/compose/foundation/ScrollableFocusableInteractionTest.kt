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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ScrollableFocusableInteractionTest(private val orientation: Orientation) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Orientation> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal
        )
    }

    @get:Rule
    val rule = createComposeRule()

    private val scrollableAreaTag = "scrollableArea"
    private val focusableTag = "focusable"
    private val focusRequester = FocusRequester()
    private val scrollState = ScrollState(initial = 0)

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
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(40.dp)
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenFullyInViewAndBecomesPartiallyHidden() {
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 95.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(85.dp)
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenPartiallyInViewAndBecomesMoreHidden() {
        var viewportSize by mutableStateOf(95.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 91.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(81.dp)
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenPartiallyInViewAndBecomesFullyHidden() {
        var viewportSize by mutableStateOf(95.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 90.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(80.dp)
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportAnimatedQuickly() {
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Manually execute an "animation" that shrinks the viewport by twice the focusable's size
        // on every frame, for a few frames. The underlying bug in b/230756508 would lose track
        // of the focusable after the second frame.
        rule.mainClock.autoAdvance = false
        viewportSize = 80.dp
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        viewportSize = 60.dp
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        viewportSize = 40.dp
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Resume the clock. The scroll animation should finish.
        rule.mainClock.autoAdvance = true

        rule.onNodeWithTag(focusableTag)
            .assertIsDisplayed()
    }

    @Test
    fun scrollFromViewportShrink_isInterrupted_byGesture() {
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        rule.mainClock.autoAdvance = false
        viewportSize = 80.dp
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
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        rule.mainClock.autoAdvance = false
        viewportSize = 80.dp
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
        viewportSize = 100.dp
        rule.waitForIdle()
        Thread.sleep(2000)

        // Shrink the viewport again, this should trigger another scroll animation to keep the
        // scrollable in view.
        viewportSize = 50.dp
        rule.waitForIdle()

        rule.onNodeWithTag(focusableTag)
            .assertIsDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenNotInViewAndViewportShrunk() {
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(viewportSize) {
                // Put a focusable just below the bottom of the viewport, out of view.
                Spacer(Modifier.size(100.dp))
                TestFocusable(10.dp)
            }
        }
        requestFocusAndScrollToTop()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(100.dp)
            .assertIsNotDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.dp

        // Focusable should not have moved since it was never in view.
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(100.dp)
            .assertIsNotDisplayed()
    }

    @Test
    fun doesNotScrollUnfocusedFocusableIntoView_whenViewportShrunk() {
        var viewportSize by mutableStateOf(100.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.dp))
                TestFocusable(size = 10.dp)
            }
        }
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsDisplayed()
            .assertIsNotFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.dp)
            .assertIsNotDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndViewportGrown() {
        var viewportSize by mutableStateOf(50.dp)

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                // Put a focusable in the middle of the viewport, but ensure we're a lot bigger
                // than the viewport so it can grow without requiring a scroll.
                Spacer(Modifier.size(100.dp))
                TestFocusable(size = 10.dp)
                Spacer(Modifier.size(100.dp))
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        // Requesting focus will bring the entire focused item into view (at the bottom of the
        // viewport), then scroll up by half the focusable height so it's partially in view.
        rule.waitForIdle()
        runBlocking {
            val halfFocusableSize = with(rule.density) { (10.dp / 2).toPx() }
            scrollState.scrollBy(-halfFocusableSize)
        }
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(45.dp)
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Grow the viewport.
        viewportSize = 100.dp

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(45.dp)
            .assertIsDisplayed()
    }

    @Test
    fun scrollsToNewFocusable_whenFocusedChildChangesDuringAnimation() {
        var viewportSize by mutableStateOf(100.dp)
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        rule.setContent {
            TestScrollableColumn(size = viewportSize) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(Color.Blue)
                        .testTag("focusable1")
                        .focusRequester(focusRequester1)
                        .focusable()
                )
                Spacer(Modifier.size(100.dp))
                Box(
                    Modifier
                        .size(10.dp)
                        .background(Color.Blue)
                        .testTag("focusable2")
                        .focusRequester(focusRequester2)
                        .focusable()
                )
            }
        }
        // When focusable2 gets focus, focusable1 should be scrolled out of view.
        rule.runOnIdle {
            focusRequester2.requestFocus()
        }
        rule.onNodeWithTag("focusable1").assertIsNotDisplayed()
        rule.onNodeWithTag("focusable2").assertIsDisplayed()
        // Pause the clock because we need to do some work in the middle of an animation.
        rule.mainClock.autoAdvance = false

        // Shrink the viewport, which should scroll to keep focusable2 in-view.
        rule.runOnIdle {
            viewportSize = 20.dp
        }

        // Tick the clock forward to let the animation start and run a bit.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag("focusable1").assertIsNotDisplayed()
        rule.onNodeWithTag("focusable2").assertIsNotDisplayed()

        rule.runOnIdle {
            focusRequester1.requestFocus()
        }
        // Resume the clock, allow animation to finish.
        rule.mainClock.autoAdvance = true

        rule.onNodeWithTag("focusable1").assertIsDisplayed()
        rule.onNodeWithTag("focusable2").assertIsNotDisplayed()
    }

    @Composable
    private fun TestScrollableColumn(
        size: Dp,
        content: @Composable () -> Unit
    ) {
        val modifier = Modifier
            .testTag(scrollableAreaTag)
            .size(size)
            .border(2.dp, Color.Black)

        when (orientation) {
            Orientation.Vertical -> {
                Column(
                    // Uses scrollable under the hood.
                    modifier.verticalScroll(scrollState)
                ) { content() }
            }
            Orientation.Horizontal -> {
                Row(
                    // Uses scrollable under the hood.
                    modifier.horizontalScroll(scrollState)
                ) { content() }
            }
        }
    }

    @Composable
    private fun TestFocusable(size: Dp) {
        Box(
            Modifier
                .testTag(focusableTag)
                .size(size)
                .background(Color.Blue)
                .focusRequester(focusRequester)
                .focusable()
        )
    }

    private fun requestFocusAndScrollToTop() {
        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        // Reset scroll to top since requesting focus will scroll it.
        runBlocking {
            scrollState.scrollTo(0)
        }
    }

    private fun SemanticsNodeInteraction.assertScrollAxisPositionInRootIsEqualTo(expected: Dp) =
        when (orientation) {
            Orientation.Vertical -> assertTopPositionInRootIsEqualTo(expected)
            Orientation.Horizontal -> assertLeftPositionInRootIsEqualTo(expected)
        }
}