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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
import androidx.compose.ui.semantics.SemanticsProperties.IndexForKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the semantics properties defined on a LazyGrid:
 * - GetIndexForKey
 * - ScrollToIndex
 *
 * GetIndexForKey:
 * Create a lazy grid, iterate over all indices, verify key of each of them
 *
 * ScrollToIndex:
 * Create a lazy grid, scroll to a line off screen, verify shown items
 *
 * All tests performed in [runTest], scenarios set up in the test methods.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class LazySemanticsTest {
    private val N = 20
    private val LazyGridTag = "lazy_grid"
    private val LazyGridModifier = Modifier.testTag(LazyGridTag).requiredSize(100.dp)

    private fun tag(index: Int): String = "tag_$index"
    private fun key(index: Int): String = "key_$index"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun itemSemantics_verticalGrid() {
        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(1), LazyGridModifier) {
                repeat(N) {
                    item(key = key(it)) {
                        SpacerInColumn(it)
                    }
                }
            }
        }
        runTest()
    }

    @Test
    fun itemsSemantics_verticalGrid() {
        rule.setContent {
            val state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(1), LazyGridModifier, state) {
                items(items = List(N) { it }, key = { key(it) }) {
                    SpacerInColumn(it)
                }
            }
        }
        runTest()
    }

    // @Test
    // fun itemSemantics_row() {
    //     rule.setContent {
    //         LazyRow(LazyGridModifier) {
    //             repeat(N) {
    //                 item(key = key(it)) {
    //                     SpacerInRow(it)
    //                 }
    //             }
    //         }
    //     }
    //     runTest()
    // }

    // @Test
    // fun itemsSemantics_row() {
    //     rule.setContent {
    //         LazyRow(LazyGridModifier) {
    //             items(items = List(N) { it }, key = { key(it) }) {
    //                 SpacerInRow(it)
    //             }
    //         }
    //     }
    //     runTest()
    // }

    private fun runTest() {
        checkViewport(firstExpectedItem = 0, lastExpectedItem = 3)

        // Verify IndexForKey
        rule.onNodeWithTag(LazyGridTag).assert(
            SemanticsMatcher.keyIsDefined(IndexForKey).and(
                SemanticsMatcher("keys match") { node ->
                    val actualIndex = node.config.getOrNull(IndexForKey)!!
                    (0 until N).all { expectedIndex ->
                        expectedIndex == actualIndex.invoke(key(expectedIndex))
                    }
                }
            )
        )

        // Verify ScrollToIndex
        rule.onNodeWithTag(LazyGridTag).assert(SemanticsMatcher.keyIsDefined(ScrollToIndex))

        invokeScrollToIndex(targetIndex = 10)
        checkViewport(firstExpectedItem = 10, lastExpectedItem = 13)

        invokeScrollToIndex(targetIndex = N - 1)
        checkViewport(firstExpectedItem = N - 4, lastExpectedItem = N - 1)
    }

    private fun invokeScrollToIndex(targetIndex: Int) {
        val node = rule.onNodeWithTag(LazyGridTag)
            .fetchSemanticsNode("Failed: invoke ScrollToIndex")
        rule.runOnUiThread {
            node.config[ScrollToIndex].action!!.invoke(targetIndex)
        }
    }

    private fun checkViewport(firstExpectedItem: Int, lastExpectedItem: Int) {
        if (firstExpectedItem > 0) {
            rule.onNodeWithTag(tag(firstExpectedItem - 1)).assertDoesNotExist()
        }
        (firstExpectedItem..lastExpectedItem).forEach {
            rule.onNodeWithTag(tag(it)).assertExists()
        }
        if (firstExpectedItem < N - 1) {
            rule.onNodeWithTag(tag(lastExpectedItem + 1)).assertDoesNotExist()
        }
    }

    @Composable
    private fun SpacerInColumn(index: Int) {
        Spacer(Modifier.testTag(tag(index)).requiredHeight(30.dp).fillMaxWidth())
    }

    @Composable
    private fun SpacerInRow(index: Int) {
        Spacer(Modifier.testTag(tag(index)).requiredWidth(30.dp).fillMaxHeight())
    }
}
