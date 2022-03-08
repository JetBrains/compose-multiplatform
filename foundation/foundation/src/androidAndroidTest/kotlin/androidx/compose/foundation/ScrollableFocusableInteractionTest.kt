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