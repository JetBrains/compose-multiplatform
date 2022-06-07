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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class LazyItemStateRestoration {

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
            LazyVerticalGrid(GridCells.Fixed(1)) {
                item {
                    realState[0] = rememberSaveable { counter0++ }
                    Box(Modifier.requiredSize(1.dp))
                }
                items((1..2).toList()) {
                    if (it == 1) {
                        realState[1] = rememberSaveable { counter1++ }
                    } else {
                        realState[2] = rememberSaveable { counter2++ }
                    }
                    Box(Modifier.requiredSize(1.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
        }
    }

    @Test
    fun itemsStateRestoredWhenWeScrolledBackToIt() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        lateinit var state: LazyGridState
        var itemDisposed = false
        var realState = 0
        restorationTester.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.requiredSize(20.dp),
                state = rememberLazyGridState().also { state = it }
            ) {
                items((0..10).toList()) {
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
        }

        rule.runOnIdle {
            assertThat(realState).isEqualTo(1)
            runBlocking {
                // we scroll through multiple items to make sure the 0th element is not kept in
                // the reusable items buffer
                state.scrollToItem(3)
                state.scrollToItem(5)
                state.scrollToItem(8)
            }
        }

        rule.runOnIdle {
            assertThat(itemDisposed).isEqualTo(true)
            realState = 0
            runBlocking {
                state.scrollToItem(0, 0)
            }
        }

        rule.runOnIdle {
            assertThat(realState).isEqualTo(1)
        }
    }

    @Test
    fun itemsStateRestoredWhenWeScrolledRestoredAndScrolledBackTo() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        var counter1 = 10
        lateinit var state: LazyGridState
        var realState = arrayOf(0, 0)
        restorationTester.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.requiredSize(20.dp),
                state = rememberLazyGridState().also { state = it }
            ) {
                items((0..1).toList()) {
                    if (it == 0) {
                        realState[0] = rememberSaveable { counter0++ }
                    } else {
                        realState[1] = rememberSaveable { counter1++ }
                    }
                    Box(Modifier.requiredSize(30.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            runBlocking {
                state.scrollToItem(1, 5)
            }
        }

        rule.runOnIdle {
            assertThat(realState[1]).isEqualTo(10)
            realState = arrayOf(0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(realState[1]).isEqualTo(10)
            runBlocking {
                state.scrollToItem(0, 0)
            }
        }

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
        }
    }

    @Test
    fun nestedLazy_itemsStateRestoredWhenWeScrolledBackToIt() {
        val restorationTester = StateRestorationTester(rule)
        var counter0 = 1
        lateinit var state: LazyGridState
        var itemDisposed = false
        var realState = 0
        restorationTester.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.requiredSize(20.dp),
                state = rememberLazyGridState().also { state = it }
            ) {
                items((0..10).toList()) {
                    if (it == 0) {
                        LazyRow {
                            item {
                                realState = rememberSaveable { counter0++ }
                                DisposableEffect(Unit) {
                                    onDispose {
                                        itemDisposed = true
                                    }
                                }
                                Box(Modifier.requiredSize(30.dp))
                            }
                        }
                    } else {
                        Box(Modifier.requiredSize(30.dp))
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(realState).isEqualTo(1)
            runBlocking {
                // we scroll through multiple items to make sure the 0th element is not kept in
                // the reusable items buffer
                state.scrollToItem(3)
                state.scrollToItem(5)
                state.scrollToItem(8)
            }
        }

        rule.runOnIdle {
            assertThat(itemDisposed).isEqualTo(true)
            realState = 0
            runBlocking {
                state.scrollToItem(0, 0)
            }
        }

        rule.runOnIdle {
            assertThat(realState).isEqualTo(1)
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
            LazyVerticalGrid(GridCells.Fixed(1)) {
                items(3, key = { "$it" }) {
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
        }

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
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
            LazyVerticalGrid(GridCells.Fixed(1)) {
                items(list, key = { "$it" }) {
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
        }

        rule.runOnIdle {
            list = listOf(1, 2)
        }

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(1)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
            realState = arrayOf(0, 0, 0)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(realState[0]).isEqualTo(0)
            assertThat(realState[1]).isEqualTo(10)
            assertThat(realState[2]).isEqualTo(100)
        }
    }
}
