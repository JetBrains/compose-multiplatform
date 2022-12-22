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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class ScrollableFocusableInteractionTest {
    companion object {
        fun initParameters() = arrayOf(
            arrayOf(Vertical, true),
            arrayOf(Vertical, false),
            arrayOf(Horizontal, true),
            arrayOf(Horizontal, false),
        )
    }

    private var orientation: Orientation? = null
    private var reverseScrolling: Boolean? = null

    private val parameters = initParameters()
    private fun runParametrizedTest(test: SkikoComposeUiTest.() -> Unit) {
        parameters.forEach {
            orientation = it[0] as Orientation
            reverseScrolling = it[1] as Boolean
            runSkikoComposeUiTest { test() }
        }
    }

    private val scrollableAreaTag = "scrollableArea"
    private val focusableTag = "focusable"
    private val focusRequester = FocusRequester()
    private val scrollState = ScrollState(initial = 0)
    private lateinit var focusManager: FocusManager

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
        orientation = null
        reverseScrolling = null
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenFullyInViewAndBecomesFullyHidden() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp(), focusRequester)
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(40.toDp())
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenFullyInViewAndBecomesPartiallyHidden() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp(), focusRequester)
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 95.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(85.toDp())
            .assertIsDisplayed()
    }

    @Test
    @Ignore // TODO: the test is failing
    fun scrollsFocusedFocusableIntoView_whenPartiallyInViewAndBecomesMoreHidden() = runParametrizedTest {
        var viewportSize by mutableStateOf(95.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp(), focusRequester)
                }
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-5).toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 91.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-5).toDp() else 81.toDp()
            )
            .assertIsDisplayed()
    }

    @Test
    @Ignore // TODO: the test is failing
    fun scrollsFocusedFocusableIntoView_whenPartiallyInViewAndBecomesFullyHidden() = runParametrizedTest {
        var viewportSize by mutableStateOf(95.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                WithSpacerBefore(90.toDp()) {
                    TestFocusable(size = 10.toDp(), focusRequester)
                }
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-5).toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 90.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-5).toDp() else 80.toDp()
            )
            .assertIsDisplayed()
    }

    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportAnimatedQuickly() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp(), focusRequester)
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Manually execute an "animation" that shrinks the viewport by twice the focusable's size
        // on every frame, for a few frames. The underlying bug in b/230756508 would lose track
        // of the focusable after the second frame.
        mainClock.autoAdvance = false
        viewportSize = 80.toDp()
        mainClock.advanceTimeByFrame()
        waitForIdle()
        viewportSize = 60.toDp()
        mainClock.advanceTimeByFrame()
        waitForIdle()
        viewportSize = 40.toDp()
        mainClock.advanceTimeByFrame()
        waitForIdle()

        // Resume the clock. The scroll animation should finish.
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertIsDisplayed()
    }

    @Test
    fun scrollFromViewportShrink_isInterrupted_byGesture() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp(), focusRequester)
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        mainClock.autoAdvance = false
        viewportSize = 80.toDp()
        // Run the first frame of the scroll animation.
        mainClock.advanceTimeByFrame()
        waitForIdle()

        // Interrupt the scroll by manually dragging.
        onNodeWithTag(scrollableAreaTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(viewConfiguration.touchSlop + 1, viewConfiguration.touchSlop + 1))
                up()
            }

        // Resume the clock. The animation scroll animation should have been interrupted and not
        // continue.
        mainClock.advanceTimeByFrame()
        mainClock.autoAdvance = true

        onNodeWithTag(focusableTag)
            .assertIsNotDisplayed()
    }

    /**
     * This test ensures that scrollable correctly cleans up its state when the scroll animation
     * triggered by shrinking the viewport is interrupted by something other than another shrink
     * and the focusable child does not change. If it's cleaned up correctly, expanding then re-
     * shrinking the viewport should trigger another animation.
     */
    @Test
    fun scrollsFocusedFocusableIntoView_whenViewportExpandedThenReshrunk_afterInterruption() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                Spacer(Modifier.size(90.toDp()))
                TestFocusable(size = 10.toDp(), focusRequester)
            }
        }
        requestFocusAndScrollToTop()
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(90.toDp())
            .assertIsDisplayed()
            .assertIsFocused()

        // Shrink the viewport to start the scroll animation.
        mainClock.autoAdvance = false
        viewportSize = 80.toDp()
        // Run the first frame of the scroll animation.
        mainClock.advanceTimeByFrame()
        waitForIdle()

        // Interrupt the scroll by manually dragging.
        onNodeWithTag(scrollableAreaTag)
            .performTouchInput {
                down(center)
                moveBy(Offset(viewConfiguration.touchSlop + 1, viewConfiguration.touchSlop + 1))
                up()
            }

        // Resume the clock. The animation scroll animation should have been interrupted and not
        // continue.
        mainClock.advanceTimeByFrame()
        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag(focusableTag)
            .assertIsFocused()
            .assertIsNotDisplayed()

        // Expand the viewport back to its original size to bring the focusable back into view.
        viewportSize = 100.toDp()
        waitForIdle()
//        Thread.sleep(2000)

        // Shrink the viewport again, this should trigger another scroll animation to keep the
        // scrollable in view.
        viewportSize = 50.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertIsDisplayed()
    }

    @Test
    @Ignore // TODO: the test is failing
    fun doesNotScrollFocusedFocusableIntoView_whenNotInViewAndViewportShrunk() = runParametrizedTest {
        val gapSize = 100.toDp()
        var viewportSize by mutableStateOf(gapSize)

        setContent {
            ScrollableRowOrColumn(viewportSize) {
                // Put a focusable just out of view.
                if (!reverseScrolling!!) {
                    Spacer(Modifier.size(gapSize))
                }
                TestFocusable(10.toDp(), focusRequester)
                if (reverseScrolling!!) {
                    Spacer(Modifier.size(gapSize))
                }
            }
        }
        requestFocusAndScrollToTop()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-10).toDp() else gapSize
            )
            .assertIsNotDisplayed()
            .assertIsFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()
        waitForIdle()

        // Focusable should not have moved since it was never in view.
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                // Focusable size minus the change in viewport size.
                if (reverseScrolling!!) (-10 - 50).toDp() else gapSize
            )
            .assertIsNotDisplayed()
    }

    @Test
    @Ignore // TODO: the test is failing
    fun doesNotScrollUnfocusedFocusableIntoView_whenViewportShrunk() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the bottom of the viewport.
                WithSpacerBefore(size = 90.toDp()) {
                    TestFocusable(size = 10.toDp(), focusRequester)
                }
            }
        }
        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) 0.toDp() else 90.toDp()
            )
            .assertIsDisplayed()
            .assertIsNotFocused()

        // Act: Shrink the viewport.
        viewportSize = 50.toDp()
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) (-50).toDp() else 90.toDp()
            )
            .assertIsNotDisplayed()
    }

    @Test
    @Ignore // TODO: the test is failing
    fun doesNotScrollFocusedFocusableIntoView_whenPartiallyInViewAndViewportGrown() = runParametrizedTest {
        val initialViewPortSize = 50.toDp()
        val itemSize = 30.toDp()
        var viewportSize by mutableStateOf(initialViewPortSize)

        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            ScrollableRowOrColumn(size = viewportSize) {
                // Put a focusable in the middle of the viewport, but ensure we're a lot bigger
                // than the viewport so it can grow without requiring a scroll.
                Spacer(Modifier.size(initialViewPortSize * 2))
                TestFocusable(size = itemSize, focusRequester)
                Spacer(Modifier.size(initialViewPortSize * 2))
            }
        }
        scrollToTop()
        runOnIdle {
            focusRequester.requestFocus()
        }

        // Requesting focus will bring the entire focused item into view (at the bottom of the
        // viewport), then scroll up by half the focusable height so it's partially in view.
        runOnIdle {
            scope!!.launch {
                val halfFocusableSize = with(density) { (itemSize / 2).toPx() }
                scrollState.scrollBy(-halfFocusableSize)
            }
        }

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(
                if (reverseScrolling!!) -itemSize / 2 else initialViewPortSize - (itemSize / 2)
            )
            .assertIsDisplayed()
            .assertIsFocused()

        // Act: Grow the viewport.
        viewportSize *= 2
        waitForIdle()

        onNodeWithTag(focusableTag)
            .assertScrollAxisPositionInRootIsEqualTo(initialViewPortSize - (itemSize / 2))
            .assertIsDisplayed()
    }

    @Test
    fun scrollsToNewFocusable_whenFocusedChildChangesDuringAnimation() = runParametrizedTest {
        var viewportSize by mutableStateOf(100.toDp())
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        @Composable
        fun Focusable1() {
            Box(
                Modifier
                    .size(10.toDp())
                    .background(Color.Blue)
                    .testTag("focusable1")
                    .focusRequester(focusRequester1)
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
                    .focusRequester(focusRequester2)
                    .focusable()
            )
        }

        setContent {
            ScrollableRowOrColumn(size = viewportSize) {
                if (reverseScrolling!!) Focusable2() else Focusable1()
                Spacer(Modifier.size(100.toDp()))
                if (reverseScrolling!!) Focusable1() else Focusable2()
            }
        }
        // When focusable2 gets focus, focusable1 should be scrolled out of view.
        runOnIdle {
            focusRequester2.requestFocus()
        }
        onNodeWithTag("focusable1").assertIsNotDisplayed()
        onNodeWithTag("focusable2").assertIsDisplayed()
        // Pause the clock because we need to do some work in the middle of an animation.
        mainClock.autoAdvance = false

        // Shrink the viewport, which should scroll to keep focusable2 in-view.
        runOnIdle {
            viewportSize = 20.toDp()
        }

        // Tick the clock forward to let the animation start and run a bit.
        repeat(3) { mainClock.advanceTimeByFrame() }
        onNodeWithTag("focusable1").assertIsNotDisplayed()
        onNodeWithTag("focusable2").assertIsNotDisplayed()

        runOnIdle {
            focusRequester1.requestFocus()
        }
        // Resume the clock, allow animation to finish.
        mainClock.autoAdvance = true
        waitForIdle()

        onNodeWithTag("focusable1").assertIsDisplayed()
        onNodeWithTag("focusable2").assertIsNotDisplayed()
    }

    @Test
    fun focusingOnVisibleItemDoesNotScroll_whenMultipleFocusables() = runParametrizedTest {
        // Arrange.
        val itemSize = with(density) { 100.toDp() }
        setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 3) {
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, focusRequester)
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, null)
            }
        }

        // Act.
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle { assertThat(scrollState.value).isEqualTo(0) }
    }

    @Test
    fun focusingOutOfBoundsItem_bringsItIntoView_whenMultipleFocusables() = runParametrizedTest {
        // Arrange.
        val itemSize = with(density) { 100.toDp() }
        setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, focusRequester)
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, null)
            }
        }

        // Act.
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    @Test
    fun moveOutFromBoundaryItem_bringsNextItemIntoView() = runParametrizedTest {
        // Arrange.
        val itemSize = with(density) { 100.toDp() }
        setContentForTest {
            ScrollableRowOrColumn(size = itemSize * 2) {
                TestFocusable(itemSize, null)
                TestFocusable(itemSize, focusRequester)
                TestFocusable(itemSize, null)
            }
        }
        runOnIdle { focusRequester.requestFocus() }

        // Act.
        runOnIdle { focusManager.moveFocus(if (reverseScrolling!!) Previous else Next) }

        // Assert.
        runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    private fun SkikoComposeUiTest.setContentForTest(
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

        when (orientation!!) {
            Vertical -> {
                Column(
                    // Uses scrollable under the hood.
                    modifier.verticalScroll(
                        state = scrollState,
                        reverseScrolling = reverseScrolling!!
                    )
                ) { content() }
            }
            Horizontal -> {
                Row(
                    // Uses scrollable under the hood.
                    modifier.horizontalScroll(
                        state = scrollState,
                        reverseScrolling = reverseScrolling!!
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
        if (!reverseScrolling!!) {
            Spacer(Modifier.size(size))
        }
        content()
        if (reverseScrolling!!) {
            Spacer(Modifier.size(size))
        }
    }

    @Composable
    private fun TestFocusable(size: Dp, focusRequester: FocusRequester?) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
            Modifier
                .testTag(focusableTag)
                .size(size)
                .border(1.dp, Color.White)
                .background(if (isFocused) Color.Blue else Color.Black)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .focusable(interactionSource = interactionSource)
        )
    }

    /**
     * Sizes and offsets of the composables in these tests must be specified using this function.
     * If they're specified using `xx.dp` syntax, a rounding error somewhere in the layout system
     * will cause the pixel values to be off-by-one.
     */
    private fun Int.toDp(): Dp = with(Density(1f)) { this@toDp.toDp() }

    private fun SkikoComposeUiTest.requestFocusAndScrollToTop() {
        runOnIdle {
            focusRequester.requestFocus()
        }
        scrollToTop()
    }

    private fun SkikoComposeUiTest.scrollToTop() {
        waitForIdle()
        // Reset scroll to top since requesting focus will scroll it.
        MainScope().launch {
            scrollState.scrollTo(if (reverseScrolling!!) -scrollState.maxValue else 0)
        }
    }

    private fun SemanticsNodeInteraction.assertScrollAxisPositionInRootIsEqualTo(expected: Dp) =
        when (orientation!!) {
            Vertical -> assertTopPositionInRootIsEqualTo(expected)
            Horizontal -> assertLeftPositionInRootIsEqualTo(expected)
        }
}
