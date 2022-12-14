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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Copy of [ScrollableFocusableInteractionTest], modified for lazy lists. Any new tests added here
 * should probably be added there too.
 */
@MediumTest
@RunWith(Parameterized::class)
class LazyListFocusableInteractionTest(
    private val orientation: Orientation,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = arrayOf(
            arrayOf(Vertical),
            arrayOf(Horizontal),
        )
    }

    @get:Rule
    val rule = createComposeRule()

    private val scrollableAreaTag = "scrollableArea"
    private val focusableTag = "focusable"
    private val scrollState = LazyListState()
    private lateinit var focusManager: FocusManager
    private lateinit var scope: CoroutineScope

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

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable at the end of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToStart()
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

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToStart()
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

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToStart()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 91.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndBecomesFullyHidden() {
        var viewportSize by mutableStateOf(95.toDp())

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                WithSpacerBefore(90.toDp()) {
                    TestFocusable(size = 10.toDp())
                }
            }
        }
        requestFocusAndScrollToStart()
        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 90.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsNotDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportAnimatedQuickly() {
        var viewportSize by mutableStateOf(100.toDp())
        var animate by mutableStateOf(false)

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                item { Spacer(Modifier.size(90.toDp())) }
                item { TestFocusable(size = 10.toDp()) }
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
        requestFocusAndScrollToStart()
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

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                item { Spacer(Modifier.size(90.toDp())) }
                item { TestFocusable(size = 10.toDp()) }
            }
        }
        requestFocusAndScrollToStart()
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

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                item { Spacer(Modifier.size(90.toDp())) }
                item { TestFocusable(size = 10.toDp()) }
            }
        }
        requestFocusAndScrollToStart()
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

        rule.setContentForTest {
            ScrollableRowOrColumn(viewportSize) {
                // Put a focusable just out of view.
                WithSpacerBefore(size = gapSize) {
                    TestFocusable(10.toDp())
                }
            }
        }
        runBlockingOnIdle {
            scrollState.scrollToItem(1)
        }
        requestFocus()
        rule.waitForIdle()
        scrollToStart()

        rule.onNodeWithTag(focusableTag)
            .assertIsNotDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()

        // Focusable should not have moved since it was never in view.
        rule.onNodeWithTag(focusableTag)
            .assertIsNotDisplayed()
            .assertIsFocused()
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
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsNotFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()

        rule.onNodeWithTag(focusableTag)
            .assertIsNotDisplayed()
    }

    @Test
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndViewportGrown() {
        val initialViewPortSize = 50
        val itemSize = 30
        var viewportSize by mutableStateOf(initialViewPortSize)

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize.toDp()) {
                // Put a focusable in the middle of the viewport, but ensure we're a lot bigger
                // than the viewport so it can grow without requiring a scroll.
                item { Spacer(Modifier.size(initialViewPortSize.toDp() * 2)) }
                item { TestFocusable(size = itemSize.toDp()) }
                item { Spacer(Modifier.size(initialViewPortSize.toDp() * 2)) }
            }
        }
        // Scroll the item to the end of the viewport so we can request focus.
        runBlockingOnIdle {
            scrollState.scrollToItem(1, scrollOffset = -(viewportSize - itemSize))
        }
        requestFocus()

        // Requesting focus will bring the entire focused item into view (at the end of the
        // viewport), then scroll back by half the focusable size so it's partially in view.
        rule.waitForIdle()
        runBlockingOnIdle {
            val halfFocusableSize = itemSize / 2f
            scrollState.scrollBy(-halfFocusableSize)
        }

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo((initialViewPortSize - (itemSize / 2)).toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Grow the viewport.
        viewportSize *= 2

        rule.onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo((initialViewPortSize - (itemSize / 2)).toDp())
            .assertIsDisplayed()
    }

    @Test
    fun scrollsToNewFocusable_whenFocusedChildChangesDuringAnimation() {
        var viewportSize by mutableStateOf(100.toDp())

        @Composable
        fun Focusable1() {
            Box(
                Modifier
                    .size(10.toDp())
                    .background(Color.Blue)
                    .testTag("focusable1")
                    .focusable()
            )
        }

        @Composable
        fun Focusable2() {
            Box(
                Modifier
                    .size(10.toDp())
                    .background(Color.Blue)
                    .testTag("focusable2")
                    .focusable()
            )
        }

        rule.setContentForTest {
            ScrollableRowOrColumn(size = viewportSize) {
                item { Focusable1() }
                item { Spacer(Modifier.size(100.toDp())) }
                item { Focusable2() }
            }
        }
        // When focusable2 gets focus, focusable1 should be scrolled out of view.
        runBlockingOnIdle {
            scrollState.scrollToItem(2)
        }
        requestFocus("focusable2")
        rule.onNodeWithTag("focusable1").assertIsNotDisplayed()
        rule.onNodeWithTag("focusable2")
            .assertIsDisplayed()
            .assertIsFocused()
        // Pause the clock because we need to do some work in the middle of an animation.
        rule.mainClock.autoAdvance = false

        // Shrink the viewport, which should scroll to keep focusable2 in-view.
        rule.runOnIdle {
            viewportSize = 20.toDp()
        }

        // Tick the clock forward to let the animation start and run a bit.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag("focusable1").assertIsNotDisplayed()
        rule.onNodeWithTag("focusable2").assertIsNotDisplayed()

        rule.runOnIdle {
            focusManager.moveFocus(Previous)
        }
        // Resume the clock, allow animation to finish.
        rule.mainClock.autoAdvance = true

        rule.onNodeWithTag("focusable2").assertIsNotDisplayed()
        rule.onNodeWithTag("focusable1")
            .assertIsDisplayed()
            .assertIsFocused()
    }

    @Test
    fun focusingOnVisibleItemDoesNotScroll_whenMultipleFocusables() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 3) {
                item { TestFocusable(itemSize, tag = "") }
                item { TestFocusable(itemSize, tag = "") }
                item { TestFocusable(itemSize, tag = focusableTag) }
                item { TestFocusable(itemSize, tag = "") }
                item { TestFocusable(itemSize, tag = "") }
            }
        }

        // Act.
        requestFocus()

        // Assert.
        rule.runOnIdle {
            assertThat(scrollState.firstVisibleItemIndex).isEqualTo(0)
            assertThat(scrollState.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun focusingOutOfBoundsItem_bringsItIntoView_whenMultipleFocusables() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                item { TestFocusable(itemSize, tag = focusableTag) }
                item { TestFocusable(itemSize, tag = "") }
                item { TestFocusable(itemSize, tag = "finalFocusable") }
                item { TestFocusable(itemSize, tag = "") }
                item { TestFocusable(itemSize, tag = "") }
            }
        }

        // Act.
        requestFocus()
        rule.runOnIdle {
            focusManager.moveFocus(Next)
            focusManager.moveFocus(Next)
        }

        // Assert.
        rule.onNodeWithTag("finalFocusable")
            .assertIsDisplayed()
            .assertIsFocused()
        rule.runOnIdle {
            assertThat(scrollState.firstVisibleItemIndex).isEqualTo(1)
            assertThat(scrollState.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun moveOutFromBoundaryItem_bringsNextItemIntoView() {
        // Arrange.
        val itemSize = with(rule.density) { 100.toDp() }
        rule.setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                item { TestFocusable(itemSize, tag = "1") }
                item { TestFocusable(itemSize, tag = "2") }
                item { TestFocusable(itemSize, tag = "finalFocusable") }
            }
        }
        requestFocus("2")

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(Next)
        }

        // Assert.
        rule.onNodeWithTag("finalFocusable").assertIsDisplayed()
        rule.runOnIdle { assertThat(scrollState.firstVisibleItemIndex).isEqualTo(1) }
        rule.runOnIdle { assertThat(scrollState.firstVisibleItemScrollOffset).isEqualTo(0) }
    }

    private fun ComposeContentTestRule.setContentForTest(
        composable: @Composable () -> Unit
    ) {
        setContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            composable()
        }
    }

    @Composable
    private fun ScrollableRowOrColumn(
        size: Dp,
        content: LazyListScope.() -> Unit
    ) {
        val modifier = Modifier
            .testTag(scrollableAreaTag)
            .size(size)
            .border(2.toDp(), Color.Black)

        when (orientation) {
            Vertical -> {
                LazyColumn(
                    state = scrollState,
                    modifier = modifier,
                    content = content
                )
            }

            Horizontal -> {
                LazyRow(
                    state = scrollState,
                    modifier = modifier,
                    content = content
                )
            }
        }
    }

    /**
     * Places a spacer before [content].
     */
    private fun LazyListScope.WithSpacerBefore(size: Dp, content: @Composable () -> Unit) {
        item { Spacer(Modifier.size(size)) }
        item { content() }
    }

    @Composable
    private fun TestFocusable(
        size: Dp,
        tag: String = focusableTag
    ) {
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

    private fun requestFocusAndScrollToStart() {
        requestFocus()
        // Reset scroll to top since requesting focus will scroll it.
        scrollToStart()
    }

    private fun requestFocus(tag: String = focusableTag) {
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    private fun scrollToStart() {
        runBlockingOnIdle {
            scrollState.scrollToItem(0, 0)
        }
    }

    private fun runBlockingOnIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = rule.runOnIdle {
            scope.launch(block = block)
        }
        runBlocking { job.join() }
    }

    private fun SemanticsNodeInteraction.assertScrollAxisPositionInRootIsEqualTo(expected: Dp) =
        when (orientation) {
            Vertical -> assertTopPositionInRootIsEqualTo(expected)
            Horizontal -> assertLeftPositionInRootIsEqualTo(expected)
        }
}