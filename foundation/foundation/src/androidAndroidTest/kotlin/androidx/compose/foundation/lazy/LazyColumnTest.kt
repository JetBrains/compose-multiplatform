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

import android.os.Build
import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.height(200.dp)) {
                item {
                    Spacer(
                        Modifier.height(40.dp).fillParentMaxWidth().testTag(itemTestTag)
                    )
                }
                items(items) {
                    Spacer(Modifier.height(40.dp).fillParentMaxWidth().testTag(it))
                }
                itemsIndexed(indexedItems) { index, item ->
                    Spacer(
                        Modifier.height(41.dp).fillParentMaxWidth()
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn {
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
    fun lazyColumnAllowsNullableItems() {
        val items = listOf("1", null, "3")
        val nullTestTag = "nullTestTag"

        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.height(200.dp)) {
                items(items) {
                    if (it != null) {
                        Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                    } else {
                        Spacer(
                            Modifier.height(101.dp).fillParentMaxWidth()
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

        rule.setContentWithTestViewConfiguration {
            // Fixed height to eliminate device size as a factor
            Box(Modifier.testTag(LazyListTag).height(300.dp)) {
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
                        Spacer(Modifier.height(31.dp))
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.testTag(LazyListTag).fillMaxSize()) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose {
                            disposals++
                        }
                    }

                    Spacer(Modifier.requiredHeight(50.dp))
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

        rule.setContentWithTestViewConfiguration {
            if (emitAdapterList) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(2) {
                        Box(Modifier.requiredSize(100.dp))
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
        rule.setContentWithTestViewConfiguration {
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
        rule.setContentWithTestViewConfiguration {
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .testTag(LazyListTag)
            ) {
                items(items) {
                    if (it == 3) {
                        Spacer(
                            Modifier.testTag(thirdTag)
                                .fillParentMaxWidth()
                                .height(if (thirdHasSize) 60.dp else 0.dp)
                        )
                    } else {
                        Spacer(Modifier.fillParentMaxWidth().height(60.dp))
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

        rule.setContentWithTestViewConfiguration {
            Row {
                LazyColumn(Modifier.testTag(LazyListTag)) {
                    items(listOf(1, 2)) {
                        if (it == 1) {
                            Spacer(Modifier.size(50.dp).testTag(itemInsideLazyColumn))
                        } else {
                            Spacer(Modifier.size(if (sameSizeItems) 50.dp else 70.dp))
                        }
                    }
                }
                Spacer(Modifier.size(50.dp).testTag(itemOutsideLazyColumn))
            }
        }

        rule.onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        rule.onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        var lazyColumnBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        assertThat(lazyColumnBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyColumnBounds.right.roundToPx()).isWithin1PixelFrom(50.dp.roundToPx())
        assertThat(lazyColumnBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyColumnBounds.bottom.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())

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

        assertThat(lazyColumnBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyColumnBounds.right.roundToPx()).isWithin1PixelFrom(70.dp.roundToPx())
        assertThat(lazyColumnBounds.top.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
        assertThat(lazyColumnBounds.bottom.roundToPx()).isWithin1PixelFrom(120.dp.roundToPx())
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyColumnsItemsAlignment(horizontalGravity: Alignment.Horizontal) {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                Modifier.testTag(LazyListTag).requiredWidth(100.dp),
                horizontalAlignment = horizontalGravity
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Spacer(Modifier.size(50.dp).testTag(firstItemTag))
                    } else {
                        Spacer(Modifier.size(70.dp).testTag(secondItemTag))
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
            assertThat(lazyColumnBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyColumnBounds.right.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
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
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
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
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
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
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.fillParentMaxWidth(0.6f)
                            .requiredHeight(50.dp)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeightFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
                items(listOf(0)) {
                    Spacer(
                        Modifier.requiredWidth(50.dp)
                            .fillParentMaxHeight(0.2f)
                            .testTag(firstItemTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun itemFillingParentSizeFraction() {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(width = 100.dp, height = 150.dp)) {
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(parentSize)) {
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
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSizeIn(maxHeight = 100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
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
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            state.prefetchingEnabled = false
            LazyColumn(
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
    fun stateUpdatedAfterScroll() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumn(
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
            .scrollBy(y = 30.dp, density = rule.density)

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
    fun flingAnimationStopsOnFingerDown() {
        val items by mutableStateOf((1..20).toList())
        val state = LazyListState()
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
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

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(LazyListTag)
            .performGesture { swipeUp() }
        rule.mainClock.advanceTimeBy(100)

        val itemIndexWhenInterrupting = state.firstVisibleItemIndex
        val itemOffsetWhenInterrupting = state.firstVisibleItemScrollOffset

        assertThat(itemIndexWhenInterrupting).isNotEqualTo(0)
        assertThat(itemOffsetWhenInterrupting).isNotEqualTo(0)

        rule.onNodeWithTag(LazyListTag)
            .performGesture { down(center) }
        rule.mainClock.advanceTimeBy(100)

        assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndexWhenInterrupting)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(itemOffsetWhenInterrupting)
    }

    @Test
    fun stateUpdatedAfterScrollWithinTheSameItem() {
        val items by mutableStateOf((1..20).toList())
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumn(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state
            ) {
                items(items) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .scrollBy(y = 10.dp, density = rule.density)

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
    fun initialScrollIsApplied() {
        val items by mutableStateOf((0..20).toList())
        lateinit var state: LazyListState
        val expectedOffset = with(rule.density) { 10.dp.roundToPx() }
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState(2, expectedOffset)
            LazyColumn(
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
            .assertTopPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun stateIsRestored() {
        val restorationTester = StateRestorationTester(rule)
        var state: LazyListState? = null
        restorationTester.setContent {
            state = rememberLazyListState()
            LazyColumn(
                Modifier.requiredSize(100.dp).testTag(LazyListTag),
                state = state!!
            ) {
                items(20) {
                    Spacer(Modifier.requiredSize(20.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(Modifier.requiredSize(10.dp).testTag("$it"))
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
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumn(
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
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
                items(items) {
                    Spacer(
                        Modifier.requiredSize(20.dp)
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(Modifier.requiredSize(100.dp).testTag(LazyListTag)) {
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
            LazyColumn(
                Modifier.requiredHeight(itemSizeMinusOne).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(2) {
                    Spacer(
                        if (it == 0) {
                            Modifier.requiredWidth(30.dp).requiredHeight(itemSizeMinusOne)
                        } else {
                            Modifier.requiredWidth(20.dp).requiredHeight(itemSize)
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
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                Modifier.requiredHeight(itemSize * 1.75f).testTag(LazyListTag),
                state = rememberLazyListState().also { state = it }
            ) {
                items(items) {
                    Spacer(
                        if (it == 0) {
                            Modifier.requiredWidth(30.dp).requiredHeight(itemSize / 2)
                        } else if (it == 1) {
                            Modifier.requiredWidth(20.dp).requiredHeight(itemSize / 2)
                        } else {
                            Modifier.requiredWidth(20.dp).requiredHeight(itemSize)
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn {
                items(items) {
                    Spacer(Modifier.requiredSize(itemSize).testTag(it))
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

        rule.setContentWithTestViewConfiguration {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.requiredSize(itemSize).testTag("$index*$item"))
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

    @Test
    fun removalWithMutableStateListOf() {
        val items = mutableStateListOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            LazyColumn {
                items(items) { item ->
                    Spacer(Modifier.size(itemSize).testTag(item))
                }
            }
        }

        rule.runOnIdle {
            items.removeLast()
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun recompositionOrder() {
        val outerState = mutableStateOf(0)
        val innerState = mutableStateOf(0)
        val recompositions = mutableListOf<Pair<Int, Int>>()

        rule.setContent {
            val localOuterState = outerState.value
            LazyColumn {
                items(count = 1) {
                    recompositions.add(localOuterState to innerState.value)
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.runOnIdle {
            innerState.value++
            outerState.value++
        }

        rule.runOnIdle {
            assertThat(recompositions).isEqualTo(
                listOf(0 to 0, 1 to 1)
            )
        }
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: LazyListState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberLazyListState()
            LazyColumn(Modifier.fillMaxWidth().height(10.dp), state) {
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scrolledAwayItemIsNotDisplayedAnymore() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberLazyListState()
            LazyColumn(
                Modifier
                    .requiredSize(10.dp)
                    .testTag(LazyListTag)
                    .graphicsLayer()
                    .background(Color.Blue),
                state = state
            ) {
                items(2) {
                    val size = if (it == 0) 5.dp else 100.dp
                    val color = if (it == 0) Color.Red else Color.Transparent
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(size)
                            .background(color)
                            .testTag("$it")
                    )
                }
            }
        }

        rule.runOnIdle {
            with(rule.density) {
                runBlocking {
                    // we scroll enough to make the Red item not visible anymore
                    state.scrollBy(6.dp.toPx())
                }
            }
        }

        // and verify there is no Red item displayed
        rule.onNodeWithTag(LazyListTag)
            .captureToImage()
            .assertPixels {
                Color.Blue
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
                LazyColumn(
                    Modifier
                        .testTag(LazyListTag)
                        .background(Color.Blue),
                    state = rememberLazyListState(2, 5)
                ) {
                    items(100) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(itemSizeDp)
                                .testTag("$it")
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyListTag)
            .performGesture {
                // we do move manually and not with swipe() utility because we want to have one
                // drag gesture, not multiple smaller ones
                down(center)
                moveBy(Offset(0f, -TestTouchSlop))
                moveBy(
                    Offset(
                        0f,
                        itemSizePx * 15f // large value which makes us overscroll
                    )
                )
                up()
            }

        rule.onNodeWithTag(LazyListTag)
            .assertHeightIsEqualTo(containerSize)

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(containerSize - itemSizeDp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun lazyColumnDoesNotClipHorizontalOverdraw() {
        rule.setContent {
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                LazyColumn(
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

        rule.onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 0.dp,
                verticalPadding = 20.dp
            )
    }

    @Test
    fun initialScrollPositionIsCorrectWhenItemsAreLoadedAsynchronously() {
        lateinit var state: LazyListState
        var itemsCount by mutableStateOf(0)
        rule.setContent {
            state = rememberLazyListState(2, 10)
            LazyColumn(Modifier.fillMaxSize(), state) {
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
            LazyColumn(Modifier.fillMaxSize(), state) {
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
            LazyColumn(Modifier.fillMaxSize(), listState) {
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
            LazyColumn(Modifier.width(150.dp).height(100.dp), state) {
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
            LazyColumn {
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
            LazyColumn(Modifier.testTag(LazyListTag), state = state) {
                items(50) {
                    Box(Modifier.height(200.dp))
                }
            }
        }

        rule.waitForIdle()
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)

        rule.onNodeWithTag(LazyListTag).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 100f)
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

    private fun SemanticsNodeInteraction.assertTopPositionIsAlmost(expected: Dp) {
        getUnclippedBoundsInRoot().top.assertIsEqualTo(expected, tolerance = 1.dp)
    }

    private fun LazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
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

internal const val TestTouchSlop = 18f

internal fun ComposeContentTestRule.setContentWithTestViewConfiguration(
    composable: @Composable () -> Unit
) {
    this.setContent {
        WithTouchSlop(TestTouchSlop, composable)
    }
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performGesture {
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

internal fun Modifier.drawOutsideOfBounds() = drawBehind {
    val inflate = 20.dp.roundToPx().toFloat()
    drawRect(
        Color.Red,
        Offset(-inflate, -inflate),
        Size(size.width + inflate * 2, size.height + inflate * 2)
    )
}