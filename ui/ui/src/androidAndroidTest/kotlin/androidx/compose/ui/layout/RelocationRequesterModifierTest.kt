/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.layout

import android.os.Build.VERSION_CODES.O
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.GOLDEN_UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = O)
class RelocationRequesterModifierTest(private val orientation: Orientation) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    private val parentBox = "parent box"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Orientation> = arrayOf(Horizontal, Vertical)
    }

    @Test
    fun noScrollableParent_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.dp, 50.dp)
                            Vertical -> Modifier.size(50.dp, 100.dp)
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(Blue)
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    @Test
    fun noScrollableParent_itemNotVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.dp, 50.dp)
                            Vertical -> Modifier.size(50.dp, 100.dp)
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 150.dp)
                                Vertical -> Modifier.offset(y = 150.dp)
                            }
                        )
                        .size(50.dp)
                        .background(Blue)
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "grayRectangleHorizontal" else "grayRectangleVertical")
    }

    @Test
    fun itemAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(Blue)
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    @Test
    fun itemAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 50.dp)
                                Vertical -> Modifier.offset(y = 50.dp)
                            }
                        )
                        .size(50.dp)
                        .background(Blue)
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxRight" else "blueBoxBottom")
    }

    @Test
    fun itemAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 25.dp)
                                Vertical -> Modifier.offset(y = 25.dp)
                            }
                        )
                        .size(50.dp)
                        .background(Blue)
                        .relocationRequester(relocationRequester)
                )
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxCenterHorizontal" else "blueBoxCenterVertical")
    }

    @Test
    fun itemBiggerThanParentAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        rule.setContent {
            Box(
                Modifier
                    .size(50.dp)
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier.horizontalScrollWithRelocation(rememberScrollState())
                            Vertical ->
                                Modifier.verticalScrollWithRelocation(rememberScrollState())
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(Modifier.relocationRequester(relocationRequester)) {
                    Box(Modifier.size(50.dp).background(Blue))
                    Box(Modifier.size(50.dp).background(Green))
                    Box(Modifier.size(50.dp).background(Red))
                }
            }
        }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot("blueBox")
    }

    @Test
    fun itemBiggerThanParentAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.dp)
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScrollWithRelocation(scrollState)
                            Vertical -> Modifier.verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(Modifier.relocationRequester(relocationRequester)) {
                    Box(Modifier.size(50.dp).background(Red))
                    Box(Modifier.size(50.dp).background(Green))
                    Box(Modifier.size(50.dp).background(Blue))
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot("blueBox")
    }

    @Test
    fun itemBiggerThanParentAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.dp)
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScrollWithRelocation(scrollState)
                            Vertical -> Modifier.verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(Modifier.relocationRequester(relocationRequester)) {
                    Box(Modifier.size(50.dp).background(Green))
                    Box(Modifier.size(50.dp).background(Blue))
                    Box(Modifier.size(50.dp).background(Red))
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot("blueBox")
    }

    @Test
    fun childBeforeVisibleBounds_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.dp, 50.dp)
                        Vertical -> Modifier.size(50.dp, 200.dp)
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 50.dp)
                                    Vertical -> Modifier.offset(y = 50.dp)
                                }
                            )
                            .size(50.dp)
                            .background(Blue)
                            .relocationRequester(relocationRequester)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    @Test
    fun childAfterVisibleBounds_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.dp, 50.dp)
                        Vertical -> Modifier.size(50.dp, 200.dp)
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.dp)
                                    Vertical -> Modifier.offset(y = 150.dp)
                                }
                            )
                            .size(50.dp)
                            .background(Blue)
                            .relocationRequester(relocationRequester)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxRight" else "blueBoxBottom")
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                Box(Modifier.size(200.dp)) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 25.dp)
                                    Vertical -> Modifier.offset(y = 25.dp)
                                }
                            )
                            .size(50.dp)
                            .background(Blue)
                            .relocationRequester(relocationRequester)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.dp, 50.dp)
                        Vertical -> Modifier.size(50.dp, 200.dp)
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.dp)
                                    Vertical -> Modifier.offset(y = 150.dp)
                                }
                            )
                            .size(50.dp)
                            .background(Blue)
                            .relocationRequester(relocationRequester)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxRight" else "blueBoxBottom")
    }

    @Test
    fun multipleParentsAreScrolledSoThatChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        rule.setContent {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .horizontalScrollWithRelocation(grandParentScrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .verticalScrollWithRelocation(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal ->
                                    Modifier
                                        .size(200.dp, 50.dp)
                                        .horizontalScrollWithRelocation(parentScrollState)
                                Vertical ->
                                    Modifier
                                        .size(50.dp, 200.dp)
                                        .verticalScrollWithRelocation(parentScrollState)
                            }
                        )
                ) {
                    Box(
                        when (orientation) {
                            Horizontal -> Modifier.size(400.dp, 50.dp)
                            Vertical -> Modifier.size(50.dp, 400.dp)
                        }
                    ) {
                        Box(
                            Modifier
                                .then(
                                    when (orientation) {
                                        Horizontal -> Modifier.offset(x = 25.dp)
                                        Vertical -> Modifier.offset(y = 25.dp)
                                    }
                                )
                                .size(50.dp)
                                .background(Blue)
                                .relocationRequester(relocationRequester)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    @Test
    fun multipleParentsAreScrolledInDifferentDirectionsSoThatChildIsVisible() {
        // Arrange.
        val relocationRequester = RelocationRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        rule.setContent {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .verticalScrollWithRelocation(grandParentScrollState)
                            Vertical ->
                                Modifier
                                    .size(50.dp, 100.dp)
                                    .horizontalScrollWithRelocation(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(100.dp)
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal ->
                                    Modifier.horizontalScrollWithRelocation(parentScrollState)
                                Vertical ->
                                    Modifier.verticalScrollWithRelocation(parentScrollState)
                            }
                        )
                ) {
                    Box(Modifier.size(200.dp)) {
                        Box(
                            Modifier
                                .offset(x = 25.dp, y = 25.dp)
                                .size(50.dp)
                                .background(Blue)
                                .relocationRequester(relocationRequester)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { relocationRequester.bringIntoView() }

        // Assert.
        assertScreenshot(if (horizontal) "blueBoxLeft" else "blueBoxTop")
    }

    private val horizontal: Boolean get() = (orientation == Horizontal)

    @Composable
    private fun RowOrColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        when (orientation) {
            Horizontal -> Row(modifier) { content() }
            Vertical -> Column(modifier) { content() }
        }
    }

    @RequiresApi(O)
    private fun assertScreenshot(screenshot: String) {
        rule.onNodeWithTag(parentBox)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "bringIntoParentBounds_$screenshot")
    }

    private fun runBlockingAndAwaitIdle(block: suspend CoroutineScope.() -> Unit) {
        runBlockingTest {
            withContext(TestMonotonicFrameClock(this)) {
                block()
                advanceUntilIdle()
            }
        }
        rule.waitForIdle()
    }
}

// This is a helper function that users will have to use since experimental "ui" API cannot be used
// inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is added
// to Scrollable, users can use Modifier.horizontalScroll directly.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.horizontalScrollWithRelocation(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier {
    return this
        .onRelocationRequest(
            onProvideDestination = { rect, layoutCoordinates ->
                val size = layoutCoordinates.size.toSize()
                rect.translate(relocationDistance(rect.left, rect.right, size.width), 0f)
            },
            onPerformRelocation = { source, destination ->
                val offset = destination.left - source.left
                state.animateScrollBy(if (reverseScrolling) -offset else offset)
            }
        )
        .horizontalScroll(state, enabled, flingBehavior, reverseScrolling)
}

// This is a helper function that users will have to use since experimental "ui" API cannot be used
// inside Scrollable, which is ihe "foundation" package. After onRelocationRequest is added
// to Scrollable, users can use Modifier.verticalScroll directly.
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.verticalScrollWithRelocation(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false
): Modifier {
    return this
        .onRelocationRequest(
            onProvideDestination = { rect, layoutCoordinates ->
                val size = layoutCoordinates.size.toSize()
                rect.translate(0f, relocationDistance(rect.top, rect.bottom, size.height))
            },
            onPerformRelocation = { source, destination ->
                val offset = destination.top - source.top
                state.animateScrollBy(if (reverseScrolling) -offset else offset)
            }
        )
        .verticalScroll(state, enabled, flingBehavior, reverseScrolling)
}

// Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
// closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
// The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
// 'bottom').
private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
    // If the item is already visible, no need to scroll.
    leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

    // If the item is visible but larger than the parent, we don't scroll.
    leadingEdge < 0 && trailingEdge > parentSize -> 0f

    // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
    abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
    else -> trailingEdge - parentSize
}
