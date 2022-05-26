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

package androidx.compose.foundation.lazy.list

import android.os.Build
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyNotDefined
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListTest(orientation: Orientation) : BaseLazyListTestWithOrientation(orientation) {
    private val LazyListTag = "LazyListTag"
    private val firstItemTag = "firstItemTag"

    @Test
    fun lazyListShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.mainAxisSize(200.dp)) {
                item {
                    Spacer(
                        Modifier.mainAxisSize(40.dp)
                            .then(fillParentMaxCrossAxis())
                            .testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.mainAxisSize(40.dp).then(fillParentMaxCrossAxis()).testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(
                        Modifier.mainAxisSize(41.dp).then(fillParentMaxCrossAxis())
                            .testTag("$index-$item")
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("0-3")
            .assertIsDisplayed()

        rule.onNodeWithTag("1-4")
            .assertIsDisplayed()

        rule.onNodeWithTag("2-5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyListAllowEmptyListItems() {
        val itemTag = "itemTag"

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow {
                items(emptyList<Any>()) { }
                item {
                    Spacer(Modifier.size(10.dp).testTag(itemTag))
                }
            }
        }

        rule.onNodeWithTag(itemTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyListAllowsNullableItems() {
        val items = listOf("1", null, "3")
        val nullTestTag = "nullTestTag"

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.mainAxisSize(200.dp)) {
                items(items) {
                    if (it != null) {
                        Spacer(
                            Modifier.mainAxisSize(101.dp)
                                .then(fillParentMaxCrossAxis())
                                .testTag(it)
                        )
                    } else {
                        Spacer(
                            Modifier.mainAxisSize(101.dp).then(fillParentMaxCrossAxis())
                                .testTag(nullTestTag)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag(nullTestTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun lazyListOnlyVisibleItemsAdded() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.mainAxisSize(200.dp)) {
                LazyColumnOrRow {
                    items(items) {
                        Spacer(
                            Modifier.mainAxisSize(101.dp).then(fillParentMaxCrossAxis()).testTag(it)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyListScrollToShowItems123() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.mainAxisSize(200.dp)) {
                LazyColumnOrRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(
                            Modifier.mainAxisSize(101.dp).then(fillParentMaxCrossAxis()).testTag(it)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(50.dp)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertIsNotDisplayed()
    }

    @Test
    fun lazyListScrollToHideFirstItem() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.mainAxisSize(200.dp)) {
                LazyColumnOrRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(
                            Modifier.mainAxisSize(101.dp).then(fillParentMaxCrossAxis()).testTag(it)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(105.dp)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()
    }

    @Test
    fun lazyListScrollToShowItems234() {
        val items = (1..4).map { it.toString() }

        rule.setContentWithTestViewConfiguration {
            Box(Modifier.mainAxisSize(200.dp)) {
                LazyColumnOrRow(Modifier.testTag(LazyListTag)) {
                    items(items) {
                        Spacer(
                            Modifier.mainAxisSize(101.dp).then(fillParentMaxCrossAxis()).testTag(it)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(150.dp)

        rule.onNodeWithTag("1")
            .assertIsNotDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()
    }

    @Test
    fun lazyListWrapsContent() = with(rule.density) {
        val itemInsideLazyList = "itemInsideLazyList"
        val itemOutsideLazyList = "itemOutsideLazyList"
        var sameSizeItems by mutableStateOf(true)

        rule.setContentWithTestViewConfiguration {
            Column {
                LazyColumnOrRow(Modifier.testTag(LazyListTag)) {
                    items(listOf(1, 2)) {
                        if (it == 1) {
                            Spacer(Modifier.size(50.dp).testTag(itemInsideLazyList))
                        } else {
                            Spacer(Modifier.size(if (sameSizeItems) 50.dp else 70.dp))
                        }
                    }
                }
                Spacer(Modifier.size(50.dp).testTag(itemOutsideLazyList))
            }
        }

        rule.onNodeWithTag(itemInsideLazyList)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyList)
            .assertIsDisplayed()

        var lazyListBounds = rule.onNodeWithTag(LazyListTag).getUnclippedBoundsInRoot()
        var mainAxisEndBound = if (vertical) lazyListBounds.bottom else lazyListBounds.right
        var crossAxisEndBound = if (vertical) lazyListBounds.right else lazyListBounds.bottom

        assertThat(lazyListBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(mainAxisEndBound.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        assertThat(lazyListBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(crossAxisEndBound.roundToPx()).isWithin1PixelFrom(50.dp.roundToPx())

        rule.runOnIdle {
            sameSizeItems = false
        }

        rule.waitForIdle()

        rule.onNodeWithTag(itemInsideLazyList)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyList)
            .assertIsDisplayed()

        lazyListBounds = rule.onNodeWithTag(LazyListTag).getUnclippedBoundsInRoot()
        mainAxisEndBound = if (vertical) lazyListBounds.bottom else lazyListBounds.right
        crossAxisEndBound = if (vertical) lazyListBounds.right else lazyListBounds.bottom

        assertThat(lazyListBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(mainAxisEndBound.roundToPx()).isWithin1PixelFrom(120.dp.roundToPx())
        assertThat(lazyListBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(crossAxisEndBound.roundToPx()).isWithin1PixelFrom(70.dp.roundToPx())
    }

    @Test
    fun compositionsAreDisposed_whenNodesAreScrolledOff() {
        var composed: Boolean
        var disposed = false
        // Ten 31dp spacers in a 300dp list
        val latch = CountDownLatch(10)

        rule.setContentWithTestViewConfiguration {
            // Fixed size to eliminate device size as a factor
            Box(Modifier.testTag(LazyListTag).mainAxisSize(300.dp)) {
                LazyColumnOrRow(Modifier.fillMaxSize()) {
                    items(50) {
                        DisposableEffect(NeverEqualObject) {
                            composed = true
                            // Signal when everything is done composing
                            latch.countDown()
                            onDispose {
                                disposed = true
                            }
                        }

                        // There will be 10 of these in the 300dp box
                        Spacer(Modifier.mainAxisSize(31.dp))
                    }
                }
            }
        }

        latch.await()
        composed = false

        assertWithMessage("Compositions were disposed before we did any scrolling")
            .that(disposed).isFalse()

        // Mostly a validity check, this is not part of the behavior under test
        assertWithMessage("Additional composition occurred for no apparent reason")
            .that(composed).isFalse()

        rule.onNodeWithTag(LazyListTag)
            .performTouchInput { if (vertical) swipeUp() else swipeLeft() }

        rule.waitForIdle()

        assertWithMessage("No additional items were composed after scroll, scroll didn't work")
            .that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "No compositions were disposed after scrolling, compositions were leaked"
        ).that(disposed).isTrue()
    }

    @Test
    fun whenItemsAreInitiallyCreatedWith0SizeWeCanScrollWhenTheyExpanded() {
        val thirdTag = "third"
        val items = (1..3).toList()
        var thirdHasSize by mutableStateOf(false)

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.fillMaxCrossAxis()
                    .mainAxisSize(100.dp)
                    .testTag(LazyListTag)
            ) {
                items(items) {
                    if (it == 3) {
                        Spacer(
                            Modifier.testTag(thirdTag)
                                .then(fillParentMaxCrossAxis())
                                .mainAxisSize(if (thirdHasSize) 60.dp else 0.dp)
                        )
                    } else {
                        Spacer(Modifier.then(fillParentMaxCrossAxis()).mainAxisSize(60.dp))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(21.dp)

        rule.onNodeWithTag(thirdTag)
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            thirdHasSize = true
        }

        rule.waitForIdle()

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(10.dp)

        rule.onNodeWithTag(thirdTag)
            .assertIsDisplayed()
    }

    @Test
    fun itemFillingParentWidth() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth().requiredHeight(50.dp).testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp).fillParentMaxHeight().testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentWidthFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth(0.7f)
                            .requiredHeight(50.dp)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeightFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp)
                            .fillParentMaxHeight(0.3f)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(45.dp)
    }

    @Test
    fun itemFillingParentSizeFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize(0.5f).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(75.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        var parentSize by mutableStateOf(100.dp)
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(parentSize)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
                }
            }
        }

        rule.runOnIdle {
            parentSize = 150.dp
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun whenNotAnymoreAvailableItemWasDisplayed() {
        var items by mutableStateOf((1..30).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 16-20
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(300.dp)

        rule.runOnIdle {
            items = (1..10).toList()
        }

        // there is no item 16 anymore so we will just display the last items 6-10
        rule.onNodeWithTag("6")
            .assertStartPositionIsAlmost(0.dp)
    }

    @Test
    fun whenFewDisplayedItemsWereRemoved() {
        var items by mutableStateOf((1..10).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(100.dp)

        rule.runOnIdle {
            items = (1..8).toList()
        }

        // there are no more items 9 and 10, so we have to scroll back
        rule.onNodeWithTag("4")
            .assertStartPositionIsAlmost(0.dp)
    }

    @Test
    fun whenItemsBecameEmpty() {
        var items by mutableStateOf((1..10).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.requiredSizeIn(maxHeight = 100.dp, maxWidth = 100.dp)
                    .testTag(LazyListTag)
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 2-6
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(20.dp)

        rule.runOnIdle {
            items = emptyList()
        }

        // there are no more items so the lazy list is zero sized
        rule.onNodeWithTag(LazyListTag)
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)

        // and has no children
        rule.onNodeWithTag("1")
            .assertDoesNotExist()
        rule.onNodeWithTag("2")
            .assertDoesNotExist()
    }

    @Test
    fun scrollBackAndForth() {
        val items by mutableStateOf((1..20).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(100.dp)

        // and scroll back
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy((-100).dp)

        rule.onNodeWithTag("1")
            .assertStartPositionIsAlmost(0.dp)
    }

    @Test
    fun tryToScrollBackwardWhenAlreadyOnTop() {
        val items by mutableStateOf((1..20).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        // we already displaying the first item, so this should do nothing
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy((-50).dp)

        rule.onNodeWithTag("1")
            .assertStartPositionIsAlmost(0.dp)
        rule.onNodeWithTag("5")
            .assertStartPositionIsAlmost(80.dp)
    }

    @Test
    fun contentOfNotStableItemsIsNotRecomposedDuringScroll() {
        val items = listOf(NotStable(1), NotStable(2))
        var firstItemRecomposed = 0
        var secondItemRecomposed = 0
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    if (it.count == 1) {
                        firstItemRecomposed++
                    } else {
                        secondItemRecomposed++
                    }
                    Spacer(Modifier.requiredSize(75.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(50.dp)

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }
    }

    @Test
    fun onlyOneMeasurePassForScrollEvent() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            state.prefetchingEnabled = false
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        val initialMeasurePasses = state.numMeasurePasses

        rule.runOnIdle {
            with(rule.density) {
                state.onScroll(-110.dp.toPx())
            }
        }

        rule.waitForIdle()

        assertThat(state.numMeasurePasses).isEqualTo(initialMeasurePasses + 1)
    }

    @Test
    fun onlyOneInitialMeasurePass() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.numMeasurePasses).isEqualTo(1)
        }
    }

    @Test
    fun stateUpdatedAfterScroll() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(30.dp)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)

            with(rule.density) {
                // TODO(b/169232491): test scrolling doesn't appear to be scrolling exactly the right
                //  number of pixels
                val expectedOffset = 10.dp.roundToPx()
                val tolerance = 2.dp.roundToPx()
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun stateUpdatedAfterScrollWithinTheSameItem() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(10.dp)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) {
                val expectedOffset = 10.dp.roundToPx()
                val tolerance = 2.dp.roundToPx()
                assertThat(state.firstVisibleItemScrollOffset)
                    .isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun scroll_makeListSmaller_scroll() {
        var items by mutableStateOf((1..100).toList())
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(10.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(300.dp)

        rule.runOnIdle {
            items = (1..11).toList()
        }

        // try to scroll after the data set has been updated. this was causing a crash previously
        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy((-10).dp)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    @Test
    fun initialScrollIsApplied() {
        val items by mutableStateOf((0..20).toList())
        lateinit var state: LazyListState
        val expectedOffset = with(rule.density) { 10.dp.roundToPx() }
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState(2, expectedOffset)
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
        }

        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun stateIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        var state: LazyListState? = null
        restorationTester.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state!!
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(30.dp)

        val (index, scrollOffset) = rule.runOnIdle {
            state!!.firstVisibleItemIndex to state!!.firstVisibleItemScrollOffset
        }

        state = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.firstVisibleItemIndex).isEqualTo(index)
            assertThat(state!!.firstVisibleItemScrollOffset).isEqualTo(scrollOffset)
        }
    }

    @Test
    fun snapToItemIndex() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3, 10)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun itemsAreNotRedrawnDuringScroll() {
        val items = (0..20).toList()
        val redrawCount = Array(6) { 0 }
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(
                        Modifier.requiredSize(20.dp)
                            .drawBehind { redrawCount[it]++ }
                    )
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollMainAxisBy(10.dp)

        rule.runOnIdle {
            redrawCount.forEachIndexed { index, i ->
                assertWithMessage("Item with index $index was redrawn $i times")
                    .that(i).isEqualTo(1)
            }
        }
    }

    @Test
    fun itemInvalidationIsNotCausingAnotherItemToRedraw() {
        val redrawCount = Array(2) { 0 }
        var stateUsedInDrawScope by mutableStateOf(false)
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(2) {
                    Spacer(
                        Modifier.requiredSize(50.dp)
                            .drawBehind {
                                redrawCount[it]++
                                if (it == 1) {
                                    stateUsedInDrawScope.hashCode()
                                }
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            stateUsedInDrawScope = true
        }

        rule.runOnIdle {
            assertWithMessage("First items is not expected to be redrawn")
                .that(redrawCount[0]).isEqualTo(1)
            assertWithMessage("Second items is expected to be redrawn")
                .that(redrawCount[1]).isEqualTo(2)
        }
    }

    @Test
    fun notVisibleAnymoreItemNotAffectingCrossAxisSize() {
        val itemSize = with(rule.density) { 30.toDp() }
        val itemSizeMinusOne = with(rule.density) { 29.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSizeMinusOne).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(2) {
                    Spacer(
                        if (it == 0) {
                            Modifier.crossAxisSize(30.dp).mainAxisSize(itemSizeMinusOne)
                        } else {
                            Modifier.crossAxisSize(20.dp).mainAxisSize(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertCrossAxisSizeIsEqualTo(20.dp)
    }

    @Test
    fun itemStillVisibleAfterOverscrollIsAffectingCrossAxisSize() {
        val items = (0..2).toList()
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 1.75f).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(items) {
                    Spacer(
                        if (it == 0) {
                            Modifier.crossAxisSize(30.dp).mainAxisSize(itemSize / 2)
                        } else if (it == 1) {
                            Modifier.crossAxisSize(20.dp).mainAxisSize(itemSize / 2)
                        } else {
                            Modifier.crossAxisSize(20.dp).mainAxisSize(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertCrossAxisSizeIsEqualTo(30.dp)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow {
                items(items) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertStartPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.requiredSize(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertStartPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertStartPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertStartPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: LazyListState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(Modifier.fillMaxCrossAxis().mainAxisSize(10.dp), state) {
                items(count) { index ->
                    composedIndexes.add(index)
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            composedIndexes.clear()
            count = 10
            runBlocking(AutoTestFrameClock()) {
                state.scrollToItem(50)
            }
            composedIndexes.forEach {
                assertThat(it).isLessThan(count)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
        }
    }

    @Test
    fun overscrollingBackwardFromNotTheFirstPosition() {
        val containerTag = "container"
        val itemSizePx = 10
        val itemSizeDp = with(rule.density) { itemSizePx.toDp() }
        val containerSize = itemSizeDp * 5
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier
                    .testTag(containerTag)
                    .size(containerSize)
            ) {
                LazyColumnOrRow(
                    Modifier
                        .testTag(LazyListTag)
                        .background(Color.Blue),
                    state = rememberLazyListState(2, 5)
                ) {
                    items(100) {
                        Box(
                            Modifier
                                .fillMaxCrossAxis()
                                .mainAxisSize(itemSizeDp)
                                .testTag("$it")
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .performTouchInput {
                // we do move manually and not with swipe() utility because we want to have one
                // drag gesture, not multiple smaller ones
                down(center)
                if (vertical) {
                    moveBy(Offset(0f, -TestTouchSlop))
                    moveBy(
                        Offset(
                            0f,
                            itemSizePx * 15f // large value which makes us overscroll
                        )
                    )
                } else {
                    moveBy(Offset(-TestTouchSlop, 0f))
                    moveBy(
                        Offset(
                            itemSizePx * 15f, // large value which makes us overscroll
                            0f
                        )
                    )
                }
                up()
            }

        rule.onNodeWithTag(LazyListTag)
            .assertMainAxisSizeIsEqualTo(containerSize)

        rule.onNodeWithTag("0")
            .assertStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("4")
            .assertStartPositionInRootIsEqualTo(containerSize - itemSizeDp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun doesNotClipHorizontalOverdraw() {
        rule.setContent {
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                LazyColumnOrRow(
                    Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    rememberLazyListState(1)
                ) {
                    items(4) {
                        Box(Modifier.size(20.dp).drawOutsideOfBounds())
                    }
                }
            }
        }

        val horizontalPadding = if (vertical) 0.dp else 20.dp
        val verticalPadding = if (vertical) 20.dp else 0.dp

        rule.onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = horizontalPadding,
                verticalPadding = verticalPadding
            )
    }

    @Test
    fun initialScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        lateinit var state: LazyListState
        var itemsCount by mutableStateOf(0)
        rule.setContent {
            state = rememberLazyListState(2, 10)
            LazyColumnOrRow(Modifier.fillMaxSize(), state) {
                items(itemsCount) {
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            itemsCount = 100
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun restoredScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        lateinit var state: LazyListState
        var itemsCount = 100
        val recomposeCounter = mutableStateOf(0)
        val tester = StateRestorationTester(rule)
        tester.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(Modifier.fillMaxSize(), state) {
                recomposeCounter.value
                items(itemsCount) {
                    Box(Modifier.size(20.dp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2, 10)
            }
            itemsCount = 0
        }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            itemsCount = 100
            recomposeCounter.value = 1
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun animateScrollToItemDoesNotScrollPastItem() {
        lateinit var state: LazyListState
        var target = 0
        var reverse = false
        rule.setContent {
            val listState = rememberLazyListState()
            SideEffect {
                state = listState
            }
            LazyColumnOrRow(Modifier.fillMaxSize(), listState) {
                items(2500) { _ ->
                    Box(Modifier.size(100.dp))
                }
            }

            if (reverse) {
                assertThat(listState.firstVisibleItemIndex).isAtLeast(target)
            } else {
                assertThat(listState.firstVisibleItemIndex).isAtMost(target)
            }
        }

        // Try a bunch of different targets with varying spacing
        listOf(500, 800, 1500, 1600, 1800).forEach {
            target = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.animateScrollToItem(target)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(target)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }

        reverse = true

        listOf(1600, 1500, 800, 500, 0).forEach {
            target = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.animateScrollToItem(target)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(target)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }
    }

    @Test
    fun animateScrollToTheLastItemWhenItemsAreLargerThenTheScreen() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(Modifier.crossAxisSize(150.dp).mainAxisSize(100.dp), state) {
                items(20) {
                    Box(Modifier.size(150.dp))
                }
            }
        }

        // Try a bunch of different start indexes
        listOf(0, 5, 12).forEach {
            val startIndex = it
            rule.runOnIdle {
                runBlocking(AutoTestFrameClock()) {
                    state.scrollToItem(startIndex)
                    state.animateScrollToItem(19)
                }
            }

            rule.runOnIdle {
                assertThat(state.firstVisibleItemIndex).isEqualTo(19)
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            }
        }
    }

    @Test
    fun recreatingContentLambdaTriggersItemRecomposition() {
        val countState = mutableStateOf(0)
        rule.setContent {
            val count = countState.value
            LazyColumnOrRow {
                item {
                    BasicText(text = "Count $count")
                }
            }
        }

        rule.onNodeWithText("Count 0")
            .assertIsDisplayed()

        rule.runOnIdle {
            countState.value++
        }

        rule.onNodeWithText("Count 1")
            .assertIsDisplayed()
    }

    @Test
    fun semanticsScroll_isAnimated() {
        rule.mainClock.autoAdvance = false
        val state = LazyListState()

        rule.setContent {
            LazyColumnOrRow(Modifier.testTag(LazyListTag), state = state) {
                items(50) {
                    Box(Modifier.mainAxisSize(200.dp))
                }
            }
        }

        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag(LazyListTag).performSemanticsAction(SemanticsActions.ScrollBy) {
            if (vertical) {
                it(0f, 100f)
            } else {
                it(100f, 0f)
            }
        }

        // We haven't advanced time yet, make sure it's still zero
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        // Advance and make sure we're partway through
        // Note that we need two frames for the animation to actually happen
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // The items are 200dp each, so still the first one, but offset
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
        assertThat(state.firstVisibleItemScrollOffset).isLessThan(100)

        // Finish the scroll, make sure we're at the target
        rule.mainClock.advanceTimeBy(5000)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(100)
    }

    @Test
    fun maxIntElements() {
        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.requiredSize(itemSize * 3),
                state = LazyListState(firstVisibleItemIndex = Int.MAX_VALUE - 3)
            ) {
                items(Int.MAX_VALUE) {
                    Box(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("${Int.MAX_VALUE - 3}").assertStartPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("${Int.MAX_VALUE - 2}").assertStartPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("${Int.MAX_VALUE - 1}").assertStartPositionInRootIsEqualTo(itemSize * 2)

        rule.onNodeWithTag("${Int.MAX_VALUE}").assertDoesNotExist()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun scrollingByExactlyTheItemSize_switchesTheFirstVisibleItem() {
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 3),
                state = rememberLazyListState().also { state = it },
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag("0")
            .assertIsNotDisplayed()

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun pointerInputScrollingIsAllowedWhenUserScrollingIsEnabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 3).testTag(LazyListTag),
                userScrollEnabled = true,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag).scrollBy(itemSize)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun pointerInputScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 3).testTag(LazyListTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag).scrollBy(itemSize)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun programmaticScrollingIsAllowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag("1")
            .assertStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun semanticScrollingIsDisallowedWhenUserScrollingIsDisabled() {
        val itemSize = with(rule.density) { 30.toDp() }
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemSize * 3).testTag(LazyListTag),
                userScrollEnabled = false,
            ) {
                items(5) {
                    Spacer(Modifier.size(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .assert(keyNotDefined(SemanticsActions.ScrollBy))
            .assert(keyNotDefined(SemanticsActions.ScrollToIndex))
            // but we still have a read only scroll range property
            .assert(
                keyIsDefined(
                    if (vertical) {
                        SemanticsProperties.VerticalScrollAxisRange
                    } else {
                        SemanticsProperties.HorizontalScrollAxisRange
                    }
                )
            )
    }

    @Test
    fun withMissingItems() {
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnOrRow(
                modifier = Modifier.mainAxisSize(itemSize + 1.dp),
                state = state
            ) {
                items(4) {
                    if (it != 1) {
                        Box(Modifier.size(itemSize).testTag(it.toString()))
                    }
                }
            }
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(1)
            }
        }

        rule.onNodeWithTag("0").assertIsNotDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("3").assertIsDisplayed()
    }

    @Test
    fun withZeroSizedFirstItem_shouldNotConsumedDrag() {
        var scrollConsumedAccumulator = Offset.Zero
        val collectingDataConnection = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                scrollConsumedAccumulator += consumed
                return Offset.Zero
            }
        }

        rule.setContent {
            val state = rememberLazyListState()
            LazyColumnOrRow(
                modifier = Modifier
                    .testTag("mainList")
                    .nestedScroll(collectingDataConnection),
                state = state,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(all = 10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.size(size = 0.dp))
                }
                items(10) {
                    Box(Modifier.fillMaxWidth()) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        rule.onNodeWithTag("mainList").performTouchInput {
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(scrollConsumedAccumulator).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun withZeroSizedFirstItem_shouldKeepItemOnSizeChange() {
        val firstItemSize = mutableStateOf(0.dp)

        rule.setContent {
            val state = rememberLazyListState()
            LazyColumnOrRow(
                modifier = Modifier
                    .testTag("mainList"),
                state = state,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(all = 10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier
                        .testTag("firstItem")
                        .size(size = firstItemSize.value)
                        .background(Color.Black))
                }
                items(10) {
                    Box(Modifier.background(Color.Red).fillMaxWidth()) {
                        BasicText(text = it.toString())
                    }
                }
            }
        }

        rule.onNodeWithTag("firstItem").assertIsNotDisplayed()
        firstItemSize.value = 20.dp
        rule.onNodeWithTag("firstItem").assertIsDisplayed()
    }

    @Test
    fun recomposingWithNewComposedModifierObjectIsNotCausingRemeasure() {
        var remeasureCount = 0
        val layoutModifier = Modifier.layout { measurable, constraints ->
            remeasureCount++
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        val counter = mutableStateOf(0)

        rule.setContentWithTestViewConfiguration {
            counter.value // just to trigger recomposition
            LazyColumnOrRow(
                // this will return a new object everytime causing Lazy list recomposition
                // without causing remeasure
                Modifier.composed { layoutModifier }
            ) {
                items(1) {
                    Spacer(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
            counter.value++
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
        }
    }

    @Test
    fun passingNegativeItemsCountIsNotAllowed() {
        var exception: Exception? = null
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow {
                try {
                    items(-1) {
                        Box(Modifier)
                    }
                } catch (e: Exception) {
                    exception = e
                }
            }
        }

        rule.runOnIdle {
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun scrollingALotDoesntCauseLazyLayoutRecomposition() {
        var recomposeCount = 0
        lateinit var state: LazyListState

        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumnOrRow(
                Modifier.composed {
                    recomposeCount++
                    Modifier
                },
                state
            ) {
                items(1000) {
                    Spacer(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(recomposeCount).isEqualTo(1)

            runBlocking {
                state.scrollToItem(100)
            }
        }

        rule.runOnIdle {
            assertThat(recomposeCount).isEqualTo(1)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun zIndexOnItemAffectsDrawingOrder() {
        rule.setContentWithTestViewConfiguration {
            LazyColumnOrRow(
                Modifier.size(6.dp).testTag(LazyListTag)
            ) {
                items(listOf(Color.Blue, Color.Green, Color.Red)) { color ->
                    Box(
                        Modifier
                            .mainAxisSize(2.dp)
                            .crossAxisSize(6.dp)
                            .zIndex(if (color == Color.Green) 1f else 0f)
                            .drawBehind {
                                drawRect(
                                    color,
                                    topLeft = Offset(-10.dp.toPx(), -10.dp.toPx()),
                                    size = Size(20.dp.toPx(), 20.dp.toPx())
                                )
                            })
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .captureToImage()
            .assertPixels { Color.Green }
    }

    @Test
    fun scrollDuringMeasure() {
        rule.setContent {
            BoxWithConstraints {
                val state = rememberLazyListState()
                LazyColumnOrRow(
                    state = state,
                    modifier = Modifier.mainAxisSize(100.dp).fillMaxCrossAxis()
                ) {
                    items(20) {
                        val tag = it.toString()
                        BasicText(
                            text = tag,
                            modifier = Modifier.mainAxisSize(30.dp).fillMaxCrossAxis().testTag(tag)
                        )
                    }
                }
                LaunchedEffect(state) {
                    state.scrollToItem(10)
                }
            }
        }

        rule.onNodeWithTag("10")
            .assertStartPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun scrollInLaunchedEffect() {
        rule.setContent {
            val state = rememberLazyListState()
            LazyColumnOrRow(
                state = state,
                modifier = Modifier.mainAxisSize(100.dp).fillMaxCrossAxis()
            ) {
                items(20) {
                    val tag = it.toString()
                    BasicText(
                        text = tag,
                        modifier = Modifier.mainAxisSize(30.dp).fillMaxCrossAxis().testTag(tag)
                    )
                }
            }
            LaunchedEffect(state) {
                state.scrollToItem(10)
            }
        }

        rule.onNodeWithTag("10")
            .assertStartPositionInRootIsEqualTo(0.dp)
    }

    // ********************* END OF TESTS *********************
    // Helper functions, etc. live below here

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}

internal val NeverEqualObject = object {
    override fun equals(other: Any?): Boolean {
        return false
    }
}

private data class NotStable(val count: Int)

internal const val TestTouchSlop = 18f

internal fun IntegerSubject.isWithin1PixelFrom(expected: Int) {
    isEqualTo(expected, 1)
}

internal fun IntegerSubject.isEqualTo(expected: Int, tolerance: Int) {
    isIn(Range.closed(expected - tolerance, expected + tolerance))
}

internal fun ComposeContentTestRule.setContentWithTestViewConfiguration(
    composable: @Composable () -> Unit
) {
    this.setContent {
        WithTouchSlop(TestTouchSlop, composable)
    }
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performTouchInput {
        with(density) {
            val touchSlop = TestTouchSlop.toInt()
            val xPx = x.roundToPx()
            val yPx = y.roundToPx()
            val offsetX = if (xPx > 0) xPx + touchSlop else if (xPx < 0) xPx - touchSlop else 0
            val offsetY = if (yPx > 0) yPx + touchSlop else if (yPx < 0) yPx - touchSlop else 0
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - offsetX, center.y - offsetY),
                endVelocity = 0f
            )
        }
    }