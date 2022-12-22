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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.grid

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class LazyGridSpanTest {

    @Test
    fun spans() = runSkikoComposeUiTest {
        val columns = 4
        val columnWidth = with(density) { 5.toDp() }
        val itemHeight = with(density) { 10.toDp() }
        setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3)
            ) {
                items(
                    count = 6,
                    span = { index ->
                        when (index) {
                            0 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(3)
                            }
                            1 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(1)
                                GridItemSpan(1)
                            }
                            2 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            3 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(3)
                            }
                            4 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(4)
                                GridItemSpan(1)
                            }
                            5 -> {
                                assertThat(maxLineSpan).isEqualTo(4)
                                assertThat(maxCurrentLineSpan).isEqualTo(3)
                                GridItemSpan(1)
                            }
                            else -> error("Out of index span queried")
                        }
                    },
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
        onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("5")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
    }

    @Test
    fun spansWithHorizontalSpacing() = runSkikoComposeUiTest {
        val columns = 4
        val columnWidth = with(density) { 5.toDp() }
        val itemHeight = with(density) { 10.toDp() }
        val spacing = with(density) { 4.toDp() }
        setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(
                    columnWidth * columns + spacing * (columns - 1),
                    itemHeight
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(
                    count = 2,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            1 -> GridItemSpan(3)
                            else -> error("Out of index span queried")
                        }
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("$it"))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth + spacing)
            .assertWidthIsEqualTo(columnWidth * 3 + spacing * 2)
    }

    @Test
    fun spansMultipleBlocks() = runSkikoComposeUiTest {
        val columns = 4
        val columnWidth = with(density) { 5.toDp() }
        val itemHeight = with(density) { 10.toDp() }
        setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight)
            ) {
                items(
                    count = 1,
                    span = { index ->
                        when (index) {
                            0 -> GridItemSpan(1)
                            else -> error("Out of index span queried")
                        }
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                item(span = {
                    if (maxCurrentLineSpan != 3) error("Wrong maxSpan")
                    GridItemSpan(2)
                }) {
                    Box(Modifier.height(itemHeight).testTag("1"))
                }
                items(
                    count = 1,
                    span = { index ->
                        if (maxCurrentLineSpan != 1 || index != 0) {
                            error("Wrong span calculation parameters")
                        }
                        GridItemSpan(1)
                    }
                ) {
                    if (it != 0) error("Wrong index")
                    Box(Modifier.height(itemHeight).testTag("2"))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth)
            .assertWidthIsEqualTo(columnWidth * 2)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 3)
            .assertWidthIsEqualTo(columnWidth)
    }

    @Test
    fun spansLineBreak() = runSkikoComposeUiTest {
        val columns = 4
        val columnWidth = with(density) { 5.toDp() }
        val itemHeight = with(density) { 10.toDp() }
        setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.requiredSize(columnWidth * columns, itemHeight * 3)
            ) {
                item(span = {
                    if (maxCurrentLineSpan != 4) error("Wrong maxSpan")
                    GridItemSpan(3)
                }) {
                    Box(Modifier.height(itemHeight).testTag("0"))
                }
                items(
                    count = 4,
                    span = { index ->
                        if (maxCurrentLineSpan != when (index) {
                                0 -> 1
                                1 -> 2
                                2 -> 1
                                3 -> 2
                                else -> error("Wrong index")
                            }
                        ) error("Wrong maxSpan")
                        GridItemSpan(listOf(2, 1, 2, 2)[index])
                    }
                ) {
                    Box(Modifier.height(itemHeight).testTag((it + 1).toString()))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 3)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemHeight)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth)
        onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(columnWidth * 2)
        onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemHeight * 2)
            .assertLeftPositionInRootIsEqualTo(columnWidth * 2)
            .assertWidthIsEqualTo(columnWidth * 2)
    }

    @Test
    fun spansCalculationDoesntCrash() = runSkikoComposeUiTest {
        // regression from b/222530458
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            state = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state,
                modifier = Modifier.size(100.dp)
            ) {
                repeat(100) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(1.dp))
                    }
                    items(10) {
                        Box(Modifier.fillMaxWidth().height(1.dp))
                    }
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollToItem(state.layoutInfo.totalItemsCount)
            }
        }
    }
}