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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridSemanticTest(
    private val orientation: Orientation
) : BaseLazyStaggeredGridWithOrientation(orientation) {
    companion object {
        private const val LazyStaggeredGridTag = "LazyStaggeredGridTag"
        private const val ItemCount = 60

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(
            Orientation.Vertical,
            Orientation.Horizontal,
        )
    }

    private fun key(index: Int): String = "key_$index"
    private fun tag(index: Int): String = "tag_$index"

    private var itemSizeDp: Dp = Dp.Unspecified
    private val itemSizePx: Int = 100

    @Before
    fun setUp() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @Test
    fun semantics_item() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGridTag)
                    .mainAxisSize(itemSizeDp * 3 - 1.dp) // -1 to prevent laying out more items
                    .crossAxisSize(itemSizeDp * 2)
            ) {
                repeat(ItemCount) {
                    item(key = key(it)) {
                        Spacer(Modifier.testTag(tag(it)).mainAxisSize(itemSizeDp))
                    }
                }
            }
        }
        runTest()
    }

    @Test
    fun semantics_items() {
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 2,
                modifier = Modifier
                    .testTag(LazyStaggeredGridTag)
                    .mainAxisSize(itemSizeDp * 3 - 1.dp) // -1 to prevent laying out more items
                    .crossAxisSize(itemSizeDp * 2)
            ) {
                items(items = List(ItemCount) { it }, key = { key(it) }) {
                    BasicText("$it", Modifier.testTag(tag(it)).mainAxisSize(itemSizeDp))
                }
            }
        }
        runTest()
    }

    private fun runTest() {
        checkViewport(firstExpectedItem = 0, lastExpectedItem = 5)

        // Verify IndexForKey
        rule.onNodeWithTag(LazyStaggeredGridTag).assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.IndexForKey).and(
                SemanticsMatcher("keys match") { node ->
                    val actualIndex = node.config.getOrNull(SemanticsProperties.IndexForKey)!!
                    (0 until ItemCount).all { expectedIndex ->
                        expectedIndex == actualIndex.invoke(key(expectedIndex))
                    }
                }
            )
        )

        // Verify ScrollToIndex
        rule.onNodeWithTag(LazyStaggeredGridTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))

        invokeScrollToIndex(targetIndex = 30)
        checkViewport(firstExpectedItem = 30, lastExpectedItem = 35)

        invokeScrollToIndex(targetIndex = ItemCount - 1)
        checkViewport(firstExpectedItem = ItemCount - 6, lastExpectedItem = ItemCount - 1)
    }

    private fun invokeScrollToIndex(targetIndex: Int) {
        val node = rule.onNodeWithTag(LazyStaggeredGridTag)
            .fetchSemanticsNode("Failed: invoke ScrollToIndex")
        rule.runOnUiThread {
            node.config[SemanticsActions.ScrollToIndex].action!!.invoke(targetIndex)
        }
    }

    private fun checkViewport(firstExpectedItem: Int, lastExpectedItem: Int) {
        if (firstExpectedItem > 0) {
            rule.onNodeWithTag(tag(firstExpectedItem - 1)).assertDoesNotExist()
        }
        (firstExpectedItem..lastExpectedItem).forEach {
            rule.onNodeWithTag(tag(it)).assertExists()
        }
        if (firstExpectedItem < ItemCount - 1) {
            rule.onNodeWithTag(tag(lastExpectedItem + 1)).assertDoesNotExist()
        }
    }
}
