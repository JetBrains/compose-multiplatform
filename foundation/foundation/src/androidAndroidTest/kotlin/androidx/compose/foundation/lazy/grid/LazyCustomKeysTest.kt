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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyCustomKeysTest {

    @get:Rule
    val rule = createComposeRule()

    val itemSize = with(rule.density) {
        100.toDp()
    }
    val columns = 2

    @Test
    fun itemsWithKeysAreLaidOutCorrectly() {
        val list = listOf(MyClass(0), MyClass(1), MyClass(2))

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item("${it.id}")
                }
            }
        }

        assertItems("0", "1", "2")
    }

    @Test
    fun removing_statesAreMoved() {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        rule.runOnIdle {
            list = listOf(list[0], list[2])
        }

        assertItems("0", "2")
    }

    @Test
    fun reordering_statesAreMoved_list() {
        testReordering { grid ->
            items(grid, key = { it.id }) {
                Item(remember { "${it.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_list_indexed() {
        testReordering { grid ->
            itemsIndexed(grid, key = { _, item -> item.id }) { _, item ->
                Item(remember { "${item.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_array() {
        testReordering { grid ->
            val array = grid.toTypedArray()
            items(array, key = { it.id }) {
                Item(remember { "${it.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_array_indexed() {
        testReordering { grid ->
            val array = grid.toTypedArray()
            itemsIndexed(array, key = { _, item -> item.id }) { _, item ->
                Item(remember { "${item.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_itemsWithCount() {
        testReordering { grid ->
            items(grid.size, key = { grid[it].id }) {
                Item(remember { "${grid[it].id}" })
            }
        }
    }

    @Test
    fun fullyReplacingTheList() {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        rule.runOnIdle {
            list = listOf(MyClass(3), MyClass(4), MyClass(5), MyClass(6))
        }

        assertItems("3", "4", "5", "6")
    }

    @Test
    fun keepingOneItem() {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        rule.runOnIdle {
            list = listOf(MyClass(1))
        }

        assertItems("1")
    }

    @Test
    fun keepingOneItemAndAddingMore() {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        rule.runOnIdle {
            list = listOf(MyClass(1), MyClass(3))
        }

        assertItems("1", "3")
    }

    @Test
    fun mixingKeyedItemsAndNot() {
        testReordering { list ->
            item {
                Item("${list.first().id}")
            }
            items(list.subList(fromIndex = 1, toIndex = list.size), key = { it.id }) {
                Item(remember { "${it.id}" })
            }
        }
    }

    @Test
    fun updatingTheDataSetIsCorrectlyApplied() {
        val state = mutableStateOf(emptyList<Int>())

        rule.setContent {
            LaunchedEffect(Unit) {
                state.value = listOf(4, 1, 3)
            }

            val list = state.value

            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize()) {
                items(list, key = { it }) {
                    Item(it.toString())
                }
            }
        }

        assertItems("4", "1", "3")

        rule.runOnIdle {
            state.value = listOf(2, 4, 6, 1, 3, 5)
        }

        assertItems("2", "4", "6", "1", "3", "5")
    }

    @Test
    fun reordering_usingMutableStateListOf() {
        val list = mutableStateListOf(MyClass(0), MyClass(1), MyClass(2))

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        rule.runOnIdle {
            list.add(list.removeAt(1))
        }

        assertItems("0", "2", "1")
    }

    @Test
    fun keysInLazyListItemInfoAreCorrect() {
        val list = listOf(MyClass(0), MyClass(1), MyClass(2))
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(columns), state = state) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        rule.runOnIdle {
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(0, 1, 2))
        }
    }

    @Test
    fun keysInLazyListItemInfoAreCorrectAfterReordering() {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(columns = GridCells.Fixed(columns), state = state) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        rule.runOnIdle {
            list = listOf(list[0], list[2], list[1])
        }

        rule.runOnIdle {
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(0, 2, 1))
        }
    }

    @Test
    fun addingItemsBeforeWithoutKeysIsMaintainingTheIndex() {
        var list by mutableStateOf((10..15).toList())
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.size(itemSize * 2.5f), state) {
                items(list) {
                    Item(remember { "$it" })
                }
            }
        }

        rule.runOnIdle {
            list = (0..15).toList()
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun addingItemsBeforeKeepingThisItemFirst() {
        var list by mutableStateOf((10..15).toList())
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        rule.runOnIdle {
            list = (0..15).toList()
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(10)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(10, 11, 12, 13, 14, 15))
        }
    }

    @Test
    fun addingItemsRightAfterKeepingThisItemFirst() {
        var list by mutableStateOf((0..5).toList() + (10..15).toList())
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState(5)
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        rule.runOnIdle {
            list = (0..15).toList()
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(4)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(4, 5, 6, 7, 8, 9))
        }
    }

    @Test
    fun addingItemsBeforeWhileCurrentItemIsNotInTheBeginning() {
        var list by mutableStateOf((10..30).toList())
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState(10) // key 20 is the first item
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        rule.runOnIdle {
            list = (0..30).toList()
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(20)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(20, 21, 22, 23, 24, 25))
        }
    }

    @Test
    fun removingTheCurrentItemMaintainsTheIndex() {
        var list by mutableStateOf((0..20).toList())
        lateinit var state: LazyGridState

        rule.setContent {
            state = rememberLazyGridState(8)
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        rule.runOnIdle {
            list = (0..20) - 8
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(8)
            assertThat(state.visibleKeys).isEqualTo(listOf(9, 10, 11, 12, 13, 14))
        }
    }

    private fun testReordering(content: LazyGridScope.(List<MyClass>) -> Unit) {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))

        rule.setContent {
            LazyVerticalGrid(GridCells.Fixed(columns)) {
                content(list)
            }
        }

        rule.runOnIdle {
            list = listOf(list[0], list[2], list[1])
        }

        assertItems("0", "2", "1")
    }

    private fun assertItems(vararg tags: String) {
        var currentTop = 0.dp
        var column = 0
        tags.forEach {
            rule.onNodeWithTag(it)
                .assertTopPositionInRootIsEqualTo(currentTop)
                .assertHeightIsEqualTo(itemSize)
            ++column
            if (column == columns) {
                currentTop += itemSize
                column = 0
            }
        }
    }

    @Composable
    private fun Item(tag: String) {
        Spacer(
            Modifier.testTag(tag).size(itemSize)
        )
    }

    private class MyClass(val id: Int)
}

val LazyGridState.visibleKeys: List<Any> get() = layoutInfo.visibleItemsInfo.map { it.key }
