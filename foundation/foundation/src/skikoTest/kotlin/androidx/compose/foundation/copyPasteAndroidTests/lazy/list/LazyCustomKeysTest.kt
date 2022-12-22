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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class LazyCustomKeysTest {

    val density = Density(1f)

    val itemSize = with(density) {
        100.toDp()
    }

    @Test
    fun itemsWithKeysAreLaidOutCorrectly() = runSkikoComposeUiTest {
        val list = listOf(MyClass(0), MyClass(1), MyClass(2))

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item("${it.id}")
                }
            }
        }

        assertItems("0", "1", "2")
    }

    @Test
    fun removing_statesAreMoved() = runSkikoComposeUiTest {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        runOnIdle {
            list = listOf(list[0], list[2])
        }

        assertItems("0", "2")
    }

    @Test
    fun reordering_statesAreMoved_list() = runSkikoComposeUiTest {
        testReordering { list ->
            items(list, key = { it.id }) {
                Item(remember { "${it.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_list_indexed() = runSkikoComposeUiTest {
        testReordering { list ->
            itemsIndexed(list, key = { _, item -> item.id }) { _, item ->
                Item(remember { "${item.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_array() = runSkikoComposeUiTest {
        testReordering { list ->
            val array = list.toTypedArray()
            items(array, key = { it.id }) {
                Item(remember { "${it.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_array_indexed() = runSkikoComposeUiTest {
        testReordering { list ->
            val array = list.toTypedArray()
            itemsIndexed(array, key = { _, item -> item.id }) { _, item ->
                Item(remember { "${item.id}" })
            }
        }
    }

    @Test
    fun reordering_statesAreMoved_itemsWithCount() = runSkikoComposeUiTest {
        testReordering { list ->
            items(list.size, key = { list[it].id }) {
                Item(remember { "${list[it].id}" })
            }
        }
    }

    @Test
    fun fullyReplacingTheList() = runSkikoComposeUiTest {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        runOnIdle {
            list = listOf(MyClass(3), MyClass(4), MyClass(5), MyClass(6))
        }

        assertItems("3", "4", "5", "6")
    }

    @Test
    fun keepingOneItem() = runSkikoComposeUiTest {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        runOnIdle {
            list = listOf(MyClass(1))
        }

        assertItems("1")
    }

    @Test
    fun keepingOneItemAndAddingMore() = runSkikoComposeUiTest {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        var counter = 0

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item(remember { counter++ }.toString())
                }
            }
        }

        runOnIdle {
            list = listOf(MyClass(1), MyClass(3))
        }

        assertItems("1", "3")
    }

    @Test
    fun mixingKeyedItemsAndNot() = runSkikoComposeUiTest {
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
    fun updatingTheDataSetIsCorrectlyApplied() = runSkikoComposeUiTest {
        val state = mutableStateOf(emptyList<Int>())

        setContent {
            LaunchedEffect(Unit) {
                state.value = listOf(4, 1, 3)
            }

            val list = state.value

            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it }) {
                    Item(it.toString())
                }
            }
        }

        assertItems("4", "1", "3")

        runOnIdle {
            state.value = listOf(2, 4, 6, 1, 3, 5)
        }

        assertItems("2", "4", "6", "1", "3", "5")
    }

    @Test
    fun reordering_usingMutableStateListOf() = runSkikoComposeUiTest {
        val list = mutableStateListOf(MyClass(0), MyClass(1), MyClass(2))

        setContent {
            LazyColumn {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        runOnIdle {
            list.add(list.removeAt(1))
        }

        assertItems("0", "2", "1")
    }

    @Test
    fun keysInLazyListItemInfoAreCorrect() = runSkikoComposeUiTest {
        val list = listOf(MyClass(0), MyClass(1), MyClass(2))
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState()
            LazyColumn(state = state) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        runOnIdle {
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(0, 1, 2))
        }
    }

    @Test
    fun keysInLazyListItemInfoAreCorrectAfterReordering() = runSkikoComposeUiTest {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState()
            LazyColumn(state = state) {
                items(list, key = { it.id }) {
                    Item(remember { "${it.id}" })
                }
            }
        }

        runOnIdle {
            list = listOf(list[0], list[2], list[1])
        }

        runOnIdle {
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(0, 2, 1))
        }
    }

    @Test
    fun addingItemsBeforeWithoutKeysIsMaintainingTheIndex() = runSkikoComposeUiTest {
        var list by mutableStateOf((10..15).toList())
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState()
            LazyColumn(Modifier.size(itemSize * 2.5f), state) {
                items(list) {
                    Item(remember { "$it" })
                }
            }
        }

        runOnIdle {
            list = (0..15).toList()
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun addingItemsBeforeKeepingThisItemFirst() = runSkikoComposeUiTest {
        var list by mutableStateOf((10..15).toList())
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState()
            LazyColumn(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        runOnIdle {
            list = (0..15).toList()
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(10)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(10, 11, 12))
        }
    }

    @Test
    fun addingItemsRightAfterKeepingThisItemFirst() = runSkikoComposeUiTest {
        var list by mutableStateOf((0..5).toList() + (10..15).toList())
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState(5)
            LazyColumn(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        runOnIdle {
            list = (0..15).toList()
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(5)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(5, 6, 7))
        }
    }

    @Test
    fun addingItemsBeforeWhileCurrentItemIsNotInTheBeginning() = runSkikoComposeUiTest {
        var list by mutableStateOf((10..30).toList())
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState(10) // key 20 is the first item
            LazyColumn(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        runOnIdle {
            list = (0..30).toList()
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(20)
            assertThat(
                state.visibleKeys
            ).isEqualTo(listOf(20, 21, 22))
        }
    }

    @Test
    fun removingTheCurrentItemMaintainsTheIndex() = runSkikoComposeUiTest {
        var list by mutableStateOf((0..20).toList())
        lateinit var state: LazyListState

        setContent {
            state = rememberLazyListState(5)
            LazyColumn(Modifier.size(itemSize * 2.5f), state) {
                items(list, key = { it }) {
                    Item(remember { "$it" })
                }
            }
        }

        runOnIdle {
            list = (0..20) - 5
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(5)
            assertThat(state.visibleKeys).isEqualTo(listOf(6, 7, 8))
        }
    }

    private fun SkikoComposeUiTest.testReordering(content: LazyListScope.(List<MyClass>) -> Unit) {
        var list by mutableStateOf(listOf(MyClass(0), MyClass(1), MyClass(2)))

        setContent {
            LazyColumn {
                content(list)
            }
        }

        runOnIdle {
            list = listOf(list[0], list[2], list[1])
        }

        assertItems("0", "2", "1")
    }

    private fun SkikoComposeUiTest.assertItems(vararg tags: String) {
        var currentTop = 0.dp
        tags.forEach {
            onNodeWithTag(it)
                .assertTopPositionInRootIsEqualTo(currentTop)
                .assertHeightIsEqualTo(itemSize)
            currentTop += itemSize
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

val LazyListState.visibleKeys: List<Any> get() = layoutInfo.visibleItemsInfo.map { it.key }