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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.isEqualTo
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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LazyListsReverseLayoutTest {

    private val ContainerTag = "ContainerTag"


    private var itemSize: Dp = Dp.Infinity

    private val density = Density(1f)

    @BeforeTest
    fun before() {
        with(density) {
            itemSize = 50.toDp()
        }
    }

    @Test
    fun column_emitTwoElementsAsOneItem_positionedReversed() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_emitTwoItems_positionedReversed() = runSkikoComposeUiTest {
        setContent {
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

        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun column_initialScrollPositionIs0() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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

        runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun column_scrollInWrongDirectionDoesNothing() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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
        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(itemSize.toPx())
        }

        runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    @Ignore // TODO: the test is failing
    fun column_scrollForwardHalfWay() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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

        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(-itemSize.toPx() * 0.5f)
        }

        val scrolled = runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(-itemSize + scrolled)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(scrolled)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize + scrolled)
    }

    @Test
    fun column_scrollForwardTillTheEnd() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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
        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(-itemSize.toPx() * 2.2f)
        }

        runOnIdle {
            with(density) {
                val realOffset = state.firstVisibleItemScrollOffset.toDp() +
                    itemSize * state.firstVisibleItemIndex
                assertThat(realOffset).isEqualTo(itemSize * 2)
            }
        }

        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_emitTwoElementsAsOneItem_positionedReversed() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                reverseLayout = true
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_emitTwoItems_positionedReversed() = runSkikoComposeUiTest {
        setContent {
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

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_initialScrollPositionIs0() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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

        runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }
    }

    @Test
    fun row_scrollInWrongDirectionDoesNothing() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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
        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(itemSize.toPx(), ScrollWheel.Horizontal)
        }


        runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    @Ignore // TODO: the test is failing
    fun row_scrollForwardHalfWay() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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

        onNodeWithTag(ContainerTag)
            .performMouseInput {
                scroll(-itemSize.toPx() * 0.5f, ScrollWheel.Horizontal)
            }

        val scrolled = runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(-itemSize + scrolled)
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(scrolled)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize + scrolled)
    }

    @Test
    fun row_scrollForwardTillTheEnd() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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
        onNodeWithTag(ContainerTag)
            .performMouseInput {
                scroll(-itemSize.toPx() * 2.2f, ScrollWheel.Horizontal)
            }

        runOnIdle {
            with(density) {
                val realOffset = state.firstVisibleItemScrollOffset.toDp() +
                    itemSize * state.firstVisibleItemIndex
                assertThat(realOffset).isEqualTo(itemSize * 2)
            }
        }

        onNodeWithTag("3")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_rtl_emitTwoElementsAsOneItem_positionedReversed() = runSkikoComposeUiTest {
        setContent {
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

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun row_rtl_emitTwoItems_positionedReversed() = runSkikoComposeUiTest {
        setContent {
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

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    @Ignore // TODO: the test is failing
    fun row_rtl_scrollForwardHalfWay() = runSkikoComposeUiTest {
        lateinit var state: LazyListState
        setContent {
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

        onNodeWithTag(ContainerTag)
            .performMouseInput {
                scroll(itemSize.toPx() * 0.5f, ScrollWheel.Horizontal)
            }

        val scrolled = runOnIdle {
            assertThat(state.firstVisibleItemScrollOffset).isGreaterThan(0)
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            with(density) { state.firstVisibleItemScrollOffset.toDp() }
        }

        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(-scrolled)
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize - scrolled)
        onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2 - scrolled)
    }

    @Test
    fun column_whenParameterChanges() = runSkikoComposeUiTest {
        var reverse by mutableStateOf(true)
        setContent {
            LazyColumn(
                reverseLayout = reverse
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemSize)

        runOnIdle {
            reverse = false
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun row_whenParameterChanges() = runSkikoComposeUiTest {
        var reverse by mutableStateOf(true)
        setContent {
            LazyRow(
                reverseLayout = reverse
            ) {
                item {
                    Box(Modifier.requiredSize(itemSize).testTag("0"))
                    Box(Modifier.requiredSize(itemSize).testTag("1"))
                }
            }
        }

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(itemSize)

        runOnIdle {
            reverse = false
        }

        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }
}
