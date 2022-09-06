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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyLayoutStateRestorationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun visibleItemsStateRestored() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        var counter1 = 10
        var counter2 = 100
        var realState = arrayOf(0, 0, 0)
        restorationTester.setContent {
            LazyLayout(3) {
                if (it == 0) {
                    realState[0] = rememberSaveable { counter0++ }
                } else if (it == 1) {
                    realState[1] = rememberSaveable { counter1++ }
                } else {
                    realState[2] = rememberSaveable { counter2++ }
                }
                Box(Modifier.requiredSize(1.dp))
            }
        }

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(1)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(1)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
        }
    }

    @Test
    fun itemsStateRestoredWhenWeScrolledBackToIt() {
        var counter0 = 1
        var itemDisposed = false
        var visibleItem by mutableStateOf(0)
        var realState = 0
        rule.setContent {
            LazyLayout(
                itemCount = 2,
                itemIsVisible = { it == visibleItem }
            ) {
                if (it == 0) {
                    realState = rememberSaveable { counter0++ }
                    DisposableEffect(Unit) {
                        onDispose {
                            itemDisposed = true
                        }
                    }
                }
                Box(Modifier.requiredSize(30.dp))
            }
        }

        rule.runOnIdle {
            Truth.assertThat(realState).isEqualTo(1)
            visibleItem = 1
        }

        rule.runOnIdle {
            Truth.assertThat(itemDisposed).isEqualTo(true)
            realState = 0
            visibleItem = 0
        }

        rule.runOnIdle {
            Truth.assertThat(realState).isEqualTo(1)
        }
    }

    @Test
    fun nestedLazy_itemsStateRestoredWhenWeScrolledBackToIt() {
        var counter0 = 1
        var visibleItem by mutableStateOf(0)
        var itemDisposed = false
        var realState = 0
        rule.setContent {
            LazyLayout(
                itemCount = 2,
                itemIsVisible = { it == visibleItem }
            ) {
                if (it == 0) {
                    LazyLayout(itemCount = 1) {
                        realState = rememberSaveable { counter0++ }
                        DisposableEffect(Unit) {
                            onDispose {
                                itemDisposed = true
                            }
                        }
                        Box(Modifier.requiredSize(30.dp))
                    }
                } else {
                    Box(Modifier.requiredSize(30.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(realState).isEqualTo(1)
            visibleItem = 1
        }

        rule.runOnIdle {
            Truth.assertThat(itemDisposed).isEqualTo(true)
            realState = 0
            visibleItem = 0
        }

        rule.runOnIdle {
            Truth.assertThat(realState).isEqualTo(1)
        }
    }

    @Test
    fun stateRestoredWhenUsedWithCustomKeys() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        var counter1 = 10
        var counter2 = 100
        var realState = arrayOf(0, 0, 0)
        restorationTester.setContent {
            LazyLayout(
                itemCount = 3,
                indexToKey = { "$it" }
            ) {
                if (it == 0) {
                    realState[0] = rememberSaveable { counter0++ }
                } else if (it == 1) {
                    realState[1] = rememberSaveable { counter1++ }
                } else {
                    realState[2] = rememberSaveable { counter2++ }
                }
                Box(Modifier.requiredSize(1.dp))
            }
        }

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(1)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(1)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
        }
    }

    @Test
    fun stateRestoredWhenUsedWithCustomKeysAfterReordering() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        var counter1 = 10
        var counter2 = 100
        var realState = arrayOf(0, 0, 0)
        var list by mutableStateOf(listOf(0, 1, 2))
        restorationTester.setContent {
            LazyLayout(
                itemCount = list.size,
                indexToKey = { "${list[it]}" }
            ) { index ->
                val it = list[index]
                if (it == 0) {
                    realState[0] = rememberSaveable { counter0++ }
                } else if (it == 1) {
                    realState[1] = rememberSaveable { counter1++ }
                } else {
                    realState[2] = rememberSaveable { counter2++ }
                }
                Box(Modifier.requiredSize(1.dp))
            }
        }

        rule.runOnIdle {
            list = listOf(1, 2)
        }

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(1)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            Truth.assertThat(realState[0]).isEqualTo(0)
            Truth.assertThat(realState[1]).isEqualTo(10)
            Truth.assertThat(realState[2]).isEqualTo(100)
        }
    }

    @Test
    fun itemsStateRestoredOnlyForVisibleItemsWhenStateSaved_100items() {
        val restorationTester = StateRestorationTester(rule)
        var stateToUse = 1
        var visibleRange by mutableStateOf(0 until 90)
        var realState = Array(100) {
            0
        }
        restorationTester.setContent {
            LazyLayout(
                itemCount = 100,
                itemIsVisible = { visibleRange.contains(it) }
            ) {
                realState[it] = rememberSaveable { stateToUse }
                Box(Modifier.requiredSize(30.dp))
            }
        }

        rule.runOnIdle {
            visibleRange = 90 until 100
        }

        rule.runOnIdle {
            // all states were initialized with 1
            Truth.assertThat(realState).isEqualTo(Array(100) { 1 })
            // reset states as we will get the new values
            realState = Array(100) { 0 }
            // compositions without restored state now will use state 2
            stateToUse = 2
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            visibleRange = 0 until 90
        }

        rule.runOnIdle {
            Truth.assertThat(realState).isEqualTo(Array(100) {
                if (it >= 90) 1 else 2
            })
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun LazyLayout(
        itemCount: Int,
        itemIsVisible: (Int) -> Boolean = { true },
        indexToKey: (Int) -> Any = { getDefaultLazyLayoutKey(it) },
        content: @Composable (Int) -> Unit
    ) {
        LazyLayout(
            itemProvider = remember(itemCount, content as Any) {
                object : LazyLayoutItemProvider {
                    override val itemCount: Int = itemCount

                    @Composable
                    override fun Item(index: Int) {
                        content(index)
                    }

                    override fun getKey(index: Int) = indexToKey(index)
                }
            }
        ) { constraints ->
            val placeables = mutableListOf<Placeable>()
            repeat(itemCount) { index ->
                if (itemIsVisible(index)) {
                    placeables.addAll(measure(index, constraints))
                }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        }
    }
}
