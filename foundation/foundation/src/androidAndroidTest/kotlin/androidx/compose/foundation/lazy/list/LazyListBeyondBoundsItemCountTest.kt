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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListBeyondBoundsItemCountTest(config: Config) :
    BaseLazyListTestWithOrientation(config.orientation) {
    private val beyondBoundsItemCount = config.beyondBoundsItemCount
    private val firstVisibleItem = config.firstVisibleItem
    private val ItemCount = 10

    @Test
    fun verifyItemsArePlacedBeforeAndAfterVisibleItems() {
        // Arrange
        val state = LazyListState(firstVisibleItem)

        // Act
        setLazyListContent(state = state) {
            items(ItemCount) {
                ListItem(index = it)
            }
        }

        // Assert
        assertBeforeItemsArePlaced(state)
        val visibleItemsNumber = state.layoutInfo.visibleItemsInfo.size
        repeat(visibleItemsNumber) {
            rule.onNodeWithTag((firstVisibleItem + it).toString()).assertIsDisplayed()
        }
        assertAfterItemsArePlaced(state)
    }

    @Test
    fun verifyItemsArePlacedBeforeAndAfterVisibleItemsAfterScroll() {
        // Arrange
        val state = LazyListState()
        setLazyListContent(state = state) {
            items(ItemCount) {
                ListItem(index = it)
            }
        }

        // Act
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(firstVisibleItem)
            }
        }

        // Assert
        assertBeforeItemsArePlaced(state)
        repeat(state.layoutInfo.visibleItemsInfo.size) {
            rule.onNodeWithTag("${firstVisibleItem + it}").assertIsDisplayed()
        }
        assertAfterItemsArePlaced(state)
    }

    private fun assertAfterItemsArePlaced(state: LazyListState) {
        if (firstVisibleItem >= ItemCount - 3) return
        val nonVisibleStartIndexAfter = state.layoutInfo.visibleItemsInfo.last().index + 1
        val nonVisibleEndIndexAfter =
            state.layoutInfo.visibleItemsInfo.last().index + beyondBoundsItemCount

        for (index in nonVisibleStartIndexAfter until nonVisibleEndIndexAfter) {
            rule.onNodeWithTag(index.toString()).assertIsPlaced()
        }
    }

    private fun assertBeforeItemsArePlaced(state: LazyListState) {
        if (firstVisibleItem <= 0) return
        val nonVisibleStartIndexBefore = state.layoutInfo.visibleItemsInfo.first().index - 1
        val nonVisibleEndIndexBefore =
            state.layoutInfo.visibleItemsInfo.first().index - beyondBoundsItemCount

        for (index in nonVisibleStartIndexBefore downTo nonVisibleEndIndexBefore) {
            rule.onNodeWithTag(index.toString()).assertIsPlaced()
        }
    }

    private fun setLazyListContent(
        state: LazyListState? = null,
        content: LazyListScope.() -> Unit
    ) {
        val lazyListState = state ?: LazyListState()
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.size(60.dp),
                state = lazyListState,
                content = content,
                beyondBoundsItemCount = beyondBoundsItemCount
            )
        }
    }

    @Composable
    private fun ListItem(index: Int) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .testTag(index.toString())
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = buildList {
            for (orientation in listOf(Orientation.Horizontal, Orientation.Vertical)) {
                for (beyondBoundsItemCount in listOf(0, 1, 2)) {
                    for (firstVisibleItem in listOf(0, 5, 7)) {
                        add(Config(orientation, beyondBoundsItemCount, firstVisibleItem))
                    }
                }
            }
        }

        class Config(
            val orientation: Orientation,
            val beyondBoundsItemCount: Int,
            val firstVisibleItem: Int
        ) {
            override fun toString(): String {
                return "orientation=$orientation " +
                    "beyondBoundsItemCount=$beyondBoundsItemCount " +
                    "firstVisibleItem=$firstVisibleItem"
            }
        }
    }
}

/**
 * Asserts that the current semantics node is placed.
 *
 * Throws [AssertionError] if the node is not placed.
 */
internal fun SemanticsNodeInteraction.assertIsPlaced(): SemanticsNodeInteraction {
    val errorMessageOnFail = "Assert failed: The component is not placed!"
    if (!fetchSemanticsNode(errorMessageOnFail).layoutInfo.isPlaced) {
        throw AssertionError(errorMessageOnFail)
    }
    return this
}