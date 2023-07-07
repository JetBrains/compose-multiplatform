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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LazyListsReverseLayoutTest {

    private val ContainerTag = "ContainerTag"

    @get:Rule
    val rule = createComposeRule()

    private var itemSize: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSize = 50.toDp()
        }
    }

    @Test
    fun column_emitTwoElementsAsOneItem_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_emitTwoItems_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                }
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_initialScrollPositionIs0() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun column_scrollInWrongDirectionDoesNothing() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        // we scroll down and as the scrolling is reversed it shouldn't affect anything
        rule.onNodeWithTag(ContainerTag)
            .scrollBy(y = itemSize, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_scrollForwardHalfWay() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(y = -itemSize * 0.5f, density = rule.density)

        val scrolled = rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(-itemSize + scrolled)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(scrolled)
        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize + scrolled)
    }

    @Test
    fun column_scrollForwardTillTheEnd() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..3).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        // we scroll a bit more than it is possible just to make sure we would stop correctly
        rule.onNodeWithTag(ContainerTag)
            .scrollBy(y = -itemSize * 2.2f, density = rule.density)

        rule.runOnIdle {
            with(rule.density) {
                val realOffset = state.firstVisibleItemScrollOffset.toDp() +
                    itemSize * state.firstVisibleItemIndex
                assertThat(realOffset).isEqualTo(itemSize * 2)
            }
        }

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_emitTwoElementsAsOneItem_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_emitTwoItems_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                }
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_initialScrollPositionIs0() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun row_scrollInWrongDirectionDoesNothing() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        // we scroll down and as the scrolling is reversed it shouldn't affect anything
        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = itemSize, density = rule.density)

        rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_scrollForwardHalfWay() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..2).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = -itemSize * 0.5f, density = rule.density)

        val scrolled = rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(-itemSize + scrolled)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(scrolled)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize + scrolled)
    }

    @Test
    fun row_scrollForwardTillTheEnd() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = true,
                state = rememberLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
            ) {
                items((0..3).toList()) {
                    Box(Modifier.requiredSize(itemSize).testTag("$it"))
                }
            }
        }

        // we scroll a bit more than it is possible just to make sure we would stop correctly
        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = -itemSize * 2.2f, density = rule.density)

        rule.runOnIdle {
            with(rule.density) {
                val realOffset = state.firstVisibleItemScrollOffset.toDp() +
                    itemSize * state.firstVisibleItemIndex
                assertThat(realOffset).isEqualTo(itemSize * 2)
            }
        }

        rule.onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_rtl_emitTwoElementsAsOneItem_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyRow(
                    reverseLayout = true
                ) {
                    item {
                        Box(Modifier.requiredSize(itemSize).testTag("0"))
                        Box(Modifier.requiredSize(itemSize).testTag("1"))
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun row_rtl_emitTwoItems_positionedReversed() {
        rule.setContentWithTestViewConfiguration {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyRow(
                    reverseLayout = true
                ) {
                    item {
                        Box(Modifier.requiredSize(itemSize).testTag("0"))
                    }
                    item {
                        Box(Modifier.requiredSize(itemSize).testTag("1"))
                    }
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun row_rtl_scrollForwardHalfWay() {
        lateinit var state: LazyListState
        rule.setContentWithTestViewConfiguration {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyRow(
                    reverseLayout = true,
                    state = rememberLazyListState().also { state = it },
                    modifier = Modifier.requiredSize(itemSize * 2).testTag(ContainerTag)
                ) {
                    items((0..2).toList()) {
                        Box(Modifier.requiredSize(itemSize).testTag("$it"))
                    }
                }
            }
        }

        rule.onNodeWithTag(ContainerTag)
            .scrollBy(x = itemSize * 0.5f, density = rule.density)

        val scrolled = rule.runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(rule.density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(-scrolled)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize - scrolled)
        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 - scrolled)
    }

    @Test
    fun column_whenParameterChanges() {
        var reverse by mutableStateOf(true)
        rule.setContentWithTestViewConfiguration {
            LazyColumn(
                reverseLayout = reverse
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            reverse = false
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_whenParameterChanges() {
        var reverse by mutableStateOf(true)
        rule.setContentWithTestViewConfiguration {
            LazyRow(
                reverseLayout = reverse
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.runOnIdle {
            reverse = false
        }

        rule.onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }
}
