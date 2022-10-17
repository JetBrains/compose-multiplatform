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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListBeyondBoundsAndExtraItemsTest(val config: Config) :
    BaseLazyListTestWithOrientation(config.orientation) {

    private val beyondBoundsLayoutDirection = config.beyondBoundsLayoutDirection
    private val reverseLayout = config.reverseLayout
    private val layoutDirection = config.layoutDirection

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun verifyItemsArePlacedBeforeBeyondBoundsItems_oneBeyondBoundItem() {
        // Arrange
        val placedItems = mutableSetOf<Int>()
        var beyondBoundsLayout: BeyondBoundsLayout? = null
        val lazyListState = LazyListState()
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyColumnOrRow(
                    modifier = Modifier.size(30.dp),
                    state = lazyListState,
                    beyondBoundsItemCount = 1,
                    reverseLayout = reverseLayout
                ) {
                    items(5) { index ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += index }
                        )
                    }
                    item {
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += 5 }
                                .modifierLocalConsumer {
                                    beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                                }
                        )
                    }
                    items(5) { index ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += index + 6 }
                        )
                    }
                }
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Beyond bounds items are present.
                if (expectedExtraItemsBeforeVisibleBounds()) {
                    assertThat(placedItems).containsAtLeast(3, 4, 5, 6, 7, 8)
                } else {
                    assertThat(placedItems).containsAtLeast(4, 5, 6, 7, 8, 9)
                }
                assertThat(lazyListState.visibleItems).containsAtLeast(5, 6, 7)
                placedItems.clear()
                true
            }
        }

        // Beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems).containsAtLeast(4, 5, 6, 7, 8)
            assertThat(lazyListState.visibleItems).containsAtLeast(5, 6, 7)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun verifyItemsArePlacedBeforeBeyondBoundsItems_twoBeyondBoundItem() {
        // Arrange
        val placedItems = mutableSetOf<Int>()
        var beyondBoundsLayout: BeyondBoundsLayout? = null
        val lazyListState = LazyListState()
        var extraItemCount = 2
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyColumnOrRow(
                    modifier = Modifier.size(30.dp),
                    state = lazyListState,
                    beyondBoundsItemCount = 1,
                    reverseLayout = reverseLayout
                ) {
                    items(5) { index ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += index }
                        )
                    }
                    item {
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += 5 }
                                .modifierLocalConsumer {
                                    beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                                }
                        )
                    }
                    items(5) { index ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .onPlaced { placedItems += index + 6 }
                        )
                    }
                }
            }
        }
        rule.runOnIdle { runBlocking { lazyListState.scrollToItem(5) } }
        rule.runOnIdle { placedItems.clear() }

        // Act
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                if (--extraItemCount > 0) {
                    placedItems.clear()
                    // Return null to continue the search.
                    null
                } else {
                    // Beyond bounds items are present.
                    if (expectedExtraItemsBeforeVisibleBounds()) {
                        assertThat(placedItems).containsAtLeast(2, 3, 4, 5, 6, 7, 8)
                    } else {
                        assertThat(placedItems).containsAtLeast(4, 5, 6, 7, 8, 9, 10)
                    }
                    assertThat(lazyListState.visibleItems).containsAtLeast(5, 6, 7)
                    placedItems.clear()
                    true
                }
            }
        }

        // Beyond bounds items are removed
        rule.runOnIdle {
            assertThat(placedItems).containsAtLeast(4, 5, 6, 7, 8)
            assertThat(lazyListState.visibleItems).containsAtLeast(5, 6, 7)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = buildList {
            for (orientation in listOf(Orientation.Horizontal, Orientation.Vertical)) {
                for (beyondBoundsLayoutDirection in listOf(
                    Left,
                    Right,
                    Above,
                    Below,
                    Before,
                    After
                )) {
                    for (reverseLayout in listOf(false, true)) {
                        for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                            add(
                                Config(
                                    orientation,
                                    beyondBoundsLayoutDirection,
                                    reverseLayout,
                                    layoutDirection
                                )
                            )
                        }
                    }
                }
            }
        }

        class Config(
            val orientation: Orientation,
            val beyondBoundsLayoutDirection: BeyondBoundsLayout.LayoutDirection,
            val reverseLayout: Boolean,
            val layoutDirection: LayoutDirection
        ) {
            override fun toString(): String {
                return "orientation=$orientation " +
                    "beyondBoundsLayoutDirection=$beyondBoundsLayoutDirection " +
                    "reverseLayout=$reverseLayout " +
                    "layoutDirection=$layoutDirection"
            }
        }
    }

    private val LazyListState.visibleItems: List<Int>
        get() = layoutInfo.visibleItemsInfo.map { it.index }

    private fun expectedExtraItemsBeforeVisibleBounds() = when (beyondBoundsLayoutDirection) {
        Right -> if (layoutDirection == LayoutDirection.Ltr) reverseLayout else !reverseLayout
        Left -> if (layoutDirection == LayoutDirection.Ltr) !reverseLayout else reverseLayout
        Above -> !reverseLayout
        Below -> reverseLayout
        After -> false
        Before -> true
        else -> error("Unsupported BeyondBoundsDirection")
    }
}