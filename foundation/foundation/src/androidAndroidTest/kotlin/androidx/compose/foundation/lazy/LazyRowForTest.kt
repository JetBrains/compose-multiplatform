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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.filters.MediumTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Providers
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.getUnclippedBoundsInRoot
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.waitForIdle
import androidx.compose.ui.unit.dp
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class LazyRowForTest {
    private val LazyRowForTag = "LazyRowForTag"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lazyRowOnlyVisibleItemsAdded() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowFor(items) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertDoesNotExist()

        onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollToShowItems123() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowFor(items, Modifier.testTag(LazyRowForTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyRowForTag)
            .scrollBy(x = 50.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertIsDisplayed()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()

        onNodeWithTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollToHideFirstItem() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowFor(items, Modifier.testTag(LazyRowForTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyRowForTag)
            .scrollBy(x = 102.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertDoesNotExist()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowScrollToShowItems234() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowFor(items, Modifier.testTag(LazyRowForTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        onNodeWithTag(LazyRowForTag)
            .scrollBy(x = 150.dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertDoesNotExist()

        onNodeWithTag("2")
            .assertIsDisplayed()

        onNodeWithTag("3")
            .assertIsDisplayed()

        onNodeWithTag("4")
            .assertIsDisplayed()
    }

    @Test
    fun contentPaddingIsApplied() = with(composeTestRule.density) {
        val itemTag = "item"

        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(1),
                modifier = Modifier.size(100.dp)
                    .testTag(LazyRowForTag),
                contentPadding = InnerPadding(
                    start = 50.dp,
                    top = 10.dp,
                    end = 50.dp,
                    bottom = 10.dp
                )
            ) {
                Spacer(Modifier.fillParentMaxHeight().preferredWidth(50.dp).testTag(itemTag))
            }
        }

        var itemBounds = onNodeWithTag(itemTag)
            .getUnclippedBoundsInRoot()

        Truth.assertThat(itemBounds.left.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
        Truth.assertThat(itemBounds.right.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        Truth.assertThat(itemBounds.top.toIntPx()).isWithin1PixelFrom(10.dp.toIntPx())
        Truth.assertThat(itemBounds.bottom.toIntPx())
            .isWithin1PixelFrom(100.dp.toIntPx() - 10.dp.toIntPx())

        onNodeWithTag(LazyRowForTag)
            .scrollBy(x = 51.dp, density = composeTestRule.density)

        itemBounds = onNodeWithTag(itemTag)
            .getUnclippedBoundsInRoot()

        Truth.assertThat(itemBounds.left.toIntPx()).isWithin1PixelFrom(0)
        Truth.assertThat(itemBounds.right.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
    }

    @Test
    fun lazyRowWrapsContent() = with(composeTestRule.density) {
        val itemInsideLazyRow = "itemInsideLazyRow"
        val itemOutsideLazyRow = "itemOutsideLazyRow"
        var sameSizeItems by mutableStateOf(true)

        composeTestRule.setContent {
            Column {
                LazyRowFor(
                    items = listOf(1, 2),
                    modifier = Modifier.testTag(LazyRowForTag)
                ) {
                    if (it == 1) {
                        Spacer(Modifier.preferredSize(50.dp).testTag(itemInsideLazyRow))
                    } else {
                        Spacer(Modifier.preferredSize(if (sameSizeItems) 50.dp else 70.dp))
                    }
                }
                Spacer(Modifier.preferredSize(50.dp).testTag(itemOutsideLazyRow))
            }
        }

        onNodeWithTag(itemInsideLazyRow)
            .assertIsDisplayed()

        onNodeWithTag(itemOutsideLazyRow)
            .assertIsDisplayed()

        var lazyRowBounds = onNodeWithTag(LazyRowForTag)
            .getUnclippedBoundsInRoot()

        Truth.assertThat(lazyRowBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.right.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.bottom.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())

        runOnIdle {
            sameSizeItems = false
        }

        waitForIdle()

        onNodeWithTag(itemInsideLazyRow)
            .assertIsDisplayed()

        onNodeWithTag(itemOutsideLazyRow)
            .assertIsDisplayed()

        lazyRowBounds = onNodeWithTag(LazyRowForTag)
            .getUnclippedBoundsInRoot()

        Truth.assertThat(lazyRowBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.right.toIntPx()).isWithin1PixelFrom(120.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyRowBounds.bottom.toIntPx()).isWithin1PixelFrom(70.dp.toIntPx())
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyRowForAlignment(verticalGravity: Alignment.Vertical) {
        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(1, 2),
                modifier = Modifier.testTag(LazyRowForTag).height(100.dp),
                verticalGravity = verticalGravity
            ) {
                if (it == 1) {
                    Spacer(Modifier.preferredSize(50.dp).testTag(firstItemTag))
                } else {
                    Spacer(Modifier.preferredSize(70.dp).testTag(secondItemTag))
                }
            }
        }

        onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyColumnBounds = onNodeWithTag(LazyRowForTag)
            .getUnclippedBoundsInRoot()

        with(composeTestRule.density) {
            // Verify the height of the row
            Truth.assertThat(lazyColumnBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
            Truth.assertThat(lazyColumnBounds.bottom.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        }
    }

    @Test
    fun lazyRowAlignmentCenterVertically() {
        prepareLazyRowForAlignment(Alignment.CenterVertically)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 25.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 15.dp)
    }

    @Test
    fun lazyRowAlignmentTop() {
        prepareLazyRowForAlignment(Alignment.Top)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)
    }

    @Test
    fun lazyRowAlignmentBottom() {
        prepareLazyRowForAlignment(Alignment.Bottom)

        onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)

        onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 30.dp)
    }

    @Test
    fun itemFillingParentWidth() {
        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.fillParentMaxWidth().height(50.dp).testTag(firstItemTag))
            }
        }

        onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun itemFillingParentHeight() {
        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.width(50.dp).fillParentMaxHeight().testTag(firstItemTag))
            }
        }

        onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSize() {
        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(0),
                modifier = Modifier.size(width = 100.dp, height = 150.dp)
            ) {
                Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
            }
        }

        onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun itemFillingParentSizeParentResized() {
        var parentSize by mutableStateOf(100.dp)
        composeTestRule.setContent {
            LazyRowFor(
                items = listOf(0),
                modifier = Modifier.size(parentSize)
            ) {
                Spacer(Modifier.fillParentMaxSize().testTag(firstItemTag))
            }
        }

        runOnIdle {
            parentSize = 150.dp
        }

        onNodeWithTag(firstItemTag)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun scrollsLeftInRtl() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Stack(Modifier.preferredWidth(100.dp)) {
                    LazyRowFor(items, Modifier.testTag(LazyRowForTag)) {
                        Spacer(Modifier.preferredWidth(101.dp).fillParentMaxHeight().testTag(it))
                    }
                }
            }
        }

        onNodeWithTag(LazyRowForTag)
            .scrollBy(x = (-150).dp, density = composeTestRule.density)

        onNodeWithTag("1")
            .assertDoesNotExist()

        onNodeWithTag("2")
            .assertIsDisplayed()
    }
}
