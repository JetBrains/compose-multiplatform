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

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.animation.core.snap
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.smoothScrollBy
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.TouchSlop
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyColumnTest {
    private val LazyListTag = "LazyListTag"

    private val NeverEqualObject = object {
        override fun equals(other: Any?): Boolean {
            return false
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyColumnShowsCombinedItems() {
        val itemTestTag = "itemTestTag"
        val items = listOf(1, 2).map { it.toString() }
        val indexedItems = listOf(3, 4, 5)

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                item {
                    Spacer(
                        Modifier.preferredHeight(40.dp).fillParentMaxWidth().testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.preferredHeight(40.dp).fillParentMaxWidth().testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(
                        Modifier.preferredHeight(41.dp).fillParentMaxWidth()
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
    fun lazyColumnAllowEmptyListItems() {
        val itemTag = "itemTag"

        rule.setContent {
            LazyColumn {
                items(emptyList<Any>()) { }
                item {
                    Spacer(Modifier.preferredSize(10.dp).testTag(itemTag))
                }
            }
        }

        rule.onNodeWithTag(itemTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnAllowsNullableItems() {
        val items = listOf("1", null, "3")
        val nullTestTag = "nullTestTag"

        rule.setContent {
            LazyColumn(Modifier.preferredHeight(200.dp)) {
                items(items) {
                    if (it != null) {
                        Spacer(Modifier.preferredHeight(101.dp).fillParentMaxWidth().testTag(it))
                    } else {
                        Spacer(
                            Modifier.preferredHeight(101.dp).fillParentMaxWidth()
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
    fun compositionsAreDisposed_whenNodesAreScrolledOff() {
        var composed: Boolean
        var disposed = false
        // Ten 31dp spacers in a 300dp list
        val latch = CountDownLatch(10)

        rule.setContent {
            // Fixed height to eliminate device size as a factor
            Box(Modifier.testTag(LazyListTag).preferredHeight(300.dp)) {
                LazyColumn(Modifier.fillMaxSize()) {
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
                        Spacer(Modifier.preferredHeight(31.dp))
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
            LazyColumn(Modifier.testTag(LazyListTag).fillMaxSize()) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose {
                            disposals++
                        }
                    }

                    Spacer(Modifier.height(50.dp))
                }
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
                LazyColumn(Modifier.fillMaxSize()) {
                    items(2) {
                        Box(Modifier.size(100.dp))
                        DisposableEffect(Unit) {
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
            LazyColumn(Modifier.testTag(tag)) {
                items((1..numItemsModel).toList()) {
                    BasicText("$it")
                }
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
            LazyColumn(Modifier.testTag(tag)) {
                items(dataModel) {
                    BasicText("$it")
                }
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
            LazyColumn(
                Modifier.fillMaxWidth()
                    .preferredHeight(100.dp)
                    .testTag(LazyListTag)
            ) {
                items(items) {
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
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 21.dp, density = rule.density)

        rule.onNodeWithTag(thirdTag)
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            thirdHasSize = true
        }

        rule.waitForIdle()

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 10.dp, density = rule.density)

        rule.onNodeWithTag(thirdTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumnWrapsContent() = with(rule.density) {
        val itemInsideLazyColumn = "itemInsideLazyColumn"
        val itemOutsideLazyColumn = "itemOutsideLazyColumn"
        var sameSizeItems by mutableStateOf(true)

        rule.setContent {
            Row {
                LazyColumn(Modifier.testTag(LazyListTag)) {
                    items(listOf(1, 2)) {
                        if (it == 1) {
                            Spacer(Modifier.preferredSize(50.dp).testTag(itemInsideLazyColumn))
                        } else {
                            Spacer(Modifier.preferredSize(if (sameSizeItems) 50.dp else 70.dp))
                        }
                    }
                }
                Spacer(Modifier.preferredSize(50.dp).testTag(itemOutsideLazyColumn))
            }
        }

        rule.onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        var lazyColumnBounds = rule.onNodeWithTag(LazyListTag)
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

        lazyColumnBounds = rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(
                Modifier.testTag(LazyListTag).width(100.dp),
                horizontalAlignment = horizontalGravity
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Spacer(Modifier.preferredSize(50.dp).testTag(firstItemTag))
                    } else {
                        Spacer(Modifier.preferredSize(70.dp).testTag(secondItemTag))
                    }
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        rule.onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyColumnBounds = rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxWidth().height(50.dp).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        rule.setContent {
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.width(50.dp).fillParentMaxHeight().testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        rule.setContent {
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
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
        rule.setContent {
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxWidth(0.6f).height(50.dp).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeightFraction() {
        rule.setContent {
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.width(50.dp).fillParentMaxHeight(0.2f).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun itemFillingParentSizeFraction() {
        rule.setContent {
            LazyColumn(Modifier.size(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(Modifier.fillParentMaxSize(0.1f).testTag(firstItemTag))
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(15.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        var parentSize by mutableStateOf(100.dp)
        rule.setContent {
            LazyColumn(Modifier.size(parentSize)) {
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
        rule.setContent {
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 16-20
        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(Modifier.sizeIn(maxHeight = 100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 2-6
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 20.dp, density = rule.density)

        rule.runOnIdle {
            items = emptyList()
        }

        // there are no more items so the LazyColumn is zero sized
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
        rule.setContent {
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        // after scroll we will display items 6-10
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 100.dp, density = rule.density)

        // and scroll back
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = (-100).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertTopPositionIsAlmost(0.dp)
    }

    @Test
    fun tryToScrollBackwardWhenAlreadyOnTop() {
        val items by mutableStateOf((1..20).toList())
        rule.setContent {
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        // we already displaying the first item, so this should do nothing
        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    if (it.count == 1) {
                        firstItemRecomposed++
                    } else {
                        secondItemRecomposed++
                    }
                    Spacer(Modifier.size(75.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstItemRecomposed).isEqualTo(1)
            assertThat(secondItemRecomposed).isEqualTo(1)
        }

        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
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
    fun stateUpdatedAfterScroll() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        rule.onNodeWithTag(LazyListTag)
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
            flingConfig = FlingConfig(FloatExponentialDecaySpec()),
            animationClock = clock
        )
        rule.setContent {
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.isAnimationRunning).isEqualTo(false)
        }

        rule.onNodeWithTag(LazyListTag)
            .performGesture { swipeUp() }

        rule.runOnIdle {
            clock.clockTimeMillis += 100
            assertThat(state.firstVisibleItemIndex).isNotEqualTo(0)
            assertThat(state.isAnimationRunning).isEqualTo(true)
        }

        rule.onNodeWithTag(LazyListTag)
            .performGesture { down(center) }

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
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
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
        var state: LazyListState? = null
        restorationTester.setContent {
            state = rememberLazyListState()
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state!!
            ) {
                items(20) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
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
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.size(10.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 300.dp, density = rule.density)

        rule.runOnIdle {
            items = (1..11).toList()
        }

        // try to scroll after the data set has been updated. this was causing a crash previously
        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = (-10).dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    @Test
    fun snapToItemIndex() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                Modifier.size(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(20) {
                    Spacer(Modifier.size(20.dp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.snapToItemIndex(3, 10)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(3)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
        }
    }

    @Test
    fun itemsAreNotRedrawnDuringScroll() {
        val items = (0..20).toList()
        val redrawCount = Array(6) { 0 }
        rule.setContent {
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(
                        Modifier.size(20.dp)
                            .drawBehind { redrawCount[it]++ }
                    )
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 10.dp, density = rule.density)

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
        rule.setContent {
            LazyColumn(Modifier.size(100.dp).testTag(LazyListTag)) {
                items(2) {
                    Spacer(
                        Modifier.size(50.dp)
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
        rule.setContent {
            LazyColumn(
                Modifier.height(itemSizeMinusOne).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(2) {
                    Spacer(
                        if (it == 0) {
                            Modifier.width(30.dp).height(itemSizeMinusOne)
                        } else {
                            Modifier.width(20.dp).height(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertWidthIsEqualTo(20.dp)
    }

    @Test
    fun itemStillVisibleAfterOverscrollIsAffectingCrossAxisSize() {
        val items = (0..2).toList()
        val itemSize = with(rule.density) { 30.toDp() }
        lateinit var state: LazyListState
        rule.setContent {
            LazyColumn(
                Modifier.height(itemSize * 1.75f).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(items) {
                    Spacer(
                        if (it == 0) {
                            Modifier.width(30.dp).height(itemSize / 2)
                        } else if (it == 1) {
                            Modifier.width(20.dp).height(itemSize / 2)
                        } else {
                            Modifier.width(20.dp).height(itemSize)
                        }
                    )
                }
            }
        }

        state.scrollBy(itemSize)

        rule.onNodeWithTag(LazyListTag)
            .assertWidthIsEqualTo(30.dp)
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyColumn {
                items(items) {
                    Spacer(Modifier.size(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.size(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    private fun SemanticsNodeInteraction.assertTopPositionIsAlmost(expected: Dp) {
        getUnclippedBoundsInRoot().top.assertIsEqualTo(expected, tolerance = 1.dp)
    }

    private fun LazyListState.scrollBy(offset: Dp) {
        runBlocking {
            smoothScrollBy(with(rule.density) { offset.toIntPx().toFloat() }, snap())
        }
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
