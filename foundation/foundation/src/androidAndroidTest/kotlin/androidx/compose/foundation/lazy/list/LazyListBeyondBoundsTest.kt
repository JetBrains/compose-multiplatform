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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyListBeyondBoundsTest(param: Param) {

    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(
        val beyondBoundsLayoutDirection: BeyondBoundsLayout.LayoutDirection,
        val reverseLayout: Boolean,
        val layoutDirection: LayoutDirection,
    ) {
        override fun toString() = "beyondBoundsLayoutDirection=$beyondBoundsLayoutDirection " +
            "reverseLayout=$reverseLayout " +
            "layoutDirection=$layoutDirection"
    }

    private val beyondBoundsLayoutDirection = param.beyondBoundsLayoutDirection
    private val reverseLayout = param.reverseLayout
    private val layoutDirection = param.layoutDirection
    private val placedItems = mutableSetOf<Int>()
    private var beyondBoundsLayout: BeyondBoundsLayout? = null
    private lateinit var lazyListState: LazyListState

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = buildList {
            for (beyondBoundsLayoutDirection in listOf(Left, Right, Above, Below, Before, After)) {
                for (reverseLayout in listOf(false, true)) {
                    for (layoutDirection in listOf(Ltr, Rtl)) {
                        add(Param(beyondBoundsLayoutDirection, reverseLayout, layoutDirection))
                    }
                }
            }
        }
    }

    @Test
    fun onlyOneVisibleItemIsPlaced() {
        // Arrange.
        rule.setLazyContent(10.dp) {
            items(100) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'contains' with 'containsExactly'.
            assertThat(placedItems).contains(0)
            assertThat(visibleItems).contains(0)
        }
    }

    @Test
    fun onlyTwoVisibleItemsArePlaced() {
        // Arrange.
        rule.setLazyContent(20.dp) {
            items(100) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(0, 1)
            assertThat(visibleItems).containsAtLeast(0, 1)
        }
    }

    @Test
    fun onlyThreeVisibleItemsArePlaced() {
        // Arrange.
        rule.setLazyContent(30.dp) {
            items(100) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(0, 1, 2)
            assertThat(visibleItems).containsAtLeast(0, 1, 2)
        }
    }

    @Test
    fun oneExtraItemBeyondVisibleBounds() {
        // Arrange.
        rule.setLazyContent(30.dp) {
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
            item {
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += 5 }
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                )
            }
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index + 6 }
                )
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that the beyond bounds items are present.
                if (expectedExtraItemsBeforeVisibleBounds()) {
                    // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                    assertThat(placedItems).containsAtLeast(4, 5, 6, 7)
                    assertThat(visibleItems).containsAtLeast(5, 6, 7)
                } else {
                    // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                    assertThat(placedItems).containsAtLeast(5, 6, 7, 8)
                    assertThat(visibleItems).containsAtLeast(5, 6, 7)
                }
                placedItems.clear()
                // Just return true so that we stop as soon as we run this once.
                // This should result in one extra item being added.
                true
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(5, 6, 7)
            assertThat(visibleItems).containsAtLeast(5, 6, 7)
        }
    }

    @Test
    fun twoExtraItemsBeyondVisibleBounds() {
        // Arrange.
        var extraItemCount = 2
        rule.setLazyContent(30.dp) {
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
            item {
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += 5 }
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                )
            }
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index + 6 }
                )
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                if (--extraItemCount > 0) {
                    placedItems.clear()
                    // Return null to continue the search.
                    null
                } else {
                    // Assert that the beyond bounds items are present.
                    if (expectedExtraItemsBeforeVisibleBounds()) {
                        // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                        assertThat(placedItems).containsAtLeast(3, 4, 5, 6, 7)
                        assertThat(visibleItems).containsAtLeast(5, 6, 7)
                    } else {
                        // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                        assertThat(placedItems).containsAtLeast(5, 6, 7, 8, 9)
                        assertThat(visibleItems).containsAtLeast(5, 6, 7)
                    }
                    placedItems.clear()
                    // Return true to stop the search.
                    true
                }
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(5, 6, 7)
            assertThat(visibleItems).containsAtLeast(5, 6, 7)
        }
    }

    @Test
    fun allBeyondBoundsItemsInSpecifiedDirection() {
        // Arrange.
        rule.setLazyContent(30.dp) {
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index }
                )
            }
            item {
                Box(Modifier
                    .size(10.dp)
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                    .onPlaced { placedItems += 5 }
                )
            }
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index + 6
                    }
                )
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                if (hasMoreContent) {
                    placedItems.clear()
                    // Just return null so that we keep adding more items till we reach the end.
                    null
                } else {
                    // Assert that the beyond bounds items are present.
                    if (expectedExtraItemsBeforeVisibleBounds()) {
                        // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                        assertThat(placedItems).containsAtLeast(0, 1, 2, 3, 4, 5, 6, 7)
                        assertThat(visibleItems).containsAtLeast(5, 6, 7)
                    } else {
                        // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
                        assertThat(placedItems).containsAtLeast(5, 6, 7, 8, 9, 10)
                        assertThat(visibleItems).containsAtLeast(5, 6, 7)
                    }
                    placedItems.clear()
                    // Return true to end the search.
                    true
                }
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(5, 6, 7)
        }
    }

    @Test
    fun returningNullDoesNotCauseInfiniteLoop() {
        // Arrange.
        rule.setLazyContent(30.dp) {
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index
                    }
                )
            }
            item {
                Box(Modifier
                    .size(10.dp)
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                    .onPlaced { placedItems += 5 }
                )
            }
            items(5) { index ->
                Box(Modifier
                    .size(10.dp)
                    .onPlaced { placedItems += index + 6
                    }
                )
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act.
        var count = 0
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that we don't keep iterating when there is no ending condition.
                assertThat(count++).isLessThan(lazyListState.layoutInfo.totalItemsCount)
                placedItems.clear()
                // Always return null to continue the search.
                null
                }
            }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            // TODO(b/228100623): Replace 'containsAtLeast' with 'containsExactly'.
            assertThat(placedItems).containsAtLeast(5, 6, 7)
            assertThat(visibleItems).containsAtLeast(5, 6, 7)
        }
    }

    private fun ComposeContentTestRule.setLazyContent(size: Dp, content: LazyListScope.() -> Unit) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                lazyListState = rememberLazyListState()
                when (beyondBoundsLayoutDirection) {
                    Left, Right, Before, After ->
                        LazyRow(
                            modifier = Modifier.size(size),
                            state = lazyListState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    Above, Below ->
                        LazyColumn(
                            modifier = Modifier.size(size),
                            state = lazyListState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    else -> error("Unsupported BeyondBoundsDirection")
                }
            }
        }
    }

    private val visibleItems: List<Int>
        get() = lazyListState.layoutInfo.visibleItemsInfo.map { it.index }

    private fun expectedExtraItemsBeforeVisibleBounds() = when (beyondBoundsLayoutDirection) {
        Right -> if (layoutDirection == Ltr) reverseLayout else !reverseLayout
        Left -> if (layoutDirection == Ltr) !reverseLayout else reverseLayout
        Above -> !reverseLayout
        Below -> reverseLayout
        After -> false
        Before -> true
        else -> error("Unsupported BeyondBoundsDirection")
    }
}
