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

package androidx.compose.foundation.lazy

import androidx.compose.animation.core.ExponentialDecay
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.Text
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.TouchSlop
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.center
import androidx.ui.test.click
import androidx.ui.test.createComposeRule
import androidx.ui.test.getUnclippedBoundsInRoot
import androidx.ui.test.onChildren
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performGesture
import androidx.ui.test.swipeUp
import androidx.ui.test.swipeWithVelocity
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyColumnForTest {
    private val LazyColumnForTag = "TestLazyColumnFor"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun compositionsAreDisposed_whenNodesAreScrolledOff() {
        var composed: Boolean
        var disposed = false
        // Ten 31dp spacers in a 300dp list
        val latch = CountDownLatch(10)
        // Make it long enough that it's _definitely_ taller than the screen
        val data = (1..50).toList()

        rule.setContent {
            // Fixed height to eliminate device size as a factor
            Box(Modifier.testTag(LazyColumnForTag).preferredHeight(300.dp)) {
                LazyColumnFor(items = data, modifier = Modifier.fillMaxSize()) {
                    onCommit {
                        composed = true
                        // Signal when everything is done composing
                        latch.countDown()
                        onDispose {
                            disposed = true
                        }
                    }

                    // There will be 10 of these in the 300dp box
                    Spacer(Modifier.preferredHeight(31.dp))
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

        rule.onNodeWithTag(LazyColumnForTag)
            .performGesture { swipeUp() }

        rule.waitForIdle()

        assertWithMessage("No additional items were composed after scroll, scroll didn't work")
            .that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "No compositions were disposed after scrolling, compositions were leaked"
        ).that(disposed).isTrue()
    }

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() {
        var composed = 0
        var disposals = 0
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        rule.setContent {
            LazyColumnFor(
                items = if (!part2) data1 else data2,
                modifier = Modifier.testTag(LazyColumnForTag).fillMaxSize()
            ) {
                onCommit {
                    composed++
                    onDispose {
                        disposals++
                    }
                }

                Spacer(Modifier.height(50.dp))
            }
        }

        rule.runOnIdle {
            assertWithMessage("Not all items were composed")
                .that(composed).isEqualTo(data1.size)
            composed = 0

            part2 = true
        }

        rule.runOnIdle {
            assertWithMessage(
                "No additional items were composed after data change, something didn't work"
            ).that(composed).isEqualTo(data2.size)

            // We may need to modify this test once we prefetch/cache items outside the viewport
            assertWithMessage(
                "Not enough compositions were disposed after scrolling, compositions were leaked"
            ).that(disposals).isEqualTo(data1.size)
        }
    }

    @Test
    fun compositionsAreDisposed_whenAdapterListIsDisposed() {
        var emitAdapterList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        rule.setContent {
            if (emitAdapterList) {
                LazyColumnFor(
                    items = listOf(0, 1),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.size(100.dp))
                    onDispose {
                        if (it == 1) {
                            disposeCalledOnFirstItem = true
                        } else {
                            disposeCalledOnSecondItem = true
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertWithMessage("First item is not immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            assertWithMessage("Second item is not immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            emitAdapterList = false
        }

        rule.runOnIdle {
            assertWithMessage("First item is correctly disposed")
                .that(disposeCalledOnFirstItem).isTrue()
            assertWithMessage("Second item is correctly disposed")
                .that(disposeCalledOnSecondItem).isTrue()
        }
    }

    @Test
    fun removeItemsTest() {
        val startingNumItems = 3
        var numItems = startingNumItems
        var numItemsModel by mutableStateOf(numItems)
        val tag = "List"
        rule.setContent {
            LazyColumnFor((1..numItemsModel).toList(), modifier = Modifier.testTag(tag)) {
                Text("$it")
            }
        }

        while (numItems >= 0) {
            // Confirm the number of children to ensure there are no extra items
            rule.onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (i in 1..3) {
                rule.onNodeWithText("$i").apply {
                    if (i <= numItems) {
                        assertExists()
                    } else {
                        assertDoesNotExist()
                    }
                }
            }
            numItems--
            if (numItems >= 0) {
                // Don't set the model to -1
                rule.runOnIdle { numItemsModel = numItems }
            }
        }
    }

    @Test
    fun changingDataTest() {
        val dataLists = listOf(
            (1..3).toList(),
            (4..8).toList(),
            (3..4).toList()
        )
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        rule.setContent {
            LazyColumnFor(dataModel, modifier = Modifier.testTag(tag)) {
                Text("$it")
            }
        }

        for (data in dataLists) {
            rule.runOnIdle { dataModel = data }

            // Confirm the number of children to ensure there are no extra items
            val numItems = data.size
            rule.onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (item in data) {
                rule.onNodeWithText("$item").assertExists()
            }
        }
    }

    @Test
    fun whenItemsAreInitiallyCreatedWith0SizeWeCanScrollWhenTheyExpanded() {
        val thirdTag = "third"
        val items = (1..3).toList()
        var thirdHasSize by mutableStateOf(false)

        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.fillMaxWidth()
                    .preferredHeight(100.dp)
                    .testTag(LazyColumnForTag)
            ) {
                if (it == 3) {
                    Spacer(
                        Modifier.testTag(thirdTag)
                            .fillParentMaxWidth()
                            .preferredHeight(if (thirdHasSize) 60.dp else 0.dp)
                    )
                } else {
                    Spacer(Modifier.fillParentMaxWidth().preferredHeight(60.dp))
                }
            }
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 21.dp, density = rule.density)

        rule.onNodeWithTag(thirdTag)
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            thirdHasSize = true
        }

        rule.waitForIdle()

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 10.dp, density = rule.density)

        rule.onNodeWithTag(thirdTag)
            .assertIsDisplayed()
    }

    @Ignore("This test is not fully working. To be fixed in b/167913500")
    @Test
    fun contentPaddingIsApplied() = with(rule.density) {
        val itemTag = "item"

        rule.setContent {
            LazyColumnFor(
                items = listOf(1),
                modifier = Modifier.size(100.dp)
                    .testTag(LazyColumnForTag),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 50.dp,
                    end = 10.dp,
                    bottom = 50.dp
                )
            ) {
                Spacer(Modifier.fillParentMaxWidth().preferredHeight(50.dp).testTag(itemTag))
            }
        }

        var itemBounds = rule.onNodeWithTag(itemTag)
            .getUnclippedBoundsInRoot()

        assertThat(itemBounds.top.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
        assertThat(itemBounds.bottom.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        assertThat(itemBounds.left.toIntPx()).isWithin1PixelFrom(10.dp.toIntPx())
        assertThat(itemBounds.right.toIntPx())
            .isWithin1PixelFrom(100.dp.toIntPx() - 10.dp.toIntPx())

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 51.dp, density = rule.density)

        itemBounds = rule.onNodeWithTag(itemTag)
            .getUnclippedBoundsInRoot()

        assertThat(itemBounds.top.toIntPx()).isWithin1PixelFrom(0)
        assertThat(itemBounds.bottom.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
    }

    @Test
    fun lazyColumnWrapsContent() = with(rule.density) {
        val itemInsideLazyColumn = "itemInsideLazyColumn"
        val itemOutsideLazyColumn = "itemOutsideLazyColumn"
        var sameSizeItems by mutableStateOf(true)

        rule.setContent {
            Row {
                LazyColumnFor(
                    items = listOf(1, 2),
                    modifier = Modifier.testTag(LazyColumnForTag)
                ) {
                    if (it == 1) {
                        Spacer(Modifier.preferredSize(50.dp).testTag(itemInsideLazyColumn))
                    } else {
                        Spacer(Modifier.preferredSize(if (sameSizeItems) 50.dp else 70.dp))
                    }
                }
                Spacer(Modifier.preferredSize(50.dp).testTag(itemOutsideLazyColumn))
            }
        }

        rule.onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        var lazyColumnBounds = rule.onNodeWithTag(LazyColumnForTag)
            .getUnclippedBoundsInRoot()

        assertThat(lazyColumnBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        assertThat(lazyColumnBounds.right.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
        assertThat(lazyColumnBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        assertThat(lazyColumnBounds.bottom.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())

        rule.runOnIdle {
            sameSizeItems = false
        }

        rule.waitForIdle()

        rule.onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        lazyColumnBounds = rule.onNodeWithTag(LazyColumnForTag)
            .getUnclippedBoundsInRoot()

        assertThat(lazyColumnBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        assertThat(lazyColumnBounds.right.toIntPx()).isWithin1PixelFrom(70.dp.toIntPx())
        assertThat(lazyColumnBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        assertThat(lazyColumnBounds.bottom.toIntPx()).isWithin1PixelFrom(120.dp.toIntPx())
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyColumnsItemsAlignment(horizontalGravity: Alignment.Horizontal) {
        rule.setContent {
            LazyColumnFor(
                items = listOf(1, 2),
                modifier = Modifier.testTag(LazyColumnForTag).width(100.dp),
                horizontalAlignment = horizontalGravity
            ) {
                if (it == 1) {
                    Spacer(Modifier.preferredSize(50.dp).testTag(firstItemTag))
                } else {
                    Spacer(Modifier.preferredSize(70.dp).testTag(secondItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        rule.onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyColumnBounds = rule.onNodeWithTag(LazyColumnForTag)
            .getUnclippedBoundsInRoot()

        with(rule.density) {
            // Verify the width of the column
            assertThat(lazyColumnBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
            assertThat(lazyColumnBounds.right.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        }
    }

    @Test
    fun lazyColumnAlignmentCenterHorizontally() {
        prepareLazyColumnsItemsAlignment(Alignment.CenterHorizontally)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(25.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(15.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentStart() {
        prepareLazyColumnsItemsAlignment(Alignment.Start)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentEnd() {
        prepareLazyColumnsItemsAlignment(Alignment.End)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(30.dp, 50.dp)
    }

    @Test
    fun itemFillingParentWidth() {
        rule.setContent {
            LazyColumnFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.fillParentMaxWidth().height(50.dp).testTag(firstItemTag))
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        rule.setContent {
            LazyColumnFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.width(50.dp).fillParentMaxHeight().testTag(firstItemTag))
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        rule.setContent {
            LazyColumnFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        var parentSize by mutableStateOf(100.dp)
        rule.setContent {
            LazyColumnFor(
                items = listOf(0),
                modifier = Modifier.size(parentSize)
            ) {
                Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
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
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        // after scroll we will display items 16-20
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 300.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..10).toList()
        }

        // there is no item 16 anymore so we will just display the last items 6-10
        rule.onNodeWithTag("6")
            .assertTopPositionIsAlmost(0.dp)
    }

    @Test
    fun whenFewDisplayedItemsWereRemoved() {
        var items by mutableStateOf((1..10).toList())
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 100.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..8).toList()
        }

        // there are no more items 9 and 10, so we have to scroll back
        rule.onNodeWithTag("4")
            .assertTopPositionIsAlmost(0.dp)
    }

    @Test
    fun whenItemsBecameEmpty() {
        var items by mutableStateOf((1..10).toList())
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.sizeIn(maxHeight = 100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        // after scroll we will display items 2-6
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 20.dp, density = rule.density)

        rule.runOnIdle {
            items = emptyList()
        }

        // there are no more items so the LazyColumn is zero sized
        rule.onNodeWithTag(LazyColumnForTag)
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
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 100.dp, density = rule.density)

        // and scroll back
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = (-100).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionIsAlmost(0.dp)
    }

    @Test
    fun tryToScrollBackwardWhenAlreadyOnTop() {
        val items by mutableStateOf((1..20).toList())
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        // we already displaying the first item, so this should do nothing
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = (-50).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionIsAlmost(0.dp)
        rule.onNodeWithTag("5")
            .assertTopPositionIsAlmost(80.dp)
    }

    @Test
    fun contentOfNotStableItemsIsNotRecomposedDuringScroll() {
        val items = listOf(NotStable(1), NotStable(2))
        var firstItemRecomposed = 0
        var secondItemRecomposed = 0
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                if (it.count == 1) {
                    firstItemRecomposed++
                } else {
                    secondItemRecomposed++
                }
                Spacer(Modifier.size(75.dp))
            }
        }

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = (50).dp, density = rule.density)

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }
    }

    @Test
    fun onlyOneMeasurePassForScrollEvent() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        val initialMeasurePasses = state.numMeasurePasses

        rule.runOnIdle {
            with(rule.density) {
                state.onScroll(110.dp.toPx())
            }
        }

        rule.waitForIdle()

        assertThat(state.numMeasurePasses).isEqualTo(initialMeasurePasses + 1)
    }

    @Test
    fun stateUpdatedAfterScroll() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 30.dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)

            with(rule.density) {
                // TODO(b/169232491): test scrolling doesn't appear to be scrolling exactly the right
                //  number of pixels
                val expectedOffset = 10.dp.toIntPx()
                val tolerance = 2.dp.toIntPx()
                assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun isAnimationRunningUpdate() {
        val items by mutableStateOf((1..20).toList())
        val clock = ManualAnimationClock(0L)
        val state = LazyListState(
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.isAnimationRunning).isEqualTo(false)
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .performGesture { swipeUp() }

        rule.runOnIdle {
            clock.clockTimeMillis += 100
            assertThat(state.firstVisibleItemIndex).isNotEqualTo(0)
            assertThat(state.isAnimationRunning).isEqualTo(true)
        }

        // TODO (jelle): this should be down, and not click to be 100% fair
        rule.onNodeWithTag(LazyColumnForTag)
            .performGesture { click() }

        rule.runOnIdle {
            assertThat(state.isAnimationRunning).isEqualTo(false)
        }
    }

    @Test
    fun stateUpdatedAfterScrollWithinTheSameItem() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 10.dp, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) {
                val expectedOffset = 10.dp.toIntPx()
                val tolerance = 2.dp.toIntPx()
                assertThat(state.firstVisibleItemScrollOffset)
                    .isEqualTo(expectedOffset, tolerance)
            }
        }
    }

    @Test
    fun initialScrollIsApplied() {
        val items by mutableStateOf((0..20).toList())
        lateinit var state: LazyListState
        val expectedOffset = with(rule.density) { 10.dp.toIntPx() }
        rule.setContent {
            state = rememberLazyListState(2, expectedOffset)
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(2)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
        }

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun stateIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        val items by mutableStateOf((1..20).toList())
        var state: LazyListState? = null
        restorationTester.setContent {
            state = rememberLazyListState()
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag),
                state = state!!
            ) {
                Spacer(Modifier.size(20.dp).testTag("$it"))
            }
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 30.dp, density = rule.density)

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
    fun scroll_makeListSmaller_scroll() {
        var items by mutableStateOf((1..100).toList())
        rule.setContent {
            LazyColumnFor(
                items = items,
                modifier = Modifier.size(100.dp).testTag(LazyColumnForTag)
            ) {
                Spacer(Modifier.size(10.dp).testTag("$it"))
            }
        }

        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = 300.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..11).toList()
        }

        // try to scroll after the data set has been updated. this was causing a crash previously
        rule.onNodeWithTag(LazyColumnForTag)
            .scrollBy(y = (-10).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    private fun SemanticsNodeInteraction.assertTopPositionIsAlmost(expected: Dp) {
        getUnclippedBoundsInRoot().top.assertIsEqualTo(expected, tolerance = 1.dp)
    }
}

data class NotStable(val count: Int)

internal fun IntegerSubject.isWithin1PixelFrom(expected: Int) {
    isEqualTo(expected, 1)
}

internal fun IntegerSubject.isEqualTo(expected: Int, tolerance: Int) {
    isIn(Range.closed(expected - tolerance, expected + tolerance))
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performGesture {
        with(density) {
            val touchSlop = TouchSlop.toIntPx()
            val xPx = x.toIntPx()
            val yPx = y.toIntPx()
            val offsetX = if (xPx > 0) xPx + touchSlop else if (xPx < 0) xPx - touchSlop else 0
            val offsetY = if (yPx > 0) yPx + touchSlop else if (yPx < 0) yPx - touchSlop else 0
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - offsetX, center.y - offsetY),
                endVelocity = 0f
            )
        }
    }
